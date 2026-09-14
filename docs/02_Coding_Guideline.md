# BankX Digital Banking Platform — Hướng Dẫn Viết Code (Coding Guideline)

Tài liệu này quy định tiêu chuẩn lập trình, quy ước đặt tên, cấu trúc package, và phong cách viết comment bắt buộc trong toàn bộ dự án BankX.

---

## 1. Tiêu Chuẩn Backend (Java 21 / Spring Boot 3)

### 1.1 Cấu Trúc Package Tổng Thể

```
com.bankx
├── shared/                          # Shared utilities (dùng toàn app)
│   ├── domain/
│   │   └── valueobject/
│   │       ├── Money.java           # Không để BigDecimal rải rắc
│   │       ├── AccountNumber.java
│   │       └── CustomerId.java
│   ├── exception/
│   │   ├── BankingException.java    # Base exception
│   │   ├── DomainException.java
│   │   └── ErrorCode.java           # Mã lỗi chuẩn RFC 7807
│   ├── response/
│   │   ├── ApiResponse.java         # Wrapper tất cả response
│   │   └── ApiErrorResponse.java
│   └── util/
│       ├── SecurityUtils.java
│       ├── DateUtils.java
│       └── MaskingUtils.java        # Mask số thẻ, số tài khoản
│
├── auth/                            # Module xác thực
├── customer/                        # Module khách hàng
├── account/                         # Module tài khoản
├── transfer/                        # Module chuyển tiền (phức tạp nhất)
├── ledger/                          # Module sổ cái kép
├── payment/                         # Module thanh toán
├── card/                            # Module thẻ
├── notification/                    # Module thông báo
├── fraud/                           # Module chống gian lận
├── audit/                           # Module nhật ký
└── config/                          # Cấu hình Spring, Security, Kafka, Redis
```

### 1.2 Quy Tắc Đặt Tên (Naming Conventions)

| Loại | Quy tắc | Ví dụ |
|---|---|---|
| Class/Interface | PascalCase | `TransferApplicationService`, `AccountRepository` |
| Method | camelCase | `createTransfer()`, `validateBalance()` |
| Variable | camelCase | `availableBalance`, `idempotencyKey` |
| Constant | UPPER_SNAKE_CASE | `MAX_TRANSFER_AMOUNT`, `OTP_TTL_SECONDS` |
| Package | lowercase.dot.separated | `com.bankx.transfer.domain.model` |
| DB Table | snake_case, số nhiều | `bank_accounts`, `ledger_entries`, `bank_transfers` |
| DB Column | snake_case | `customer_id`, `account_number`, `balance_after` |
| Kafka Topic | kebab-case | `transfer-completed`, `fraud-alert-created` |
| Redis Key | colon-separated | `otp:userId:purpose`, `idempotency:uuid` |
| DTO Suffix | Hậu tố rõ ràng | `CreateTransferRequest`, `TransferResponse` |
| Exception | Suffix Exception | `InsufficientBalanceException`, `AccountFrozenException` |
| UseCase Suffix | Suffix UseCase | `CreateTransferUseCase`, `GetAccountStatementUseCase` |

### 1.3 Domain Value Objects — Bắt Buộc

**SAI (anti-pattern):**
```java
// Đừng để BigDecimal và String rải rắc trong business logic
public void transfer(BigDecimal amount, String fromAccountId, String toAccountId) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("...");
}
```

**ĐÚNG (banking standard):**
```java
// Dùng Value Object để encapsulate validation và semantic
public void transfer(Money amount, AccountId fromAccount, AccountId toAccount) {
    // Money tự validate amount > 0 trong constructor
    // AccountId tự validate UUID format
}

// Money Value Object
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount không được null");
        Objects.requireNonNull(currency, "Currency không được null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Số tiền không được âm");
        }
        // Scale chuẩn 2 chữ số thập phân cho VND
        amount = amount.setScale(0, RoundingMode.HALF_UP); // VND không có xu
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException("Không thể cộng hai loại tiền tệ khác nhau");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        Money result = new Money(this.amount.subtract(other.amount), this.currency);
        if (result.amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Số dư không đủ");
        }
        return result;
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }
}
```

### 1.4 Không Bao Giờ Return JPA Entity Ra API

