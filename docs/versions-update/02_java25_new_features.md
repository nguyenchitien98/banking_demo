# Java 25 — LTS Mới Nhất (Tháng 9/2025)

> **Java 25 = LTS tiếp theo sau Java 21** — Những gì được finalized từ Java 22-25  
> Nhiều features preview từ Java 21 đã được hoàn thiện và stable

---

## 📋 Những JEP Quan Trọng Nhất Trong Java 22-25

| JEP | Tên | Version | Trạng Thái | Mức Độ |
|---|---|---|---|---|
| 454 | Foreign Function & Memory API | Java 22 | **Final** ⭐⭐ | Interop C/native |
| 456 | Unnamed Variables & Patterns | Java 22 | **Final** ⭐⭐ | Code cleaner |
| 461 | Stream Gatherers | Java 22 Preview → Java 24 Final | **Final** ⭐⭐⭐ | Stream power-up |
| 484 | Class-File API | Java 24 | **Final** ⭐ | Bytecode tooling |
| 485 | Stream Gatherers | Java 24 | **Final** ⭐⭐⭐ | — |
| 483 | AOT Class Loading & Linking | Java 24 | **Final** ⭐⭐ | Startup time |
| 495 | Simple Source Files (Unnamed Classes) | Java 24 | **Final** ⭐ | Beginner friendly |
| 499 | Structured Concurrency | Java 25 | **Final** ⭐⭐⭐ | Concurrent code |
| 487 | Scoped Values | Java 25 | **Final** ⭐⭐⭐ | ThreadLocal thay thế |
| 492 | Flexible Constructor Bodies | Java 25 | **Final** ⭐⭐ | OOP improvement |
| 488 | Primitive Types in Patterns | Java 25 | **Final** ⭐⭐ | Pattern completeness |
| 494 | Module Import Declarations | Java 25 | **Final** ⭐ | Module system |
| — | Value Objects (Valhalla) | Java 25+ | Preview | Performance |

---

## 1. ⭐⭐⭐ Structured Concurrency (JEP 499) — Finalized Java 25

> **Làm cho concurrent code dễ đọc, dễ debug, dễ cancel hơn bao giờ hết**

### Vấn Đề Với ExecutorService Cũ

```java
// Cách cũ — Khó quản lý lifecycle, error handling phức tạp:
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

Future<AccountInfo> accountFuture = executor.submit(() -> fetchAccount(accountId));
Future<FraudScore> fraudFuture   = executor.submit(() -> calculateFraud(transferId));
Future<LimitInfo> limitFuture    = executor.submit(() -> checkLimit(accountId));

try {
    AccountInfo account = accountFuture.get();  // Nếu fraud task fail → Không cancel account task
    FraudScore fraud    = fraudFuture.get();    // Phải manually cancel tất cả khi có lỗi
    LimitInfo limit     = limitFuture.get();
    // ...
} catch (Exception e) {
    accountFuture.cancel(true); // Phải nhớ cancel từng cái → Dễ quên → Resource leak
    fraudFuture.cancel(true);
    limitFuture.cancel(true);
}
```

### Structured Concurrency — Giải Pháp

```java
// Java 25: StructuredTaskScope
// Đảm bảo: Khi scope đóng → Tất cả tasks phải xong (hoặc cancelled)

import java.util.concurrent.StructuredTaskScope;

// === Pattern 1: ShutdownOnFailure — Cancel tất cả khi có 1 task fail ===
// Dùng khi: Tất cả tasks phải thành công, thiếu 1 là vô nghĩa
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    // Khởi động 3 tasks song song:
    StructuredTaskScope.Subtask<AccountInfo> accountTask =
        scope.fork(() -> fetchAccount(accountId));    // Virtual Thread

    StructuredTaskScope.Subtask<FraudScore> fraudTask =
        scope.fork(() -> calculateFraud(transferId));

    StructuredTaskScope.Subtask<LimitInfo> limitTask =
        scope.fork(() -> checkDailyLimit(accountId));

    scope.join();           // Chờ tất cả hoàn thành
    scope.throwIfFailed();  // Ném exception nếu có task nào fail
    // Nếu fraudTask fail → scope tự động cancel accountTask và limitTask!
    // Không cần manual cancel!

    // Lấy kết quả (an toàn sau join + throwIfFailed):
    AccountInfo account = accountTask.get();
    FraudScore  fraud   = fraudTask.get();
    LimitInfo   limit   = limitTask.get();

    return new TransferPrecheck(account, fraud, limit);
} // Scope đóng: Đảm bảo tất cả tasks kết thúc (cancel nếu cần)

// === Pattern 2: ShutdownOnSuccess — Dừng ngay khi 1 task thành công ===
// Dùng khi: Chỉ cần 1 trong nhiều tasks thành công (race pattern)
try (var scope = new StructuredTaskScope.ShutdownOnSuccess<ExchangeRate>()) {
    scope.fork(() -> fetchRateFromProvider1("USD/VND")); // Provider 1
    scope.fork(() -> fetchRateFromProvider2("USD/VND")); // Provider 2 (backup)
    scope.fork(() -> fetchRateFromProvider3("USD/VND")); // Provider 3 (backup)

    scope.join();
    ExchangeRate rate = scope.result(); // Kết quả của task thành công đầu tiên
    // Các tasks còn lại tự động bị cancel!
}

// === Timeout ===
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    scope.fork(() -> fetchAccount(accountId));
    scope.fork(() -> calculateFraud(transferId));

    scope.joinUntil(Instant.now().plusSeconds(5)); // Timeout 5 giây
    scope.throwIfFailed();
    // Nếu quá 5 giây → Scope cancel tất cả tasks tự động
}
```

