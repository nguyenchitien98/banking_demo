# Bài 05 — Redis: Đặc Tính & Data Structures

> **Mục tiêu:** Hiểu Redis là gì, các kiểu dữ liệu và khi nào dùng loại nào  
> **Liên quan BankX:** Cache balance, OTP, Idempotency key, Rate limiting, Session

---

## 1. Redis Là Gì? Đặc Tính Nổi Bật

```
Redis = Remote Dictionary Server
     = In-memory data structure store

Được dùng như:
  ✅ Cache (phổ biến nhất)
  ✅ Session store
  ✅ Message queue (simple)
  ✅ Pub/Sub messaging
  ✅ Rate limiter
  ✅ Distributed lock
  ✅ Leaderboard / Sorted ranking
  ✅ Real-time analytics

Không phải:
  ❌ Primary database thay thế PostgreSQL
  ❌ Permanent storage (dù có persistence options)
```

### So Sánh Redis vs PostgreSQL

| | Redis | PostgreSQL |
|---|---|---|
| **Storage** | In-memory (RAM) | Disk |
| **Speed** | ~100K-1M ops/sec | ~10K-100K qps |
| **Durability** | Optional (RDB/AOF) | ✅ WAL, ACID |
| **Data Model** | Key-Value + rich structures | Relational tables |
| **Query** | Commands (GET, SET, ZADD...) | SQL |
| **Transactions** | MULTI/EXEC (simple) | Full ACID |
| **Use case** | Cache, ephemeral data | Persistent, relational data |
| **Cost** | Memory (RAM đắt) | Disk (rẻ hơn) |

### Redis vs Memcached

| | Redis | Memcached |
|---|---|---|
| **Data Structures** | Phong phú (String, Hash, List, Set, ZSet, Stream...) | Chỉ String |
| **Persistence** | ✅ RDB + AOF | ❌ |
| **Pub/Sub** | ✅ | ❌ |
| **Clustering** | ✅ Redis Cluster | ✅ |
| **Lua Scripting** | ✅ | ❌ |
| **Transactions** | ✅ MULTI/EXEC | ❌ |
| **Kết luận** | Phổ biến hơn, feature-rich | Chỉ simple caching |

---

## 2. Data Types — Trái Tim Của Redis

### 2.1. String — Đơn Giản Nhất, Dùng Nhiều Nhất

```bash
# String không chỉ là text — Bất kỳ binary data nào, tối đa 512MB

# === Basic ===
SET key value
SET account_balance:088880001 5000000
GET account_balance:088880001           # → "5000000"

# === TTL (Time To Live) ===
SET otp:LOGIN:0901234567 123456 EX 120  # Hết hạn sau 120 giây
SET otp:LOGIN:0901234567 123456 PX 120000  # Milliseconds
TTL otp:LOGIN:0901234567                # Còn bao nhiêu giây?
PTTL otp:LOGIN:0901234567               # Còn bao nhiêu milliseconds?
PERSIST otp:LOGIN:0901234567            # Xóa TTL, key sống mãi

# === SET Variants ===
SETNX key value      # Set if Not Exists — atomic! (dùng cho distributed lock)
SET key value NX     # Set if Not Exists — tương đương SETNX nhưng hỗ trợ EX
SET key value XX     # Set if Exists (chỉ update, không tạo mới)
SET key value NX EX 300  # ← BankX dùng cho idempotency key

# === Atomic Numeric Operations ===
SET daily_transfer_count:acc-001:2026-09-16 0
INCR  daily_transfer_count:acc-001:2026-09-16  # +1 (atomic!)
INCRBY daily_transfer_count:acc-001:2026-09-16 5  # +5
DECR  daily_transfer_count:acc-001:2026-09-16  # -1
DECRBY daily_transfer_count:acc-001:2026-09-16 5  # -5
INCRBYFLOAT price:GOLD 0.5  # Float increment

# BankX: OTP attempts counter
INCR otp_attempts:LOGIN:0901234567  # Tăng số lần thử OTP

# === Batch Operations ===
MSET key1 val1 key2 val2 key3 val3   # Multi-SET
MGET key1 key2 key3                  # Multi-GET → Giảm round trips

# === String as Binary ===
SET profile_image:user-001 <binary_data>  # Lưu ảnh (giới hạn 512MB)
STRLEN account_balance:088880001          # Độ dài string value
GETRANGE key 0 9                          # Lấy substring (offset 0-9)
```

