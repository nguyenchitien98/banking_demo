# Bài 06 — Redis: Patterns & Use Cases Thực Tế

> **Mục tiêu:** Áp dụng Redis đúng pattern cho từng bài toán — BankX là lab thực hành  
> **Bao gồm:** Caching, Distributed Lock, Rate Limiting, Session, OTP, Queue

---

## 1. Caching Patterns

### 1.1. Cache-Aside (Lazy Loading) — Pattern BankX Dùng

```
Request đến → Check Cache → Cache Hit? → Trả về
                          → Cache Miss? → Query DB → Write to Cache → Trả về

Ưu điểm:
  - Chỉ cache data được request (không cache data không dùng)
  - DB failure không ảnh hưởng cache (cache vẫn phục vụ đến khi hết TTL)
Nhược điểm:
  - Cache Miss đầu tiên chậm (phải query DB)
  - Cache Stampede: Nhiều requests cùng miss cùng key → Tất cả query DB cùng lúc
```

```java
// BankX - AccountService với Cache-Aside:
@Service
public class AccountService {
  private final RedisTemplate<String, String> redis;
  private final AccountRepository accountRepo;
  private final ObjectMapper objectMapper;

  public BigDecimal getAccountBalance(String accountNumber) {
    String cacheKey = "account_balance:" + accountNumber;

    // 1. Check cache
    String cached = redis.opsForValue().get(cacheKey);
    if (cached != null) {
      return new BigDecimal(cached); // Cache HIT
    }

    // 2. Cache MISS → Query DB
    BankAccount account = accountRepo.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

    // 3. Write to cache
    redis.opsForValue().set(
        cacheKey,
        account.getBalance().toPlainString(),
        Duration.ofSeconds(30) // TTL 30 giây
    );

    return account.getBalance();
  }

  // QUAN TRỌNG: Evict cache sau khi update balance
  public void updateBalance(String accountNumber, BigDecimal newBalance) {
    accountRepo.updateBalance(accountNumber, newBalance);

    // Xóa cache để lần sau đọc fresh data từ DB
    redis.delete("account_balance:" + accountNumber);
    // Không SET giá trị mới vào cache vì:
    // 1. Có thể có concurrent transactions khác
    // 2. Lazy loading sẽ load lại khi cần
  }
}
```

### 1.2. Write-Through — Ghi Đồng Thời DB và Cache

```java
// Viết vào DB và Cache cùng lúc
public void updateBalance(String accountNumber, BigDecimal newBalance) {
  // Atomic transaction
  accountRepo.updateBalance(accountNumber, newBalance); // Write DB

  // Write cache ngay
  redis.opsForValue().set(
      "account_balance:" + accountNumber,
      newBalance.toPlainString(),
      Duration.ofSeconds(30)
  );
  // Ưu điểm: Cache luôn fresh sau write
  // Nhược điểm: Write latency tăng (phải chờ cả DB + Redis)
}
```

### 1.3. Cache Stampede Prevention

```java
// Vấn đề: 1000 requests cùng cache miss → 1000 DB queries cùng lúc

// Giải pháp: Mutex lock
public BigDecimal getBalanceWithLock(String accountNumber) {
  String cacheKey   = "account_balance:" + accountNumber;
  String lockKey    = "cache_lock:" + accountNumber;

  // Check cache trước
  String cached = redis.opsForValue().get(cacheKey);
  if (cached != null) return new BigDecimal(cached);

  // Thử lấy lock để query DB
  Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

  if (Boolean.TRUE.equals(acquired)) {
    try {
      // Double-check cache (có thể đã được set bởi lock holder trước đó)
      cached = redis.opsForValue().get(cacheKey);
      if (cached != null) return new BigDecimal(cached);

      // Query DB + set cache
      BigDecimal balance = accountRepo.findBalance(accountNumber);
      redis.opsForValue().set(cacheKey, balance.toPlainString(), Duration.ofSeconds(30));
      return balance;
    } finally {
      redis.delete(lockKey);
    }
  } else {
    // Không có lock → Chờ 100ms rồi thử lại (retry)
    Thread.sleep(100);
    return getBalanceWithLock(accountNumber); // Recursive retry
  }
}
```

