# BankX Digital Banking Platform — AI Agent Rules

Đây là file rules dành riêng cho workspace `banking/`. Tất cả AI Agents (Gemini, Claude, Cursor, Copilot) PHẢI tuân thủ các quy tắc này khi làm việc trong thư mục `banking/`.

---

## 1. Nguyên Tắc Cốt Lõi (Core Mandate)

### 1.1 Không Code Giả — Tuyệt Đối
```
❌ CẤM: // TODO implement later
❌ CẤM: return null;
❌ CẤM: throw new UnsupportedOperationException("Not implemented yet");
❌ CẤM: System.out.println(...) thay vì Logger
```

Nếu feature chưa implement được hoàn toàn: **Viết Mock/Stub chạy được** với data cứng nhưng có cấu trúc đúng.

### 1.2 Javadoc Tiếng Việt — Bắt Buộc
Xem chi tiết trong `banking/docs/02_Coding_Guideline.md#3`. Tóm tắt:
- **Mọi class public:** Javadoc mô tả vai trò nghiệp vụ
- **Mọi method public/protected:** Javadoc với @param, @return, @throws
- **Mọi logic banking phức tạp:** Comment inline giải thích "Tại sao thiết kế thế này?"
- **Nếu class có Annotation level (như @RestController, @Service):** Javadoc PHẢI giải thích lý do dùng annotation đó

### 1.3 Tên biến, class, method — Tiếng Anh
Code phải dùng English. Chỉ comment/Javadoc mới dùng Tiếng Việt.

---

## 2. Banking Security Rules (Không Được Vi Phạm)

```java
// ❌ NGHIÊM CẤM — Log sensitive data
log.info("OTP generated: {}", otp);
log.debug("Password attempt: {}", password);
log.info("JWT token: {}", accessToken);
log.info("Card number: {}", cardNumber);

// ✅ ĐÚNG — Mask trước khi log
log.info("OTP sent to user: {}", MaskingUtils.maskPhone(phone));
log.info("Transfer from: {} amount: {}VND", MaskingUtils.maskAccountNumber(accNo), amount);
```

```java
// ❌ NGHIÊM CẤM — Return JPA Entity trực tiếp
@GetMapping("/accounts/{id}")
public AccountJpaEntity getAccount(@PathVariable UUID id) { ... }

// ✅ ĐÚNG — Map sang Response DTO
@GetMapping("/accounts/{id}")
public ApiResponse<AccountResponse> getAccount(@PathVariable UUID id) { ... }
```

```java
// ❌ NGHIÊM CẤM — Balance không có Optimistic Lock
account.setBalance(account.getBalance().subtract(amount)); // Race condition!

// ✅ ĐÚNG — Dùng @Version và domain method
account.withdraw(amount); // Trong BankAccount domain entity, có @Version
```

---

## 3. Architecture Rules

### 3.1 Dependency Direction (Phải Tuân Thủ Tuyệt Đối)
```
Presentation (Controller/DTO)
    ↓
Application (UseCase/Service)
    ↓
Domain (Model/ValueObject/Repository Interface)
    ↑
Infrastructure (JPA/Redis/Kafka Adapters)
```

- Domain KHÔNG ĐƯỢC import Spring, JPA, Kafka, Redis
- Infrastructure implements Domain interfaces
- Application KHÔNG ĐƯỢC import Infrastructure trực tiếp (chỉ qua Domain interfaces)

### 3.2 Module Boundaries
```
Transfer module:  KHÔNG import Payment module internal classes
Payment module:   KHÔNG import Transfer module internal classes
Notification:     Chỉ consume Kafka events, KHÔNG gọi trực tiếp Transfer/Account service
Audit:            Chỉ consume events, KHÔNG gọi API các module khác
```

### 3.3 Database Rules
- Flyway PHẢI được dùng — KHÔNG dùng `spring.jpa.hibernate.ddl-auto=create/update`
- Migration file mới: `Vx__description.sql` — KHÔNG edit file cũ
- Financial tables (`transactions`, `ledger_entries`): KHÔNG có soft delete, KHÔNG update
- Mọi bảng transaction phải có `version` column cho Optimistic Lock