### 2.2. Hash — Object/Record

```bash
# Hash = Map<String, String> trong 1 key
# Tốt cho: User session, Account info, Config

# === Basic ===
HSET user:cust-001 name "Nguyen Van A" email "a@example.com" tier "PREMIUM"
HGET user:cust-001 name          # → "Nguyen Van A"
HMGET user:cust-001 name email   # Multi-get fields
HGETALL user:cust-001            # Tất cả fields và values
HKEYS user:cust-001              # Chỉ lấy keys (field names)
HVALS user:cust-001              # Chỉ lấy values
HLEN user:cust-001               # Số lượng fields

# === Update/Delete fields ===
HSET user:cust-001 tier "GOLD"   # Update 1 field
HDEL user:cust-001 email         # Xóa 1 field
HEXISTS user:cust-001 email      # Kiểm tra field tồn tại

# === Numeric trong Hash ===
HSET account:acc-001 balance 5000000 version 7
HINCRBY account:acc-001 balance -500000  # Giảm balance (atomic!)
HINCRBYFLOAT account:acc-001 balance -500000.50

# === BankX: Session storage ===
HSET session:token-abc123
  user_id "cust-001"
  username "user01"
  roles "[\"CUSTOMER\"]"
  login_at "2026-09-16T06:00:00Z"
  ip "1.2.3.4"
EXPIRE session:token-abc123 1800  # Session hết hạn sau 30 phút

# === Tại sao Hash tốt hơn JSON string cho session? ===
# Hash: Cập nhật 1 field → HSET key field val (chỉ ghi field đó)
# JSON: Phải GET toàn bộ → parse → modify → stringify → SET lại (toàn bộ)
# Hash tiết kiệm băng thông và CPU khi cập nhật từng phần
```

### 2.3. List — Queue / Stack

```bash
# List = Doubly Linked List
# Tốt cho: Message queue, Recent activity, Log buffer

# === Push/Pop ===
LPUSH transfer_queue job1 job2 job3  # Push vào HEAD (trái)
RPUSH transfer_queue job4 job5       # Push vào TAIL (phải)
LPOP transfer_queue                  # Pop từ HEAD
RPOP transfer_queue                  # Pop từ TAIL

# Stack (LIFO):   LPUSH + LPOP
# Queue (FIFO):   RPUSH + LPOP (push vào cuối, lấy từ đầu)

# === Blocking Operations ===
BLPOP transfer_queue 30   # Pop từ HEAD, chờ tối đa 30 giây nếu list rỗng
BRPOP transfer_queue 30   # Pop từ TAIL, chờ tối đa 30 giây
# Worker pattern: Worker ngồi BLPOP → Có job mới → Nhận ngay lập tức (không polling!)

# === Inspect ===
LRANGE transfer_queue 0 -1  # Tất cả elements (0 = đầu, -1 = cuối)
LRANGE transfer_queue 0 9   # 10 elements đầu
LLEN transfer_queue         # Độ dài list
LINDEX transfer_queue 0     # Element tại index 0

# === BankX: Recent transactions (cap size) ===
LPUSH recent_tx:acc-001 tx-uuid-new   # Thêm vào đầu
LTRIM recent_tx:acc-001 0 99          # Giữ tối đa 100 elements gần nhất (trim phần còn lại)
LRANGE recent_tx:acc-001 0 9          # Lấy 10 transactions gần nhất
```

### 2.4. Set — Tập Hợp Không Có Thứ Tự, Unique