---

## 2. Distributed Lock — Redisson / Redis SETNX

### 2.1. Simple Lock với SET NX EX

```java
// Distributed Lock: Đảm bảo chỉ 1 instance thực hiện critical operation

@Service
public class DistributedLockService {
  private final StringRedisTemplate redis;

  public boolean acquireLock(String lockKey, String lockValue, Duration timeout) {
    // SET key value NX EX seconds — Atomic!
    // NX: Chỉ set nếu key CHƯA tồn tại
    // EX: TTL
    Boolean result = redis.opsForValue().setIfAbsent(lockKey, lockValue, timeout);
    return Boolean.TRUE.equals(result);
  }

  public void releaseLock(String lockKey, String lockValue) {
    // QUAN TRỌNG: Chỉ xóa lock nếu lockValue của MÌNH
    // Tránh xóa nhầm lock của người khác (khi TTL hết mà lock mới được tạo)
    String lua = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
          return redis.call('del', KEYS[1])
        else
          return 0
        end
        """;
    redis.execute(
        new DefaultRedisScript<>(lua, Long.class),
        List.of(lockKey),
        lockValue
    );
  }
}

// Usage:
String lockKey   = "transfer_lock:" + transferId;
String lockValue = UUID.randomUUID().toString(); // Unique value cho instance này

if (lockService.acquireLock(lockKey, lockValue, Duration.ofSeconds(30))) {
  try {
    executeTransfer(transfer); // Critical section
  } finally {
    lockService.releaseLock(lockKey, lockValue); // Luôn release trong finally
  }
} else {
  throw new DuplicateTransferException("Transfer đang được xử lý");
}
```

### 2.2. Idempotency Lock — BankX Pattern

```java
// BankX: IdempotencyAspect dùng Redis SETNX

@Around("@annotation(Idempotent)")
public Object checkIdempotency(ProceedingJoinPoint pjp) throws Throwable {
  String idempotencyKey = getKeyFromHeader(); // X-Idempotency-Key header
  String redisKey = "idempotency:lock:" + idempotencyKey;
  String processingKey = "idempotency:processing:" + idempotencyKey;
  String resultKey = "idempotency:result:" + idempotencyKey;

  // 1. Kiểm tra đã có cached result chưa:
  String cachedResult = redis.opsForValue().get(resultKey);
  if (cachedResult != null) {
    return objectMapper.readValue(cachedResult, Object.class); // Trả về cached response
  }

  // 2. Thử lock (đánh dấu đang processing):
  Boolean locked = redis.opsForValue().setIfAbsent(
      processingKey, "processing",
      Duration.ofSeconds(30)
  );

  if (!Boolean.TRUE.equals(locked)) {
    // Đang xử lý bởi request khác → Chờ hoặc reject
    throw new DuplicateRequestException("Request đang được xử lý, vui lòng chờ");
  }

  try {
    // 3. Thực thi method gốc:
    Object result = pjp.proceed();

    // 4. Cache kết quả để request duplicate sau nhận được cùng response:
    redis.opsForValue().set(
        resultKey,
        objectMapper.writeValueAsString(result),
        Duration.ofMinutes(10) // Cache 10 phút
    );

    return result;
  } finally {
    redis.delete(processingKey); // Release processing lock
  }
}
```

---

## 3. OTP Management