### Tại Sao Structured Concurrency Tốt Hơn?

```
"Structured" vì có lifecycle rõ ràng như structured programming:
  - Scope bắt đầu → Fork tasks → Join → Scope kết thúc
  - Khi scope kết thúc → Đảm bảo KHÔNG có "orphan threads" chạy ngầm
  - Giống try-with-resources nhưng cho concurrent tasks

Lợi ích:
  1. Code dễ đọc hơn: Rõ ràng "tasks này chạy song song, scope này quản lý chúng"
  2. Error handling đơn giản: Không cần manual cancel từng Future
  3. Không resource leak: Scope tự cleanup khi đóng
  4. Thread dump dễ debug: Subtasks thấy rõ parent scope trong stack trace
  5. Cancellation propagation: Cancel scope → Tất cả subtasks bị cancel
```

---

## 2. ⭐⭐⭐ Scoped Values (JEP 487) — ThreadLocal Thay Thế

> **ThreadLocal an toàn hơn, immutable, phù hợp với Virtual Threads**

### Vấn Đề Của ThreadLocal

```java
// ThreadLocal problems:
// 1. Mutable — Có thể thay đổi bất kỳ lúc nào → Khó trace
// 2. Phải cleanup manually (remove()) → Dễ quên → Memory leak trong thread pool
// 3. Không tương thích tốt với Virtual Threads (nhiều VT → nhiều ThreadLocal entries)
// 4. Child threads không inherits ThreadLocal của parent
```

### ScopedValue — Giải Pháp

```java
import java.lang.ScopedValue;

// ScopedValue = Immutable, scoped, inheritable by child threads
public class RequestContext {
    // Khai báo ScopedValue (giống static, chia sẻ declaration nhưng không phải value)
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> USER_ID  = ScopedValue.newInstance();
}

// Usage — Bind value cho scope:
String traceId = generateTraceId();
String userId  = getCurrentUserId();

// ScopedValue.where() → Bind value CHỈ TRONG PHẠM VI runnable
ScopedValue.where(RequestContext.TRACE_ID, traceId)
    .where(RequestContext.USER_ID, userId)
    .run(() -> {
        // Trong đây: TRACE_ID và USER_ID có giá trị
        processTransfer(transferId);
        // Khi gọi phương thức khác, ScopedValue tự động truyền qua
        sendNotification(); // sendNotification() cũng đọc được TRACE_ID!
    });
// Sau đây: ScopedValue tự động unbound → Không cần cleanup!

// Đọc trong bất kỳ method nào (miễn là trong scope):
public void sendNotification() {
    String traceId = RequestContext.TRACE_ID.get();    // Lấy giá trị
    String userId  = RequestContext.USER_ID.get();
    log.info("[{}] Sending notification to {}", traceId, userId);
}

// Kiểm tra có được set không:
if (RequestContext.TRACE_ID.isBound()) {
    String tid = RequestContext.TRACE_ID.get();
}

// ✅ Với Virtual Threads + Structured Concurrency:
ScopedValue.where(RequestContext.TRACE_ID, traceId).run(() -> {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        scope.fork(() -> {
            // Child Virtual Thread TỰ ĐỘNG THẤY ScopedValue từ parent!
            String tid = RequestContext.TRACE_ID.get(); // ← Có giá trị!
            return fetchAccount(accountId);
        });
        scope.join();
    }
});
```

