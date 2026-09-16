# Java 21 — Tất Cả Tính Năng Mới (LTS, Tháng 9/2023)

> **Java 21 là LTS sau Java 17** — Mọi dự án Java nghiêm túc nên upgrade  
> **BankX dùng Java 21** — Hiểu rõ để khai thác hết sức mạnh  
> So sánh: Java 11 → Java 17 → Java 21

---

## 📋 Tổng Quan Các JEP (JDK Enhancement Proposal) Java 21

| JEP | Tên | Trạng Thái | Mức Độ Quan Trọng |
|---|---|---|---|
| 444 | Virtual Threads | **Final** ⭐⭐⭐ | Cực kỳ quan trọng |
| 440 | Record Patterns | **Final** ⭐⭐ | Quan trọng |
| 441 | Pattern Matching for `switch` | **Final** ⭐⭐⭐ | Cực kỳ quan trọng |
| 431 | Sequenced Collections | **Final** ⭐⭐ | Quan trọng |
| 439 | Generational ZGC | **Final** ⭐⭐ | Performance |
| 452 | Key Encapsulation Mechanism API | **Final** ⭐ | Security |
| 430 | String Templates | Preview | Sắp có |
| 443 | Unnamed Patterns & Variables | Preview | Developer UX |
| 463 | Unnamed Classes & Instance Main | Preview | Đơn giản hóa |

---

## 1. ⭐ Virtual Threads (JEP 444) — Game Changer Cho Backend

> **Đây là tính năng quan trọng nhất của Java 21 — Thay đổi hoàn toàn cách viết concurrent code**

### Vấn Đề Của Platform Threads (Cũ)

```
Platform Thread = OS Thread
  - Mỗi thread tốn ~1MB stack memory
  - OS quản lý → Context switch tốn kém
  - Blocking I/O (DB query, HTTP call) → Thread ngồi chờ, lãng phí
  - Với 10.000 concurrent requests → Cần 10.000 OS threads → OOM hoặc rất chậm

Ví dụ BankX (cũ - Platform Threads):
  Request 1: Gọi DB query (30ms) → Thread BLOCKED 30ms
  Request 2: Gọi Kafka publish (5ms) → Thread BLOCKED 5ms
  Request 3: Gọi External Bank API (200ms) → Thread BLOCKED 200ms
  → 3 requests = 3 OS threads blocked, lãng phí CPU
```

### Virtual Threads — Giải Pháp

```
Virtual Thread:
  - Cực nhẹ (~1KB), JVM quản lý (không phải OS)
  - Blocking I/O → JVM "unmount" Virtual Thread khỏi Carrier Thread
  - Carrier Thread phục vụ Virtual Thread khác trong lúc chờ
  - Khi I/O xong → Mount lại Carrier Thread bất kỳ → Tiếp tục

Ví dụ BankX (mới - Virtual Threads):
  Request 1: Gọi DB → Virtual Thread unmounted → Carrier Thread free
  Request 2: Dùng Carrier Thread đó xử lý trong lúc Request 1 chờ DB
  → Throughput tăng mạnh với cùng số CPU cores
```

### Cách Dùng

```java
// === Cách 1: Thread.ofVirtual() — Tạo manual ===
Thread vThread = Thread.ofVirtual()
    .name("transfer-handler-", 0) // Đặt tên prefix (tự tăng index)
    .start(() -> {
        System.out.println("Running on: " + Thread.currentThread());
        // Blocking call OK! JVM tự handle
        processTransfer(transferId);
    });

// === Cách 2: Executor với Virtual Threads ===
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Mỗi task = 1 Virtual Thread mới (rất rẻ để tạo)
    for (Transfer transfer : pendingTransfers) {
        executor.submit(() -> processTransfer(transfer));
    }
} // Auto-close: Chờ tất cả tasks hoàn thành

// === Cách 3: Spring Boot 3.2+ (BankX dùng cách này) ===
// application.yml:
// spring.threads.virtual.enabled: true
// → Spring tự động dùng Virtual Threads cho:
//   - Tomcat request handlers
//   - @Async methods
//   - @Scheduled tasks
//   - Spring WebFlux blocking adapters

// Verify đang dùng Virtual Thread:
if (Thread.currentThread().isVirtual()) {
    log.debug("Running on Virtual Thread");
}
```