```java
// BankX OtpService — đầy đủ pattern

@Service
public class OtpService {
  private final StringRedisTemplate redis;
  private static final int OTP_TTL_SECONDS = 120;
  private static final int MAX_ATTEMPTS = 3;
  private static final int BLOCK_TTL_SECONDS = 300;

  private String otpKey(String purpose, String phone) {
    return "otp:" + purpose + ":" + phone;
  }
  private String attemptsKey(String purpose, String phone) {
    return "otp_attempts:" + purpose + ":" + phone;
  }
  private String blockedKey(String purpose, String phone) {
    return "otp_blocked:" + purpose + ":" + phone;
  }

  public void generateAndSend(String purpose, String phone) {
    // Kiểm tra có đang bị block không:
    if (Boolean.TRUE.equals(redis.hasKey(blockedKey(purpose, phone)))) {
      throw new TooManyAttemptsException("Bạn đã thử quá nhiều lần. Vui lòng chờ 5 phút.");
    }

    // Generate OTP:
    String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

    // Lưu OTP vào Redis với TTL:
    redis.opsForValue().set(otpKey(purpose, phone), otp, Duration.ofSeconds(OTP_TTL_SECONDS));

    // Reset attempts counter:
    redis.delete(attemptsKey(purpose, phone));

    // Gửi OTP qua SMS (async):
    smsService.send(phone, "Mã OTP BankX: " + otp + " (hết hạn sau 2 phút)");
  }

  public boolean verify(String purpose, String phone, String inputOtp) {
    // Kiểm tra block:
    if (Boolean.TRUE.equals(redis.hasKey(blockedKey(purpose, phone)))) {
      throw new TooManyAttemptsException("Tài khoản tạm khóa do nhập sai OTP quá nhiều lần.");
    }

    String stored = redis.opsForValue().get(otpKey(purpose, phone));

    if (stored == null) {
      throw new OtpExpiredException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
    }

    if (!stored.equals(inputOtp)) {
      // Tăng counter sai:
      Long attempts = redis.opsForValue().increment(attemptsKey(purpose, phone));
      redis.expire(attemptsKey(purpose, phone), Duration.ofSeconds(OTP_TTL_SECONDS));

      if (attempts != null && attempts >= MAX_ATTEMPTS) {
        // Block 5 phút:
        redis.opsForValue().set(blockedKey(purpose, phone), "blocked",
            Duration.ofSeconds(BLOCK_TTL_SECONDS));
        redis.delete(otpKey(purpose, phone));
        redis.delete(attemptsKey(purpose, phone));
        throw new TooManyAttemptsException("Nhập sai OTP 3 lần. Tạm khóa 5 phút.");
      }

      int remaining = MAX_ATTEMPTS - attempts.intValue();
      throw new InvalidOtpException("Mã OTP không đúng. Còn " + remaining + " lần thử.");
    }

    // OTP đúng → Xóa và trả về true:
    redis.delete(otpKey(purpose, phone));
    redis.delete(attemptsKey(purpose, phone));
    return true;
  }
}
```

---

## 4. Rate Limiting Patterns

### 4.1. Fixed Window Counter

```java
// Đơn giản nhất — Đếm requests trong mỗi window cố định

public boolean isAllowed(String userId, int maxRequests) {
  String windowKey = "rate_limit:" + userId + ":" + (System.currentTimeMillis() / 60000);
  // Window mỗi phút (60000ms)

  Long count = redis.opsForValue().increment(windowKey);

  if (count == 1) {
    redis.expire(windowKey, Duration.ofMinutes(2)); // TTL 2 phút (buffer)
  }

  return count <= maxRequests;
}
// Nhược điểm: Boundary problem — 100 req cuối phút + 100 req đầu phút kế = 200 req trong 2 giây
```

### 4.2. Sliding Window — BankX Pattern (Chính Xác Hơn)

```java
// Dùng Sorted Set: score = timestamp, member = requestId

public boolean isAllowedSlidingWindow(String userId, int maxRequests, Duration window) {
  String key = "rate_limit:sliding:" + userId;
  long now = System.currentTimeMillis();
  long windowStart = now - window.toMillis();

  // Pipeline (atomic batch):
  List<Object> results = redis.executePipelined((RedisCallback<?>) connection -> {
    byte[] keyBytes = key.getBytes();

    // Xóa requests cũ hơn window:
    connection.sortedSetCommands().zRemRangeByScore(
        keyBytes, Range.of(Range.Bound.inclusive(0.0), Range.Bound.exclusive((double) windowStart))
    );

    // Thêm request hiện tại:
    String requestId = UUID.randomUUID().toString();
    connection.sortedSetCommands().zAdd(keyBytes, (double) now, requestId.getBytes());

    // Đếm requests trong window:
    connection.sortedSetCommands().zCard(keyBytes);

    // Set TTL:
    connection.keyCommands().expire(keyBytes, window.getSeconds() + 1);

    return null;
  });

  Long currentCount = (Long) results.get(2); // zCard result
  return currentCount <= maxRequests;
}
```