### ThreadLocal vs ScopedValue

| | ThreadLocal | ScopedValue |
|---|---|---|
| **Mutability** | Mutable | **Immutable** |
| **Cleanup** | Phải gọi `remove()` | Tự động khi scope kết thúc |
| **Virtual Threads** | Có vấn đề (memory) | **Tối ưu** |
| **Child threads** | Không inherit (dùng `InheritableThreadLocal`) | **Tự động inherit** |
| **Scope** | Tồn tại theo thread lifetime | **Lexical scope** |
| **Debug** | Khó | Dễ hơn (scope rõ ràng) |

---

## 3. ⭐⭐⭐ Stream Gatherers (JEP 485) — Finalized Java 24

> **Custom intermediate Stream operations** — Mở rộng Stream API vô hạn

### Vấn Đề Hiện Tại

```java
// Stream API có sẵn nhiều operations nhưng thiếu một số use cases:
// - Sliding window
// - Fixed-size batching
// - Take-while với state
// - Scan/running accumulate
// → Trước Java 24: Phải dùng for-loop hoặc custom Collector (phức tạp)
```

### Stream.gather() — Custom Operations

```java
import java.util.stream.Gatherers;

// === Sliding Window — Cửa sổ trượt ===
// Chia stream thành các window chồng lấp nhau:
List<List<BigDecimal>> windows = transfers.stream()
    .map(Transfer::getAmount)
    .gather(Gatherers.windowSliding(3))
    // [100, 200, 300, 400, 500]
    // → [[100,200,300], [200,300,400], [300,400,500]]
    .toList();

// Use case BankX: Phát hiện pattern giao dịch tăng liên tiếp
boolean increasingPattern = transfers.stream()
    .map(Transfer::getAmount)
    .gather(Gatherers.windowSliding(3))
    .anyMatch(window ->
        window.get(0).compareTo(window.get(1)) < 0 &&
        window.get(1).compareTo(window.get(2)) < 0
    ); // 3 giao dịch liên tiếp tăng dần → Fraud signal?

// === Fixed-Size Windows (non-overlapping) ===
// Chia thành batches bằng nhau:
List<List<Transfer>> batches = pendingTransfers.stream()
    .gather(Gatherers.windowFixed(100))
    // → [[t1..t100], [t101..t200], ...]
    .toList();
// Use case: Xử lý transfers theo batch 100 cái một

// === Fold (Running Accumulate / Scan) ===
// Như reduce nhưng emit intermediate values
Stream<BigDecimal> runningBalance = ledgerEntries.stream()
    .map(e -> e.getEntryType() == CREDIT ? e.getAmount() : e.getAmount().negate())
    .gather(Gatherers.fold(BigDecimal.ZERO, BigDecimal::add));
// → [100, 150, 50, 300, ...]  (số dư tích lũy sau mỗi giao dịch)

// === Custom Gatherer — Ví Dụ: Take Until Condition ===
// Lấy các transfers cho đến khi tổng vượt ngưỡng:
Gatherer<Transfer, ?, Transfer> takeUntilLimitExceeded =
    Gatherer.ofSequential(
        () -> new BigDecimal[]{BigDecimal.ZERO}, // Initial state
        (state, transfer, downstream) -> {
            state[0] = state[0].add(transfer.getAmount());
            if (state[0].compareTo(new BigDecimal("10000000")) <= 0) {
                return downstream.push(transfer); // Tiếp tục
            }
            return false; // Dừng lại
        }
    );

List<Transfer> transfersUnderLimit = dailyTransfers.stream()
    .gather(takeUntilLimitExceeded)
    .toList();
```

---

## 4. ⭐⭐ Flexible Constructor Bodies (JEP 492)

> **Cho phép code trước `super()` và `this()` trong constructor**