### Điều Cần Lưu Ý Với Virtual Threads

```java
// ⚠️ TRÁNH: synchronized với blocking operation → "Pinning" vấn đề
// Virtual Thread bị "pinned" (gắn chặt) vào Carrier Thread khi:
// 1. Trong synchronized block
// 2. Đang chạy native code

// ❌ Problematic:
synchronized (this) {
    dbQuery(); // Blocking trong synchronized → Pin Carrier Thread → Mất lợi ích Virtual Thread
}

// ✅ Dùng ReentrantLock thay thế:
private final ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    dbQuery(); // Virtual Thread unmount được khi blocking
} finally {
    lock.unlock();
}

// ⚠️ Thread-local variables: Cẩn thận với ThreadLocal
// Virtual Threads nhiều → ThreadLocal tích lũy → Memory
// Từ Java 21: Dùng ScopedValue thay ThreadLocal (xem Java 25)
```

### So Sánh Performance

```
Platform Threads (100 concurrent requests, mỗi request query DB 50ms):
  - 100 OS threads blocked 50ms
  - CPU idle gần như hoàn toàn trong 50ms
  - Memory: 100MB stack

Virtual Threads (1000 concurrent requests, mỗi request query DB 50ms):
  - ~8 Carrier Threads (= số CPU cores)
  - CPU always busy phục vụ Virtual Threads khác trong lúc I/O wait
  - Memory: ~1MB (1000 × 1KB)
  - Throughput: 10x cao hơn
```

---

## 2. ⭐ Pattern Matching for Switch (JEP 441)

> **Finalized sau nhiều phiên bản preview** — Đây là evolution của `switch` statement

### Java 14-16: Switch Expression (Cơ Bản)

```java
// Java 14+: Switch Expression (trả về giá trị)
String statusText = switch (transfer.getStatus()) {
    case "PENDING"    -> "Đang chờ xử lý";
    case "PROCESSING" -> "Đang xử lý";
    case "COMPLETED"  -> "Hoàn thành";
    case "FAILED"     -> "Thất bại";
    default           -> "Không xác định";
};
// Không cần break! Arrow syntax → Không fall-through
```

### Java 21: Pattern Matching Switch (Mạnh Hơn Nhiều)

```java
// Trước Java 21:
Object response = getApiResponse();
if (response instanceof TransferSuccessResponse) {
    TransferSuccessResponse success = (TransferSuccessResponse) response;
    log.info("Success: {}", success.getTransferId());
} else if (response instanceof TransferFailedResponse) {
    TransferFailedResponse failed = (TransferFailedResponse) response;
    log.error("Failed: {}", failed.getReason());
} else if (response instanceof PendingOtpResponse) {
    // ...
}

// Java 21: Pattern Matching trong switch — SẠCH HƠN RẤT NHIỀU
String result = switch (getApiResponse()) {
    case TransferSuccessResponse s -> "Success: " + s.getTransferId();
    case TransferFailedResponse f  -> "Failed: " + f.getReason();
    case PendingOtpResponse o      -> "OTP required for: " + o.getTransferId();
    case null                       -> "Null response";
    default                         -> "Unknown response type";
};
```

### Guarded Patterns — Thêm Điều Kiện Vào Pattern

```java
// "when" clause (guard) — Điều kiện bổ sung cho pattern
String riskLevel = switch (transfer.getAmount().compareTo(new BigDecimal("20000000"))) {
    case TransferRequest t when t.getAmount().compareTo(new BigDecimal("100000000")) >= 0
        -> "EXTREME_RISK";
    case TransferRequest t when t.getAmount().compareTo(new BigDecimal("20000000")) >= 0
        -> "HIGH_RISK";
    case TransferRequest t when t.isNewBeneficiary()
        -> "MEDIUM_RISK";
    default
        -> "LOW_RISK";
};

// Ví dụ BankX - Fraud Rule Engine với Pattern Switch:
int riskScore = switch (fraudSignal) {
    case FraudSignal.HighAmount h when h.amount().compareTo(new BigDecimal("20000000")) >= 0
        -> 40;
    case FraudSignal.NewDevice d when d.firstLogin()
        -> 50;
    case FraudSignal.NightTime n when n.hour() >= 0 && n.hour() < 5
        -> 15;
    case FraudSignal.HighVelocity v when v.txCountLastHour() > 10
        -> 30;
    default -> 0;
};
```