```java
// ❌ SAI — Làm lộ cấu trúc DB, gây Circular Reference
@GetMapping("/accounts/{id}")
public AccountJpaEntity getAccount(@PathVariable UUID id) {
    return accountRepository.findById(id).orElseThrow();
}

// ✅ ĐÚNG — Map sang DTO trước khi trả về
@GetMapping("/accounts/{id}")
public ApiResponse<AccountResponse> getAccount(@PathVariable UUID id) {
    AccountDetail account = getAccountUseCase.execute(id);
    return ApiResponse.success(AccountMapper.toResponse(account));
}
```

### 1.5 Response Wrapper Chuẩn

```java
// Tất cả API response phải dùng wrapper này
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String traceId,
    Instant timestamp
) {
    // Success response
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, getCurrentTraceId(), Instant.now());
    }

    // Success với message
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, getCurrentTraceId(), Instant.now());
    }
}

// Error response theo RFC 7807 Problem Details
public record ApiErrorResponse(
    String type,         // "https://bankx.vn/errors/insufficient-balance"
    String title,        // "Số dư không đủ"
    int status,          // 422
    String detail,       // "Tài khoản ACC-001 chỉ còn 1,000,000 VND, cần 5,000,000 VND"
    String instance,     // "/api/transfers"
    String traceId,
    Instant timestamp
) {}
```

### 1.6 Exception Handling Toàn Cục

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Xử lý Domain Exception (nghiệp vụ banking)
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Domain exception tại {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError("domain-error", ex.getMessage(), 422, request.getRequestURI());
    }

    // Xử lý Insufficient Balance (422)
    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleInsufficientBalance(InsufficientBalanceException ex, ...) { ... }

    // Xử lý Idempotency conflict (409)
    @ExceptionHandler(DuplicateRequestException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDuplicateRequest(...) { ... }

    // Xử lý Concurrent modification (409 + retry hint)
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleOptimisticLock(...) { ... }
}
```

### 1.7 Database Migration Flyway

```
src/main/resources/db/migration/
├── V1__init_schema.sql              # Initial schema (customers, accounts)
├── V2__create_transfers.sql         # Transfer + Ledger tables
├── V3__create_outbox.sql            # Outbox Pattern table
├── V4__create_audit_logs.sql        # Audit trail
├── V5__add_fraud_tables.sql         # Fraud detection
└── V6__add_card_tables.sql          # Card management

# TUYỆT ĐỐI KHÔNG chỉnh sửa file V cũ đã chạy
# Khi cần thay đổi schema: Tạo V mới
```

**Cấu hình bắt buộc:**
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # KHÔNG dùng create/update — để Flyway quản lý
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 1.8 Logging Rules — Banking Security

```java
// ✅ LOG an toàn — traceId, userId, action, amount range
log.info("Transfer initiated: traceId={}, userId={}, amount={}VND, toAccount={}",
    MDC.get("traceId"), userId, amount, maskedAccountNumber);

// ❌ KHÔNG LOG các thông tin nhạy cảm
log.info("OTP: {}", otp);           // CẤM — lộ OTP
log.info("Password: {}", pw);       // CẤM — lộ mật khẩu
log.info("Token: {}", jwt);         // CẤM — lộ JWT
log.info("CVV: {}", cvv);           // CẤM — lộ CVV
log.info("Full card: {}", card);    // CẤM — dùng masked: **** **** **** 1234