```java
// Trước Java 25: super() PHẢI là statement đầu tiên trong constructor
class InterestBearingAccount extends BankAccount {
    InterestBearingAccount(String accountId, BigDecimal initialBalance) {
        // ❌ KHÔNG được làm gì trước super()
        // validation phải làm trong factory method
        super(accountId, initialBalance); // Phải là dòng đầu!
        this.interestRate = BigDecimal.ZERO;
    }
}

// Java 25: Có thể làm validation/preparation trước super()
class InterestBearingAccount extends BankAccount {
    private final BigDecimal interestRate;

    InterestBearingAccount(String accountId, BigDecimal initialBalance) {
        // ✅ Giờ được phép code trước super() với điều kiện:
        // - Không access 'this' trước super()
        // - Không call instance methods trước super()

        // Validation trước super():
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID không được trống");
        }
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư ban đầu không thể âm");
        }

        // Tính toán trước super():
        BigDecimal adjustedBalance = applyOpeningBonus(initialBalance);

        // Bây giờ mới gọi super():
        super(accountId, adjustedBalance);

        // Code sau super() vẫn như cũ:
        this.interestRate = calculateInitialRate(adjustedBalance);
    }

    // Static method được phép gọi trước super():
    private static BigDecimal applyOpeningBonus(BigDecimal balance) {
        return balance.add(new BigDecimal("100000")); // Bonus 100k khi mở tài khoản
    }
}
```

---

## 5. ⭐⭐ Primitive Types in Patterns (JEP 488)

> **Hoàn thiện Pattern Matching — Cho phép primitive types**

```java
// Trước Java 25: Pattern Matching không hỗ trợ primitive types
Object amount = 500000; // int autoboxed thành Integer

switch (amount) {
    case Integer i -> System.out.println("Integer: " + i); // OK (Integer, không phải int)
    // Không thể: case int i -> ... (int là primitive, không phải type)
}

// Java 25: Primitive Types trong Patterns!
Object value = getTransactionValue();

switch (value) {
    case int i when i < 0    -> throw new IllegalArgumentException("Âm");
    case int i when i == 0   -> System.out.println("Zero");
    case int i               -> System.out.println("Positive: " + i);
    case long l              -> System.out.println("Long: " + l);
    case double d            -> System.out.println("Double: " + d);
    case String s            -> System.out.println("String: " + s);
    case null                -> System.out.println("Null");
}

// instanceof với primitive:
if (value instanceof int i) {
    System.out.println("Is int: " + i);
}

// Kết hợp với số tiền:
void processAmount(Object amount) {
    switch (amount) {
        case int i when i > 100_000_000  -> flagForReview(i);   // > 100 triệu
        case int i when i > 0            -> processNormal(i);
        case int i                        -> throw new InvalidAmountException();
        case BigDecimal d when d.scale() > 4 -> throw new PrecisionException();
        case BigDecimal d                 -> processDecimalAmount(d);
        default -> throw new UnsupportedTypeException();
    }
}
```

---

## 6. ⭐⭐ AOT Class Loading (JEP 483) — Startup Time

> **Giảm thời gian khởi động Spring Boot đáng kể**

```
AOT = Ahead-of-Time Class Loading & Linking

Vấn đề truyền thống:
  JVM start → Load classes từ JAR → Parse bytecode → Link → JIT compile
  Spring Boot startup: 5-10 giây (tải hàng nghìn classes)

AOT solution:
  Lần đầu tiên: JVM "học" classes nào được load và link
  Lưu vào class data archive (.jsa file)
  Những lần tiếp theo: Load từ archive (đã parsed + linked)
  → Startup time giảm 30-50%!

Cách dùng:
  # Bước 1: Training run (chỉ làm 1 lần)
  java -XX:AOTMode=record -XX:AOTConfiguration=app-aot.aotconf -jar app.jar
  
  # Bước 2: Normal run với AOT cache
  java -XX:AOTMode=on -XX:AOTConfiguration=app-aot.aotconf -jar app.jar
  → Khởi động nhanh hơn đáng kể!

Spring Boot 3.3+:
  # spring-boot-maven-plugin hỗ trợ AOT compilation
  ./mvnw spring-boot:process-aot
  ./mvnw spring-boot:build-image
```

---

## 7. ⭐ Simple Source Files / Unnamed Classes (JEP 495) — Java 24

> **Dành cho người mới học Java — Không cần class declaration**

```java
// Trước: Phải có class + main method:
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}

// Java 24+ (Unnamed Class): Chỉ cần viết thẳng code!
// File: HelloWorld.java (không cần khai báo class)
void main() {
    System.out.println("Hello, World!");
    var name = "BankX";
    System.out.println("Welcome to " + name);
}
// Chạy: java HelloWorld.java
// Phù hợp cho: Script nhỏ, giảng dạy, prototyping

// BankX không dùng feature này (quá đơn giản cho production)
// Nhưng hay cho: Viết quick test scripts
```

