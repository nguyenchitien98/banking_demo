---
name: banking-java-backend
description: >
  Skill dành cho việc implement backend Java Spring Boot cho BankX Digital Banking Platform.
  Trigger khi: viết code Java banking, implement module transfer/account/auth/payment/ledger,
  xử lý Kafka, Redis, Outbox Pattern, Saga, Idempotency, Optimistic Lock, Clean Architecture.
---

# Skill: BankX Java Banking Backend

## Khi Nào Trigger
- Yêu cầu viết Java code cho banking module (transfer, account, auth, payment, ledger, card, fraud...)
- Implement Outbox Pattern, Saga, Idempotency, Optimistic Lock
- Viết Kafka Producer/Consumer cho banking events
- Tạo Flyway migration scripts
- Implement Clean Architecture / Hexagonal Architecture
- Viết Unit Test / Integration Test cho banking use cases

## Bắt Buộc Đọc Trước Khi Code

1. `banking/docs/01_Architecture_Bible.md` — Module flow, sequence diagrams, dependency rules
2. `banking/docs/02_Coding_Guideline.md` — Coding standards, Javadoc chuẩn, exceptions
3. `banking/docs/06_Database_Schema.md` — Bảng DB hiện tại
4. `banking/docs/04_Sprint_Plan.md` — Sprint scope và công nghệ được phép dùng

## Package Structure Template

```
com.bankx.{module}/
├── domain/
│   ├── model/         # Aggregate Roots, Entities
│   ├── valueobject/   # Money, AccountId, TransferId...
│   ├── service/       # Domain Services (business rules không thuộc Entity)
│   ├── repository/    # Repository INTERFACES (ports)
│   ├── event/         # Domain Events
│   └── exception/     # Domain Exceptions
├── application/
│   ├── usecase/       # Use Case Interfaces
│   ├── command/       # CQRS Write
│   ├── query/         # CQRS Read
│   └── service/       # Application Services (implements use cases)
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/    # JPA Entities (KHÁC domain model)
│   │   ├── mapper/    # Domain ↔ JPA Entity mappers
│   │   └── repository/ # Implements domain repository interfaces
│   ├── messaging/     # Kafka producers/consumers
│   └── external/      # External API clients
└── presentation/
    ├── controller/    # REST Controllers
    └── dto/
        ├── request/
        └── response/
```

## Code Templates

### Domain Entity Template
```java
/**
 * [Tên Aggregate Root] — Đại diện cho [nghiệp vụ gì].
 *
 * <p>[Mô tả chi tiết nghiệp vụ của entity này]
 *
 * <p><b>Tại sao đây là Aggregate Root?</b>
 * [Giải thích]
 *
 * @see [Related class]
 * @since Sprint [N]
 */
public class BankAccount {
    private AccountId id;
    private CustomerId customerId;
    private AccountNumber accountNumber;
    private Money balance;
    private AccountStatus status;
    private long version;  // Optimistic Lock

    /**
     * Rút tiền khỏi tài khoản.
     *
     * <p>Đây là Domain Method — business rule được enforce ở tầng domain,
     * không thể bypass từ bất kỳ entry point nào.
     *
     * @param amount Số tiền cần rút (phải > 0)
     * @throws InsufficientBalanceException nếu số dư không đủ
     * @throws AccountFrozenException nếu tài khoản đang bị khóa
     */
    public void withdraw(Money amount) {
        // Kiểm tra trạng thái tài khoản trước khi cho rút tiền
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(
                String.format("Tài khoản %s đang ở trạng thái %s, không thể rút tiền",
                    accountNumber.value(), status)
            );
        }
        // Money.subtract() tự kiểm tra balance >= 0, throw nếu âm
        this.balance = this.balance.subtract(amount);
    }
}
```