---

## 3. ⭐ Record Patterns (JEP 440)

> **Kết hợp Destructuring + Pattern Matching**

### Records (Java 16 — Ôn Lại)

```java
// Record = Immutable data carrier, auto-generates:
// constructor, getters, equals(), hashCode(), toString()
record TransferRequest(
    String fromAccountId,
    String toAccountId,
    BigDecimal amount,
    String description
) {}

// Dùng:
TransferRequest req = new TransferRequest("acc-001", "acc-002", new BigDecimal("500000"), "Ăn trưa");
System.out.println(req.fromAccountId()); // Getter tự động (không phải getFromAccountId()!)
System.out.println(req);  // "TransferRequest[fromAccountId=acc-001, ...]"
```

### Record Patterns — Destructuring

```java
// Java 21: Có thể destructure Record trong pattern matching
Object obj = getTransferData();

// Trước Java 21:
if (obj instanceof TransferRequest req) {
    String from = req.fromAccountId(); // Phải gọi getter
    BigDecimal amount = req.amount();
    if (amount.compareTo(threshold) > 0) { ... }
}

// Java 21 — Record Pattern với destructuring:
if (obj instanceof TransferRequest(String from, String to, BigDecimal amount, String desc)) {
    // from, to, amount, desc đã được extract trực tiếp!
    if (amount.compareTo(threshold) > 0) { ... }
}

// Kết hợp với switch:
String summary = switch (obj) {
    case TransferRequest(var from, var to, var amount, var desc)
        when amount.compareTo(new BigDecimal("5000000")) >= 0
        -> "Large transfer from " + from + " to " + to;

    case TransferRequest(var from, var to, var amount, var desc)
        -> "Normal transfer: " + amount;

    default -> "Unknown";
};

// Nested Record Patterns:
record AccountInfo(String id, CustomerInfo customer) {}
record CustomerInfo(String name, String tier) {}

if (info instanceof AccountInfo(String id, CustomerInfo(String name, String tier))) {
    System.out.println(name + " - " + tier); // Nested destructuring!
}
```

---

## 4. ⭐ Sequenced Collections (JEP 431)

> **Thêm interfaces mới cho collections có thứ tự**

```java
// Trước Java 21: Không có cách thống nhất để lấy first/last element
List<String> list = List.of("a", "b", "c");
String first = list.get(0);          // List: OK
String last  = list.get(list.size() - 1); // Cồng kềnh

LinkedHashSet<String> set = new LinkedHashSet<>(Set.of("a", "b", "c"));
// Set: Không có first/last! Phải convert sang List trước

// Java 21: SequencedCollection interface
// List, Deque, LinkedHashSet, LinkedHashMap đều implement SequencedCollection
list.getFirst();  // "a"
list.getLast();   // "c"
list.reversed();  // ["c", "b", "a"] — View đảo ngược

// LinkedHashSet (ordered Set):
LinkedHashSet<String> orderedSet = new LinkedHashSet<>(List.of("c", "a", "b"));
orderedSet.getFirst(); // "c"
orderedSet.getLast();  // "b"
orderedSet.addFirst("z"); // Thêm vào đầu
orderedSet.addLast("x");  // Thêm vào cuối
orderedSet.reversed();    // Reversed view

// SequencedMap (LinkedHashMap):
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("PENDING", 10);
map.put("PROCESSING", 5);
map.put("COMPLETED", 100);
map.firstEntry();  // Entry("PENDING", 10)
map.lastEntry();   // Entry("COMPLETED", 100)
map.reversed();    // Reversed view of the map
```

---

## 5. Generational ZGC (JEP 439) — GC Performance