---

## 8. Value Objects / Project Valhalla (Preview Giai Đoạn Này)

> **Đây là feature quan trọng nhất dài hạn của Java — Đang preview**

```java
// Value Classes: Objects không có identity, pure data
// → Có thể lưu trên stack như primitive types → Không GC pressure

// Concept (cú pháp có thể thay đổi):
value class Money {
    BigDecimal amount;
    String currency;

    Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }
}
// Money objects: Không có ==, không có identity
// → Compiler có thể inline vào stack như int/long
// → Tạo hàng triệu Money objects mà không GC pressure

// Ý nghĩa cho BankX:
// BigDecimal là reference object → Nhiều allocations → GC pressure
// Nếu BigDecimal trở thành value type → 0 GC pressure
// Java tiến về hướng: Everything is an object, nhưng object có thể inline như primitive

// Hiện tại Java 25: Vẫn là preview, cú pháp có thể thay đổi
// Dự kiến finalized: Java 27-29
```

---

## 9. Foreign Function & Memory API (JEP 454) — Final Java 22

> **Gọi native code (C/C++) và quản lý native memory mà không cần JNI**

```java
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

// Gọi hàm C từ Java mà không cần JNI:
try (Arena arena = Arena.ofConfined()) {
    // Tìm hàm 'strlen' từ C standard library:
    Linker linker = Linker.nativeLinker();
    SymbolLookup stdlib = linker.defaultLookup();

    MethodHandle strlen = linker.downcallHandle(
        stdlib.find("strlen").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    );

    // Tạo native string:
    MemorySegment str = arena.allocateFrom("Hello from BankX!");

    // Gọi strlen:
    long length = (long) strlen.invoke(str);
    System.out.println("Length: " + length); // 18
}

// Ứng dụng banking:
// - Gọi native crypto library (HSM - Hardware Security Module) cho key management
// - Interface với legacy C banking systems (COBOL/C mainframes)
// - Native memory cho zero-copy I/O
```

---

## 10. So Sánh Java 21 vs Java 25 — Quick Reference

| Feature | Java 21 | Java 25 |
|---|---|---|
| Virtual Threads | ✅ Final | ✅ Stable |
| Pattern Matching Switch | ✅ Final | ✅ + Primitive types |
| Records | ✅ Final | ✅ |
| Sealed Classes | ✅ Final | ✅ |
| Sequenced Collections | ✅ Final | ✅ |
| Structured Concurrency | 🔄 Preview (JEP 453) | ✅ **Final** |
| Scoped Values | 🔄 Preview | ✅ **Final** |
| Stream Gatherers | ❌ | ✅ **Final** (Java 24) |
| Flexible Constructor Bodies | ❌ | ✅ **Final** |
| Primitive Types in Patterns | ❌ | ✅ **Final** |
| AOT Class Loading | ❌ | ✅ **Final** (Java 24) |
| Foreign Function & Memory API | 🔄 Preview | ✅ **Final** (Java 22) |
| String Templates | 🔄 Preview | ⚠️ Removed & Rethinking |
| Value Objects (Valhalla) | ❌ | 🔄 Preview |
| Unnamed Classes | 🔄 Preview | ✅ **Final** (Java 24) |

---

## 11. Nên Upgrade BankX Lên Java 25 Không?

```
Tại sao NÊN upgrade (khi Java 25 đủ mature):
  ✅ Structured Concurrency → Code concurrent đẹp hơn nhiều
  ✅ Scoped Values → Thay ThreadLocal cho tracing
  ✅ Stream Gatherers → Fraud detection patterns
  ✅ AOT → Startup time nhanh hơn (Docker container)
  ✅ LTS → 8 năm support

Tại sao CHƯA cần rush:
  ⏳ Spring Boot vẫn cần thời gian để fully support Java 25
  ⏳ 3rd party dependencies cần update
  ⏳ Java 21 vẫn đang trong LTS support period
  ⏳ "If it ain't broke, don't fix it" cho production banking

Recommendation:
  Học Java 25 features → Áp dụng vào code mới khi Spring support hoàn thiện
  Migration: Java 21 → Java 25 không có breaking changes lớn
  Timeline: Upgrade sau khi Spring Boot 3.4+ fully tested với Java 25
```

---

**← [Java 21 Features](./01_java21_new_features.md)** | **→ [Angular 22 Features](./03_angular22_new_features.md)**