### 4.3. Token Bucket với Redis (Leak Algorithm)

```java
// Token Bucket: Cho phép burst nhưng rate trung bình bị giới hạn
// Dùng Lua script để atomic operation

private static final String TOKEN_BUCKET_SCRIPT = """
    local tokens_key = KEYS[1]
    local last_refill_key = KEYS[2]
    local rate = tonumber(ARGV[1])      -- tokens per second
    local capacity = tonumber(ARGV[2])  -- max tokens
    local now = tonumber(ARGV[3])       -- current time in ms
    local requested = tonumber(ARGV[4]) -- tokens needed (usually 1)
    
    local last_tokens = tonumber(redis.call('get', tokens_key) or capacity)
    local last_refill = tonumber(redis.call('get', last_refill_key) or now)
    
    -- Tính tokens được refill từ lần cuối:
    local elapsed = (now - last_refill) / 1000  -- seconds
    local new_tokens = math.min(capacity, last_tokens + elapsed * rate)
    
    if new_tokens >= requested then
      -- Đủ tokens → Cho phép và trừ tokens
      redis.call('setex', tokens_key, 3600, new_tokens - requested)
      redis.call('setex', last_refill_key, 3600, now)
      return 1  -- Allowed
    else
      -- Không đủ tokens → Từ chối
      redis.call('setex', tokens_key, 3600, new_tokens)
      redis.call('setex', last_refill_key, 3600, now)
      return 0  -- Denied
    end
    """;

public boolean consumeToken(String userId) {
  List<String> keys = List.of(
      "token_bucket:tokens:" + userId,
      "token_bucket:refill:" + userId
  );

  Long result = redis.execute(
      new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class),
      keys,
      "5",    // 5 tokens per second
      "20",   // capacity 20 tokens (burst)
      String.valueOf(System.currentTimeMillis()),
      "1"     // Tiêu 1 token
  );

  return Long.valueOf(1).equals(result);
}
```

---

## 5. Session Management

```java
// BankX: JWT-based auth với Redis session store

@Service
public class SessionService {
  private final HashOperations<String, String, String> hashOps;

  private String sessionKey(String jwtId) { return "session:" + jwtId; }

  public void createSession(String jwtId, UserPrincipal user) {
    String key = sessionKey(jwtId);

    Map<String, String> sessionData = Map.of(
        "userId",    user.getId(),
        "username",  user.getUsername(),
        "roles",     String.join(",", user.getRoles()),
        "loginAt",   Instant.now().toString(),
        "ipAddress", user.getIpAddress(),
        "deviceId",  user.getDeviceId()
    );

    hashOps.putAll(key, sessionData);
    redis.expire(key, Duration.ofMinutes(30)); // Session timeout 30 phút
  }

  public Optional<SessionData> getSession(String jwtId) {
    String key = sessionKey(jwtId);
    Map<String, String> data = hashOps.entries(key);

    if (data.isEmpty()) return Optional.empty();

    // Sliding expiration: Reset TTL mỗi lần truy cập
    redis.expire(key, Duration.ofMinutes(30));

    return Optional.of(SessionData.fromMap(data));
  }

  public void invalidateSession(String jwtId) {
    redis.delete(sessionKey(jwtId));
  }

  public void invalidateAllSessions(String userId) {
    // Tìm và xóa tất cả sessions của user (scan pattern)
    ScanOptions options = ScanOptions.scanOptions()
        .match("session:*")
        .count(100)
        .build();

    try (Cursor<byte[]> cursor = redis.getConnectionFactory()
        .getConnection().scan(options)) {
      cursor.forEachRemaining(keyBytes -> {
        String key = new String(keyBytes);
        String uid = (String) hashOps.get(key, "userId");
        if (userId.equals(uid)) {
          redis.delete(key);
        }
      });
    }
  }
}
```