```java
// ZGC: Low-latency GC, pause < 1ms bất kể heap size
// Generational ZGC: Thêm Generational approach (Young/Old generation)
// → Hiệu quả hơn cho objects short-lived (hầu hết objects trong banking)

// Kích hoạt:
// JVM flags: -XX:+UseZGC -XX:+ZGenerational  (Java 21)
// application.yml Docker:
// JAVA_OPTS: "-XX:+UseZGC -XX:+ZGenerational -Xmx2g"

// Ưu điểm Generational ZGC vs ZGC cũ:
// - Minor GC (Young gen) nhanh hơn
// - Major GC ít xảy ra hơn
// - Throughput tổng thể cao hơn ~10-20%
// - Pause time vẫn < 1ms

// JVM GC Choice Guide:
// G1GC:          Default, balanced, good for most apps
// ZGC:           Low latency critical (banking APIs, trading)
// Shenandoah:    Tương tự ZGC, từ Red Hat
// ParallelGC:    Maximum throughput, batch processing
```

---

## 6. So Sánh Java 11 → 17 → 21

### Từ Java 11 Lên Java 17

```java
// Java 14: Switch Expression (arrow syntax):
String text = switch (status) { case "DONE" -> "Done"; default -> "Other"; };

// Java 14: instanceof Pattern Matching:
if (obj instanceof String s) { s.toUpperCase(); } // Không cần cast

// Java 15: Text Blocks (multiline strings):
String sql = """
    SELECT id, balance
    FROM bank_accounts
    WHERE customer_id = ?
    AND status = 'ACTIVE'
    """;

// Java 16: Records:
record TransferRequest(String from, String to, BigDecimal amount) {}

// Java 16: Stream.toList() (thay Collections.unmodifiableList(stream.collect(...))):
List<String> ids = transfers.stream().map(Transfer::getId).toList();

// Java 17: Sealed Classes:
sealed interface ApiResponse permits SuccessResponse, ErrorResponse, PendingResponse {}
record SuccessResponse(String data) implements ApiResponse {}
record ErrorResponse(String message, int code) implements ApiResponse {}
// → Compiler biết EXACTLY tất cả subtypes → Exhaustive switch!
```

### Từ Java 17 Lên Java 21

```java
// Java 21: Virtual Threads (lớn nhất!)
spring.threads.virtual.enabled=true

// Java 21: Pattern Matching for switch (finalized):
String result = switch (obj) {
    case Integer i -> "Int: " + i;
    case String s  -> "String: " + s;
    case null      -> "Null!";
    default        -> "Other";
};

// Java 21: Record Patterns (destructuring):
if (obj instanceof TransferRequest(var from, var to, var amount, var desc)) { ... }

// Java 21: Sequenced Collections:
list.getFirst(); list.getLast(); list.reversed();

// Java 21: Generational ZGC
// Java 21: String Templates (preview — finalized Java 23/25)
// Java 21: Unnamed Classes (preview — finalized Java 25)
```

---

## 7. Features Preview Trong Java 21 → Finalized Sau

### String Templates (Preview Java 21, Removed Java 23, Rethinking)

```java
// Preview Java 21 (cú pháp ban đầu):
String accountId = "acc-001";
BigDecimal balance = new BigDecimal("5000000");

// Thay vì String.format() hoặc "..." + var + "...":
String message = STR."Tài khoản \{accountId} có số dư \{balance} VNĐ";
// → "Tài khoản acc-001 có số dư 5000000 VNĐ"

// SQL template:
String query = STR."""
    SELECT * FROM bank_accounts
    WHERE id = '\{accountId}'
    AND balance > \{balance}
    """;

// ⚠️ Lưu ý: Feature này bị REMOVE ở Java 23 để thiết kế lại
// Dự kiến quay lại với thiết kế tốt hơn ở Java 25 hoặc sau
```

### Unnamed Patterns & Variables (Preview Java 21 → Final Java 22)

```java
// Khi không cần biến trong pattern:

// Trước:
switch (obj) {
    case Integer i -> System.out.println("Integer"); // Không dùng i
    case String s  -> System.out.println("String");  // Không dùng s
    default -> {}
}

// Java 22+ (_ = unnamed):
switch (obj) {
    case Integer _ -> System.out.println("Integer"); // _ = tôi biết có nhưng không dùng
    case String _  -> System.out.println("String");
    default -> {}
}

// Trong record destructuring:
if (obj instanceof TransferRequest(var from, _, var amount, _)) {
    // Chỉ lấy 'from' và 'amount', bỏ to và description
    processHighAmount(from, amount);
}

// try-catch (bỏ qua exception variable):
try {
    riskyOperation();
} catch (Exception _) { // Không cần đặt tên 'e' nếu không dùng
    log.warn("Operation failed, retrying...");
}
```