```bash
# Set = HashSet (no duplicates, no order)
# Tốt cho: Tags, permissions, unique visitors, "đã xem"

# === Basic ===
SADD fraud_blocked_accounts acc-001 acc-002 acc-003
SREM fraud_blocked_accounts acc-001         # Xóa member
SMEMBERS fraud_blocked_accounts             # Tất cả members
SCARD fraud_blocked_accounts               # Số lượng members
SISMEMBER fraud_blocked_accounts acc-002   # Kiểm tra có trong set không (0/1)

# === Set Operations ===
SADD premium_customers cust-A cust-B cust-C
SADD vip_customers cust-B cust-C cust-D

SINTER premium_customers vip_customers      # Intersection: {cust-B, cust-C}
SUNION premium_customers vip_customers      # Union: {cust-A, cust-B, cust-C, cust-D}
SDIFF  premium_customers vip_customers      # Difference: {cust-A} (trong premium nhưng không trong vip)

# === BankX: Idempotency checking ===
SADD processed_events event-uuid-1 event-uuid-2
SISMEMBER processed_events event-uuid-1   # → 1 (đã xử lý)
SISMEMBER processed_events event-uuid-new # → 0 (chưa xử lý)
```

### 2.5. Sorted Set (ZSet) — Set Có Score, Tự Động Sort

```bash
# ZSet = (score, member) pairs — sorted by score automatically
# Tốt cho: Leaderboard, Rate limiting (sliding window), Priority queue, Timeline

# === Basic ===
ZADD leaderboard 100.5 "cust-001"
ZADD leaderboard 200.0 "cust-002"
ZADD leaderboard 150.0 "cust-003"
# Tự động sort: cust-001(100.5) < cust-003(150.0) < cust-002(200.0)

ZSCORE leaderboard "cust-001"      # → 100.5
ZRANK  leaderboard "cust-001"      # → 0 (rank tính từ 0, ascending)
ZREVRANK leaderboard "cust-002"    # → 0 (rank từ cao nhất)
ZCARD leaderboard                  # → 3

# === Range queries ===
ZRANGE    leaderboard 0 -1 WITHSCORES   # Tất cả, tăng dần
ZREVRANGE leaderboard 0 9 WITHSCORES    # Top 10 cao nhất

ZRANGEBYSCORE leaderboard 100 200       # Members có score 100-200
ZRANGEBYSCORE leaderboard -inf +inf     # Tất cả

ZRANGEBYLEX leaderboard "[a" "[z"       # Range theo lexicographic (khi score bằng nhau)

# === Modify score ===
ZINCRBY leaderboard 50 "cust-001"  # score 100.5 → 150.5

# === BankX: Rate limiting với Sliding Window ===
# Key: rate_limit:transfer:{userId}
# Score = timestamp của request
# Member = request_id (unique)

# Khi có request mới:
ZADD rate_limit:transfer:cust-001 1726441773000 "req-uuid-new"
# Xóa requests cũ hơn 1 phút (60000ms):
ZREMRANGEBYSCORE rate_limit:transfer:cust-001 -inf 1726441713000
# Đếm requests trong 1 phút gần nhất:
ZCARD rate_limit:transfer:cust-001
# Nếu > 10 → Reject (rate limit exceeded)
# TTL cho key:
EXPIRE rate_limit:transfer:cust-001 60

# BankX: Daily transfer amount tracking
ZADD transfers:2026-09-16:amounts <timestamp> "<transferId>:<amount>"
ZRANGEBYSCORE transfers:2026-09-16:amounts 0 +inf WITHSCORES  # Tất cả transfers hôm nay
```

### 2.6. Stream — Append-Only Log (Redis 5.0+)