---

## 6. Pub/Sub cho Engineering Portal

```java
// Engineering Portal: Real-time system metrics và alerts

// Publisher (trong services):
@Service
public class MetricsPublisher {
  private final StringRedisTemplate redis;

  public void publishFraudAlert(FraudAlert alert) throws JsonProcessingException {
    String message = objectMapper.writeValueAsString(alert);
    redis.convertAndSend("channel:fraud_alerts", message);
    // Tất cả subscribers nhận ngay lập tức
  }

  public void publishSystemMetric(SystemMetric metric) throws JsonProcessingException {
    redis.convertAndSend("channel:system_metrics", objectMapper.writeValueAsString(metric));
  }
}

// Subscriber (trong Engineering Portal service):
@Configuration
public class RedisMessageConfig {

  @Bean
  public RedisMessageListenerContainer messageListenerContainer(
      RedisConnectionFactory factory) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);

    // Subscribe fraud alerts:
    container.addMessageListener(
        (message, pattern) -> {
          String json = new String(message.getBody());
          FraudAlert alert = objectMapper.readValue(json, FraudAlert.class);
          // Gửi qua WebSocket đến Engineering Portal UI:
          websocketService.broadcastAlert(alert);
        },
        new PatternTopic("channel:fraud_alerts")
    );

    return container;
  }
}
```

---

## 7. Lua Scripts — Atomic Multi-Command Operations

```java
// Lua script chạy atomic trên Redis server (không bị interrupt)
// Dùng khi cần nhiều commands mà phải atomic

// Ví dụ: Tăng counter VÀ kiểm tra trong 1 atomic operation
String transferCountScript = """
    local current = redis.call('INCR', KEYS[1])
    if current == 1 then
      redis.call('EXPIRE', KEYS[1], ARGV[1])
    end
    return current
    """;

Long count = redis.execute(
    new DefaultRedisScript<>(transferCountScript, Long.class),
    List.of("transfer_count:" + accountId + ":" + today),
    "86400" // TTL 1 ngày
);
// Atomic: INCREMENT + SET EXPIRE trong 1 step → Không có race condition
```

---

## 8. Redis Patterns Summary — BankX Full Map

```
Key Naming Convention: {category}:{entity}:{identifier}[:{dimension}]

BankX Redis Keys:

Caching:
  account_balance:{accountNumber}              → String, TTL 30s
  customer_profile:{customerId}                → Hash, TTL 5m
  fraud_rules:active                           → String (JSON), TTL 5m

Authentication:
  session:{jwtId}                              → Hash, TTL 30m (sliding)
  refresh_token:{tokenHash}                    → String, TTL 7d

OTP:
  otp:{purpose}:{phone}                        → String, TTL 120s
  otp_attempts:{purpose}:{phone}               → String (counter), TTL 120s
  otp_blocked:{purpose}:{phone}                → String, TTL 300s

Idempotency:
  idempotency:lock:{idempotencyKey}            → String, TTL 30s
  idempotency:result:{idempotencyKey}          → String (JSON), TTL 10m

Rate Limiting:
  rate_limit:{userId}:{windowMinute}           → String (counter), TTL 2m
  rate_limit:sliding:{userId}                  → ZSet, TTL 1m
  token_bucket:tokens:{userId}                 → String (float), TTL 1h
  token_bucket:refill:{userId}                 → String (timestamp), TTL 1h

Distributed Lock:
  lock:transfer:{transferId}                   → String, TTL 30s
  lock:outbox_polling:{instanceId}             → String, TTL 10s

Counters:
  daily_transfer_count:{accountId}:{date}      → String (counter), TTL 2d
  failed_login_attempts:{username}             → String (counter), TTL 15m
```

---

**← [Bài 05 — Redis Data Structures](./05_redis_dac_tinh_va_data_structures.md)** | **→ [Bài 07 — Phỏng Vấn Q&A](./07_phong_van_database_qa.md)**