// Mask số tài khoản: Hiển thị 4 số cuối
// 1012345678 → **** 5678
String maskedAccount = MaskingUtils.maskAccountNumber(accountNumber);
```

---

## 2. Tiêu Chuẩn Angular Frontend

### 2.1 Cấu Trúc Folder Angular

```
src/
├── app/
│   ├── core/                        # Singleton services, guards, interceptors
│   │   ├── auth/
│   │   │   ├── auth.service.ts      # Login, logout, token management
│   │   │   ├── auth.guard.ts        # Route protection
│   │   │   ├── role.guard.ts        # RBAC route guard
│   │   │   └── token.service.ts     # JWT parse, storage
│   │   ├── http/
│   │   │   ├── api.interceptor.ts   # Thêm JWT vào mỗi request
│   │   │   ├── error.interceptor.ts # Handle 401, 403, 500 globally
│   │   │   └── loading.interceptor.ts
│   │   ├── services/
│   │   │   ├── storage.service.ts   # localStorage wrapper
│   │   │   └── notification.service.ts
│   │   └── models/
│   │       └── user.model.ts        # Shared models
│   │
│   ├── shared/                      # Reusable components, pipes, directives
│   │   ├── components/
│   │   │   ├── money-display/       # Hiển thị số tiền: 1,000,000 VND
│   │   │   ├── account-card/        # Card tài khoản với số bị mask
│   │   │   ├── transaction-item/    # Item trong danh sách giao dịch
│   │   │   ├── otp-input/           # OTP input 6 chữ số
│   │   │   ├── loading-spinner/
│   │   │   └── empty-state/
│   │   ├── pipes/
│   │   │   ├── currency-vnd.pipe.ts # Format: 1000000 → 1,000,000 VND
│   │   │   ├── mask-account.pipe.ts # **** **** **** 5678
│   │   │   └── relative-date.pipe.ts
│   │   ├── directives/
│   │   │   └── auto-focus.directive.ts
│   │   └── validators/
│   │       ├── account-number.validator.ts
│   │       └── transfer-amount.validator.ts
│   │
│   ├── features/                    # Feature modules (lazy loaded)
│   │   ├── auth/                    # Login, OTP, Biometric
│   │   ├── dashboard/               # Home screen
│   │   ├── accounts/                # Account list, detail, statement
│   │   ├── transfers/               # Transfer flow (form → confirm → OTP → result)
│   │   ├── payments/                # Bill payment, QR
│   │   ├── cards/                   # Card management
│   │   ├── beneficiaries/           # Saved recipients
│   │   ├── notifications/           # Notification center
│   │   ├── promotions/              # Flash sale, cashback
│   │   └── profile/                 # Personal info, security settings
│   │
│   ├── layouts/
│   │   ├── main-layout/             # Layout với bottom nav (mobile)
│   │   ├── auth-layout/             # Layout cho login/OTP screens
│   │   └── admin-layout/            # Sidebar layout cho admin
│   │
│   └── app.routes.ts                # Root routing với lazy loading
│
└── environments/
    ├── environment.ts               # Dev API URL
    └── environment.prod.ts          # Prod API URL
```

### 2.2 State Management Strategy

```typescript
// SIGNALS cho local state (Angular 22)
// Dùng khi state chỉ thuộc về 1 component hoặc 1 feature
@Component({...})
export class AccountListComponent {
  // Signal cho state cục bộ
  accounts = signal<Account[]>([]);
  isLoading = signal(false);
  selectedAccount = signal<Account | null>(null);

  // Computed signal
  totalBalance = computed(() =>
    this.accounts().reduce((sum, acc) => sum + acc.balance, 0)
  );
}

// NgRx cho GLOBAL STATE (auth, user profile)
// Dùng khi nhiều feature cần share cùng state
// ├── state/
// │   ├── auth.state.ts     → Lưu user info, roles
// │   ├── account.state.ts  → Danh sách tài khoản của user
// │   └── notification.state.ts → Số notification unread
```

### 2.3 HTTP Interceptor Chain

```typescript
// api.interceptor.ts — Thêm JWT vào mỗi request
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Lấy token từ storage
    const token = this.tokenService.getAccessToken();

    // Clone request và thêm Authorization header
    const authReq = token
        ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
        : req;

    return next.handle(authReq);
}