```bash
# Stream = Append-only log (giống Kafka nhưng đơn giản hơn)
# Tốt cho: Event streaming, Audit log, Real-time feed

# === Produce message ===
XADD transfer_events * event_type TRANSFER_COMPLETED amount 500000 from_account acc-001
#                   ↑ Auto-generate ID: <timestamp>-<sequence>
# Hoặc custom ID:
XADD transfer_events 1726441773000-0 event_type TRANSFER_INITIATED

# === Consume messages ===
XREAD COUNT 10 STREAMS transfer_events 0   # Đọc 10 messages từ đầu (ID=0)
XREAD COUNT 10 STREAMS transfer_events $   # Chỉ messages mới (từ bây giờ)
XREAD BLOCK 0 STREAMS transfer_events $    # Block mãi chờ message mới

# === Consumer Groups (Kafka-like) ===
XGROUP CREATE transfer_events notification_service $    # Tạo consumer group
XREADGROUP GROUP notification_service worker-1 COUNT 10 STREAMS transfer_events >
# >: Chỉ đọc messages chưa được deliver cho group này
XACK transfer_events notification_service <message-id>  # Acknowledge đã xử lý
```

---

## 3. Pub/Sub — Messaging Đơn Giản

```bash
# Pub/Sub: Publisher gửi message, tất cả Subscribers nhận
# Khác Stream: Messages KHÔNG được lưu lại (fire-and-forget)

# Subscriber (listening):
SUBSCRIBE channel:fraud_alerts
# Hoặc pattern:
PSUBSCRIBE channel:*   # Subscribe tất cả channels bắt đầu bằng "channel:"

# Publisher:
PUBLISH channel:fraud_alerts '{"type":"HIGH_RISK","accountId":"acc-001","score":85}'
# Tất cả subscribers đang lắng nghe nhận ngay

# BankX: Engineering Portal nhận real-time alerts
# → Fraud service PUBLISH alert
# → Engineering Portal UI SUBSCRIBE → Hiển thị alert ngay lập tức
```

---

## 4. Persistence — Redis Không Chỉ Là In-Memory

### 4.1. RDB (Redis Database Snapshot)

```
RDB: Chụp snapshot toàn bộ data vào file .rdb theo schedule
  Ưu: File nhỏ, restore nhanh, ít tác động performance
  Nhược: Có thể mất data từ lần snapshot cuối đến khi crash

Config (redis.conf):
  save 900 1      → Snapshot nếu có ≥1 key thay đổi trong 900 giây
  save 300 10     → Snapshot nếu có ≥10 key thay đổi trong 300 giây
  save 60 10000   → Snapshot nếu có ≥10000 key thay đổi trong 60 giây
  
  dbfilename dump.rdb
  dir /var/lib/redis
```

### 4.2. AOF (Append-Only File)

```
AOF: Ghi mỗi write command vào log file
  Ưu: Ít mất data hơn RDB (log mỗi lệnh)
  Nhược: File lớn hơn, cần BGREWRITEAOF để compact, chậm hơn RDB khi restart

Config:
  appendonly yes
  appendfsync always     → Sync mỗi write (an toàn nhất, chậm nhất)
  appendfsync everysec   → Sync mỗi giây (balance, BankX dùng)
  appendfsync no         → OS quyết định (nhanh nhất, ít an toàn)
```

### 4.3. BankX Redis Persistence Strategy

```
BankX dùng: AOF + everysec
Lý do:
  - Mất tối đa 1 giây data khi crash
  - OTP, rate limit, idempotency key là ephemeral → Mất cũng OK (client retry)
  - Account balance là cached → Mất thì load lại từ PostgreSQL
  - KHÔNG lưu data quan trọng chỉ trong Redis
```

---

## 5. Redis Commands Quan Trọng Khác