### Application Service Template
```java
/**
 * Application Service xử lý nghiệp vụ [Tên Feature].
 *
 * <p>[Mô tả flow chính]
 *
 * <p><b>Tại sao @Service?</b>
 * Spring @Service đánh dấu đây là bean nghiệp vụ, được quản lý bởi Spring IoC Container.
 * Khác với @Component (generic), @Service thể hiện rõ role là business logic layer.
 *
 * <p><b>Tại sao @Transactional?</b>
 * [Giải thích boundary của transaction]
 *
 * @since Sprint [N]
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class [Feature]ApplicationService implements [Feature]UseCase {

    private final [Feature]Repository repository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * [Mô tả method]
     *
     * @param command [Mô tả input]
     * @param idempotencyKey UUID do client sinh ra để chống duplicate
     * @return [Mô tả output]
     * @throws DuplicateRequestException nếu idempotencyKey đã xử lý trước đó
     */
    @Override
    public [Result] execute([Command] command, UUID idempotencyKey) {
        // === IDEMPOTENCY CHECK ===
        // Tại sao check đầu tiên? Để prevent business logic chạy 2 lần
        // ngay cả khi có race condition từ 2 concurrent requests cùng key
        checkIdempotency(idempotencyKey);

        // === BUSINESS LOGIC ===
        // [Implementation]

        // === OUTBOX EVENT ===
        // Ghi event vào DB cùng transaction thay vì publish Kafka trực tiếp
        // → Đảm bảo atomicity giữa DB và Kafka
        outboxRepository.save(buildOutboxEvent(result));

        return result;
    }
}
```

### Controller Template
```java
/**
 * REST Controller cho [Tên Module] API.
 *
 * <p><b>Tại sao @RestController?</b>
 * Kết hợp @Controller + @ResponseBody, tự động serialize response sang JSON.
 * Phù hợp cho REST API không cần View/Template engine.
 *
 * <p><b>Tại sao @RequestMapping("/api/v1/[path]")?</b>
 * Versioning API từ v1 để dễ backward compatibility khi cần v2.
 *
 * @since Sprint [N]
 */
@RestController
@RequestMapping("/api/v1/[path]")
@RequiredArgsConstructor
@Slf4j
public class [Module]Controller {

    private final [UseCase] useCase;

    /**
     * [Mô tả endpoint]
     *
     * @param request Request body chứa [mô tả fields]
     * @param idempotencyKey UUID client sinh để chống duplicate (Header: Idempotency-Key)
     * @param authentication Spring Security context chứa thông tin user đang đăng nhập
     * @return ApiResponse wrapping [ResultType]
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<[Response]> create(
        @Valid @RequestBody [Request] request,
        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
        Authentication authentication
    ) {
        log.info("Request [action]: userId={}, traceId={}",
            authentication.getName(),
            MDC.get("traceId")
        );
        [Result] result = useCase.execute(toCommand(request, authentication), idempotencyKey);
        return ApiResponse.success([Mapper].toResponse(result));
    }
}
```

## Banking Checklist (Tự Check Trước Khi Done)

```
Security:
  [ ] Endpoint có @PreAuthorize với đúng role không?
  [ ] Có kiểm tra ownership không? (User chỉ thao tác resource của mình)
  [ ] Có log sensitive data không? (OTP, password, CVV, card, JWT)
  [ ] SQL injection safe? (parameterized queries)

Concurrency:
  [ ] Balance update có @Version (Optimistic Lock) không?
  [ ] Transfer có Idempotency check không?
  [ ] Kafka consumer có idempotent check không?

Architecture:
  [ ] Domain layer có import Spring/JPA không? (ĐÃI KỲ: KHÔNG được)
  [ ] Có return JPA Entity ra API không? (KHÔNG được)
  [ ] Có publish Kafka trực tiếp trong @Transactional không? (KHÔNG được, dùng Outbox)
  [ ] Module boundaries có bị vi phạm không?

Database:
  [ ] Có Flyway migration mới không? (nếu cần schema mới)
  [ ] Hibernate ddl-auto=validate không?
  [ ] Financial records có soft delete không? (KHÔNG được)

Javadoc:
  [ ] Tất cả class public có Javadoc tiếng Việt không?
  [ ] Tất cả method public có @param, @return, @throws không?
  [ ] Logic phức tạp có comment giải thích "Tại sao" không?

Testing:
  [ ] Happy path test?
  [ ] Insufficient balance test?
  [ ] Duplicate request test?
  [ ] Account frozen test?
  [ ] Concurrent transfer test?
```