---

## 4. Banking-Specific Rules

### 4.1 Transfer/Payment Operations
```java
// Mọi endpoint thay đổi số dư PHẢI:
// 1. Kiểm tra Idempotency Key (Header: Idempotency-Key)
// 2. Validate ownership (user chỉ thao tác account của mình)
// 3. Atomic transaction (@Transactional)
// 4. Ghi Ledger Entries (Double-Entry)
// 5. Ghi Outbox Event (không publish Kafka trực tiếp trong @Transactional)
```

### 4.2 Money Handling
```java
// ❌ KHÔNG để BigDecimal rải rắc
BigDecimal amount = request.getAmount();
account.setBalance(account.getBalance().subtract(amount));

// ✅ Dùng Money Value Object
Money amount = Money.of(request.getAmount(), Currency.VND);
account.withdraw(amount); // Domain validates and updates
```

### 4.3 Kafka Events (Outbox Pattern Mandatory)
```java
// ❌ KHÔNG publish Kafka trực tiếp trong @Transactional
@Transactional
public void execute() {
    transferRepository.save(transfer);
    kafkaTemplate.send("transfer.completed", event); // NGUY HIỂM!
}

// ✅ Dùng Outbox Pattern
@Transactional
public void execute() {
    transferRepository.save(transfer);
    outboxRepository.save(OutboxEvent.of("transfer.completed", transferId, payload));
    // Commit, sau đó OutboxPoller publish Kafka async
}
```

---

## 5. Quy Trình Làm Việc AI (Bắt Buộc)

**Bước 1:** Đọc `banking/docs/01_Architecture_Bible.md`
**Bước 2:** Đọc `banking/docs/02_Coding_Guideline.md`
**Bước 3:** Xác định Sprint hiện tại trong `banking/docs/04_Sprint_Plan.md`
**Bước 4:** Code với Javadoc tiếng Việt đầy đủ
**Bước 5:** Security check (sensitive data log? ownership check? concurrency?)
**Bước 6:** Test: Unit + Integration với banking scenarios

---

## 6. Stack Được Phép Dùng

### Backend (KHÔNG tự ý thêm dependency ngoài danh sách này)
- Java 21 LTS + Spring Boot 3.3+
- Spring Security 6 (JWT)
- Spring Data JPA + Hibernate 6
- Spring Cloud Gateway
- Spring Kafka
- Spring Data Redis (Lettuce)
- PostgreSQL 16 (driver)
- Flyway (migration)
- Resilience4j (circuit breaker, retry, rate limiter)
- OpenTelemetry (tracing)
- Micrometer + Prometheus (metrics)
- Redisson (distributed lock)
- Lombok
- MapStruct
- JUnit 5 + Mockito + Testcontainers

### Frontend (Angular)
- Angular 22 (Standalone Components, Signals)
- NgRx (global state)
- Angular Material
- Ionic Framework + Capacitor (mobile)
- Chart.js / ApexCharts
- RxJS
- SCSS

---

## 7. Quy Ước Đặt Tên File

```
# Java
TransferApplicationService.java       # Application Service
BankTransfer.java                      # Domain Entity (prefix Bank để tránh conflict)
Money.java                             # Value Object
TransferRepository.java                # Port (interface)
TransferRepositoryAdapter.java         # Adapter (implementation)
TransferController.java                # Presentation
CreateTransferRequest.java             # Request DTO
TransferResponse.java                  # Response DTO
TransferJpaEntity.java                 # JPA Entity (suffix JpaEntity)
TransferMapper.java                    # Mapper

# Flyway
V1__init_auth_customer.sql
V2__create_accounts.sql

# Angular
transfer-form.page.ts                  # Page component
transfer-form.page.html
transfer-form.page.scss
money-display.component.ts             # Shared component
currency-vnd.pipe.ts                   # Pipe
transfer-api.service.ts                # API service

# Kafka Topics (kebab-case)
transfer-completed
transfer-failed
payment-completed
fraud-alert-created
notification-requested
```