```bash
# === Key Operations ===
EXISTS key1 key2 key3        # Kiểm tra key tồn tại, trả về số lượng tồn tại
DEL key1 key2                # Xóa keys (synchronous)
UNLINK key1 key2             # Xóa keys (async, không block) → Tốt hơn DEL cho key lớn
RENAME oldkey newkey         # Đổi tên key
TYPE key                     # Trả về type: string, hash, list, set, zset, stream

# === Pattern Matching (cẩn thận với KEYS trên production!) ===
KEYS pattern:*              # Tìm tất cả keys matching pattern → BLOCK! Không dùng production
SCAN 0 MATCH "otp:*" COUNT 100  # Iterate qua keys mà không block → An toàn hơn

# === Expiry ===
EXPIRE key 300              # Set TTL 300 giây
EXPIREAT key 1726441773     # Set expiry theo Unix timestamp
TTL key                     # Xem TTL còn lại (-1 = không có TTL, -2 = key không tồn tại)
EXPIRETIME key              # Trả về Unix timestamp khi key hết hạn

# === Info & Stats ===
INFO memory                 # Memory usage
INFO stats                  # Stats (hits, misses, connections...)
INFO keyspace               # Số keys per database
DBSIZE                      # Số keys trong database hiện tại

# === Multiple Databases ===
SELECT 0                    # Database 0 (default)
SELECT 1                    # Database 1 (BankX dùng DB 1 cho session, DB 0 cho cache)
FLUSHDB                     # Xóa tất cả keys trong database hiện tại (NGUY HIỂM!)
FLUSHALL                    # Xóa tất cả databases (NGUY HIỂM!)

# === Debug / Admin ===
MONITOR                     # Real-time log mọi command đến Redis (dùng debug thôi)
SLOWLOG GET 10              # 10 commands chậm nhất
DEBUG SLEEP 1               # Redis sleep 1 giây (test timeout handling)
```

---

## 6. Memory Management

```bash
# === Memory policies khi Redis đầy ===
# Config: maxmemory 2gb
# Config: maxmemory-policy

# noeviction (default): Từ chối write commands khi full → ERROR
# allkeys-lru:  Xóa LRU (Least Recently Used) keys bất kỳ
# volatile-lru: Xóa LRU keys CHỈ có TTL
# allkeys-lfu:  Xóa LFU (Least Frequently Used) keys
# volatile-ttl: Xóa keys có TTL gần hết nhất
# allkeys-random: Xóa ngẫu nhiên
# volatile-random: Xóa ngẫu nhiên keys có TTL

# BankX recommendation:
# maxmemory-policy = volatile-lru
# → Chỉ evict cached data có TTL
# → Keys không có TTL (session) được giữ lại

# === Memory optimization ===
# Hash optimization:
# Khi Hash có ≤128 fields và ≤64 bytes per field → Dùng ziplist (compact memory)
# Config: hash-max-ziplist-entries 128

# List optimization:
# Khi List ≤128 elements và ≤64 bytes per element → Dùng ziplist

# String: Số nguyên nhỏ (-9999 đến 9999) → Redis shared integers → Tiết kiệm memory
```

---

## 7. Redis vs In-Process Cache (Application Cache)

```
In-Process Cache (Caffeine, Guava Cache — trong JVM):
  ✅ Cực nhanh (nanoseconds, không qua network)
  ❌ Mỗi app instance có cache riêng → Inconsistency khi scale horizontal
  ❌ Mất khi restart app
  ❌ Không share giữa các instances

Redis (External Cache):
  ✅ Share giữa tất cả app instances → Consistency
  ✅ Persist (RDB/AOF)
  ✅ Nhiều data types
  ⚠️ Network latency (~0.1-1ms local, ~5-50ms remote)

BankX Pattern:
  L1 Cache: Caffeine (in-process) → TTL 5 giây → Fraud rules, config
  L2 Cache: Redis → TTL 30-300 giây → Account balance, user session
  Source: PostgreSQL → All persistent data

Read: L1 miss → L2 miss → PostgreSQL → Write back to L2 → Write back to L1
```

---

**← [Bài 04 — Transaction & Concurrency](./04_postgresql_transaction_va_concurrency.md)** | **→ [Bài 06 — Redis Patterns](./06_redis_patterns_va_use_cases.md)**