---

## 8. Java 21 Best Practices Cho BankX

```java
// ✅ Dùng Records cho DTOs và Commands:
record InternalTransferCommand(
    String fromAccountId,
    String toAccountId,
    BigDecimal amount,
    String description,
    String idempotencyKey
) {}

// ✅ Dùng Sealed Interface cho Domain Events:
sealed interface TransferEvent
    permits TransferInitiated, TransferCompleted, TransferFailed, TransferReversed {}

record TransferInitiated(String transferId, BigDecimal amount) implements TransferEvent {}
record TransferCompleted(String transferId, Instant completedAt) implements TransferEvent {}
record TransferFailed(String transferId, String reason) implements TransferEvent {}

// Exhaustive switch — Compiler đảm bảo handle tất cả cases:
String message = switch (event) {
    case TransferInitiated e  -> "Transfer " + e.transferId() + " started";
    case TransferCompleted e  -> "Transfer " + e.transferId() + " done";
    case TransferFailed e     -> "Transfer " + e.transferId() + " failed: " + e.reason();
    case TransferReversed e   -> "Transfer " + e.transferId() + " reversed";
    // Không cần default! Compiler verify exhaustiveness
};

// ✅ Virtual Threads cho high-concurrency:
// spring.threads.virtual.enabled: true (BankX đã làm)

// ✅ Text Blocks cho SQL/JSON:
String auditQuery = """
    SELECT t.id, t.amount, t.status,
           a.account_name as from_account,
           b.account_name as to_account
    FROM bank_transfers t
    JOIN bank_accounts a ON a.id = t.from_account_id
    JOIN bank_accounts b ON b.id = t.to_account_id
    WHERE t.created_at >= ?
    ORDER BY t.created_at DESC
    LIMIT ?
    """;

// ✅ Pattern Matching để xử lý responses:
ApiResponse response = callExternalBank(request);
return switch (response) {
    case SuccessResponse s  -> TransferResult.success(s.referenceId());
    case PendingResponse p  -> TransferResult.pending(p.estimatedTime());
    case ErrorResponse e    -> TransferResult.failed(e.message());
};
```

---

## 9. Các Features Java Quan Trọng Từ Java 11-21 — Cheat Sheet

```java
// Java 11:
"  hello  ".strip()         // Trim Unicode whitespace (tốt hơn trim())
"".isBlank()                // Kiểm tra empty hoặc whitespace
"a\nb\nc".lines()           // Stream<String> từng dòng
"abc".repeat(3)             // "abcabcabc"
Path.of("file.txt")         // Tốt hơn Paths.get()
Files.readString(path)      // Đọc file thành String
Files.writeString(path, content)

// Java 12-13:
// (ít feature quan trọng)

// Java 14:
// Switch Expression (arrow), instanceof Pattern Matching (preview)

// Java 15:
// Text Blocks (final), Sealed Classes (preview)

// Java 16:
records, instanceof Pattern Matching (final), Stream.toList()

// Java 17 (LTS):
// Sealed Classes (final), Pattern Matching switch (preview)

// Java 18-20:
// Nhiều JEP preview, không có LTS

// Java 21 (LTS):
// Virtual Threads, Pattern Switch (final), Record Patterns, Sequenced Collections
// Generational ZGC

// Java Collections API Updates:
Map.of("key", "value")           // Immutable map (Java 9+)
List.of("a", "b")                // Immutable list (Java 9+)
Set.copyOf(existingSet)          // Immutable copy (Java 10+)
Map.entry("key", "value")        // Map.Entry (Java 9+)
List.copyOf(existingList)        // Immutable copy (Java 10+)

// Java Optional updates:
Optional<String> opt = Optional.of("value");
opt.ifPresentOrElse(System.out::println, () -> System.out.println("Empty"));
opt.or(() -> Optional.of("fallback")); // Fallback Optional
opt.stream()                           // Optional → Stream (0 hoặc 1 element)

// Java HttpClient (Java 11):
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.napas.com.vn/verify"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

---

**← [README](./README.md)** | **→ [Java 25 Features](./02_java25_new_features.md)**