// error.interceptor.ts — Handle lỗi toàn cục
// 401 Unauthorized → Thử refresh token → Nếu fail → Logout
// 403 Forbidden → Redirect về trang "Không có quyền truy cập"
// 429 Too Many Requests → Toast "Quá nhiều yêu cầu, vui lòng thử lại sau"
// 500 → Toast "Lỗi hệ thống, vui lòng liên hệ hỗ trợ"
```

### 2.4 TPBank UI Design System (Angular SCSS)

```scss
// banking.scss — Design tokens theo TPBank style
:root {
  // === Màu chủ đạo TPBank (Purple/Violet) ===
  --bank-primary: #7B2D8B;          // Tím TPBank chủ đạo
  --bank-primary-light: #9B4DBB;    // Tím sáng hơn
  --bank-primary-dark: #5B1D6B;     // Tím tối hơn
  --bank-secondary: #F5A623;        // Vàng accent

  // === Gradient (đặc trưng TPBank) ===
  --bank-gradient: linear-gradient(135deg, #7B2D8B 0%, #4A90D9 100%);
  --bank-gradient-card: linear-gradient(135deg, #6B1F7B 0%, #9B4DBB 100%);

  // === Màu nền ===
  --bg-page: #F5F5F5;               // Nền trang (light mode)
  --bg-card: #FFFFFF;               // Nền card, panel
  --bg-bottom-nav: #FFFFFF;         // Bottom navigation

  // === Màu chữ ===
  --text-primary: #1A1A2E;          // Chữ chính
  --text-secondary: #666680;        // Chữ phụ
  --text-amount-positive: #00875A;  // Số tiền nhận (xanh)
  --text-amount-negative: #DE350B;  // Số tiền gửi (đỏ)
  --text-on-primary: #FFFFFF;       // Chữ trên nền tím

  // === Status Colors ===
  --color-success: #00875A;
  --color-error: #DE350B;
  --color-warning: #FF8B00;
  --color-pending: #6554C0;

  // === Spacing ===
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;

  // === Border Radius ===
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --radius-full: 50%;

  // === Shadows ===
  --shadow-card: 0 2px 8px rgba(0, 0, 0, 0.08);
  --shadow-button: 0 4px 12px rgba(123, 45, 139, 0.3);

  // === Font ===
  --font-family: 'Be Vietnam Pro', 'Inter', system-ui, sans-serif;
}
```

---

## 3. Quy Chuẩn Comment Tiếng Việt Bắt Buộc (Javadoc Standard)

### 3.1 Class/Interface Level Comment

```java
/**
 * Service xử lý nghiệp vụ chuyển tiền của BankX Banking Platform.
 *
 * <p>Đây là service trọng tâm nhất của hệ thống, chịu trách nhiệm:
 * <ul>
 *   <li>Xác thực điều kiện chuyển tiền (số dư, trạng thái tài khoản)</li>
 *   <li>Đảm bảo idempotency (chống chuyển tiền trùng lặp)</li>
 *   <li>Ghi ledger entries theo nguyên tắc Double-Entry Bookkeeping</li>
 *   <li>Publish TransferCompletedEvent qua Outbox Pattern</li>
 * </ul>
 *
 * <p><b>Tại sao dùng @Transactional ở đây?</b>
 * Toàn bộ luồng chuyển tiền (debit → credit → ledger → outbox) phải là
 * một ACID transaction. Nếu bất kỳ bước nào thất bại, tất cả phải rollback.
 *
 * <p><b>Tại sao không publish Kafka trực tiếp trong @Transactional?</b>
 * Vì nếu Kafka publish thành công nhưng DB commit fail → dữ liệu không nhất quán.
 * Giải pháp: Ghi Outbox event vào DB trong cùng transaction,
 * sau đó Outbox Poller mới publish Kafka async.
 *
 * @see com.bankx.transfer.infrastructure.messaging.OutboxPoller
 * @see com.bankx.transfer.domain.event.TransferCompletedEvent
 * @since Sprint 06
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TransferApplicationService implements CreateTransferUseCase {
```

### 3.2 Method Level Comment

```java
/**
 * Thực thi lệnh chuyển tiền từ tài khoản nguồn sang tài khoản đích.
 *
 * <p><b>Flow xử lý:</b>
 * <ol>
 *   <li>Kiểm tra idempotency key để tránh chuyển tiền trùng lặp</li>
 *   <li>Validate số dư, trạng thái tài khoản nguồn</li>
 *   <li>Debit tài khoản nguồn (trừ tiền)</li>
 *   <li>Credit tài khoản đích (cộng tiền)</li>
 *   <li>Ghi 2 ledger entries (DEBIT + CREDIT)</li>
 *   <li>Ghi outbox event (publish async qua Kafka)</li>
 * </ol>
 *
 * @param command Lệnh chuyển tiền chứa thông tin tài khoản, số tiền, nội dung
 * @param idempotencyKey UUID duy nhất do client tạo, ngăn chặn request trùng lặp
 * @return TransferResult chứa ID giao dịch và trạng thái
 * @throws InsufficientBalanceException nếu số dư tài khoản nguồn không đủ
 * @throws AccountFrozenException nếu tài khoản nguồn hoặc đích đang bị khóa
 * @throws DuplicateRequestException nếu idempotencyKey đã được xử lý trước đó
 */
public TransferResult execute(CreateTransferCommand command, UUID idempotencyKey) {
```

### 3.3 Complex Logic Comment

```java
// === IDEMPOTENCY CHECK ===
// Tại sao cần step này?
// Kịch bản: User bấm "Chuyển tiền", mạng chập → request lần 2 được gửi
// Nếu không có idempotency → tiền bị chuyển 2 lần!
// Giải pháp: Redis SETNX (Set if Not Exists) — atomic operation
// TTL 10 phút: Đủ để prevent duplicates, không quá dài để block retry hợp lệ
Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(
    IDEMPOTENCY_PREFIX + idempotencyKey,
    TransferStatus.PROCESSING.name(),
    Duration.ofMinutes(10)
);

if (Boolean.FALSE.equals(isFirstRequest)) {
    // Key đã tồn tại → Request này là duplicate
    log.warn("Duplicate transfer request detected: idempotencyKey={}, userId={}",
        idempotencyKey, command.getUserId());
    throw new DuplicateRequestException(
        "Giao dịch đang được xử lý, vui lòng không gửi lại yêu cầu"
    );
}
```

---

## 4. Database Schema Standards

### 4.1 Quy Ước Cột Audit (Bắt Buộc Mọi Bảng)

```sql
-- Mọi bảng đều phải có các cột audit này
created_at      TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
created_by      VARCHAR(100),               -- userId hoặc system
version         BIGINT                      NOT NULL DEFAULT 0  -- Optimistic Lock
```

### 4.2 Soft Delete (Không Xóa Dữ Liệu Banking)

```sql
-- Banking KHÔNG BAO GIỜ hard delete record tài chính
-- Luôn dùng soft delete
is_deleted      BOOLEAN                     NOT NULL DEFAULT FALSE,
deleted_at      TIMESTAMP WITH TIME ZONE,
deleted_by      VARCHAR(100)
```

### 4.3 Sensitive Data Encryption

```sql
-- Dữ liệu nhạy cảm phải được mã hóa ở tầng application trước khi lưu DB
-- Không lưu plaintext: số thẻ đầy đủ, CVV, PIN, CMND số
card_number_encrypted   VARCHAR(512),       -- AES-256-GCM encrypted
pan_token               VARCHAR(100)        -- Tokenized PAN (không phải số thẻ thật)
```

---

## 5. Testing Standards

### 5.1 Test Coverage Minimum

| Layer | Minimum Coverage | Framework |
|---|---|---|
| Domain Model & Value Objects | 95% | JUnit 5 |
| Application Service | 85% | JUnit 5 + Mockito |
| REST Controller | 80% | @WebMvcTest + MockMvc |
| Integration | 70% | @SpringBootTest + Testcontainers |

### 5.2 Test Naming Convention

```java
// Format: methodName_whenCondition_thenExpectedBehavior
@Test
void createTransfer_whenInsufficientBalance_thenThrowInsufficientBalanceException()

@Test
void createTransfer_whenDuplicateIdempotencyKey_thenReturn409Conflict()

@Test
void createTransfer_whenBothAccountsValid_thenDebitAndCreditLedger()

@Test
void createTransfer_whenConcurrentRequests_thenOnlyOneSucceeds()
```

### 5.3 Banking-Specific Test Cases Bắt Buộc

Với Transfer use case, phải test đủ các scenario sau:
- ✅ Happy path: Chuyển tiền thành công
- ✅ Insufficient balance: Số dư không đủ
- ✅ Account frozen: Tài khoản bị khóa
- ✅ Invalid beneficiary: Người nhận không tồn tại
- ✅ Duplicate request: Cùng idempotency key
- ✅ OTP invalid: OTP sai
- ✅ OTP expired: OTP hết hạn
- ✅ Concurrent transfer: 2 request đồng thời trừ vượt số dư
- ✅ Amount exceeds daily limit: Vượt hạn mức ngày
- ✅ Rollback: Debit thành công nhưng credit fail → rollback

---

## 6. Git Workflow & Commit Convention

```
# Branch naming
feature/sprint-06-transfer-core
feature/sprint-07-otp-flow
bugfix/fix-concurrent-race-condition
hotfix/fix-balance-negative

# Commit message format
feat(transfer): thêm idempotency check trước khi xử lý chuyển tiền
fix(account): sửa race condition khi hai request cùng trừ tiền
refactor(ledger): tách domain model khỏi JPA entity
test(transfer): thêm concurrent transfer test cases
docs(sprint-06): cập nhật sequence diagram chuyển tiền
chore(deps): nâng Spring Boot lên 3.3.2
```
