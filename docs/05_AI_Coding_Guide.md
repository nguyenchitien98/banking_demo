# BankX Banking Platform — Chỉ Dẫn Code Cho AI (AI Coding Guide)

Tài liệu này định nghĩa quy trình, checklist và prompt mẫu để hướng dẫn AI Agents (Gemini, Claude, Cursor) lập trình cho **BankX Digital Banking Platform**.

---

## 1. Quy Trình 5 Bước Làm Việc (Banking AI Workflow)

```
[B1: Đọc Context] → [B2: Xác định Scope] → [B3: Code] → [B4: Security Review] → [B5: Test]
```

### Bước 1: Đọc Context Bắt Buộc
Trước khi viết BẤT KỲ dòng code nào, AI PHẢI đọc:
1. `banking/docs/01_Architecture_Bible.md` — Để biết module đang ở đâu trong hệ thống
2. `banking/docs/02_Coding_Guideline.md` — Để biết coding standards
3. `banking/docs/04_Sprint_Plan.md` — Để biết Sprint hiện tại cho phép dùng công nghệ gì
4. `banking/docs/05_Database_Schema.md` — Để biết schema hiện tại

### Bước 2: Xác Định Scope Chính Xác
- Input: Request DTO (gì vào?)
- Output: Response DTO (gì ra?)
- DB tables: Nào bị ảnh hưởng?
- Redis keys: Nào sẽ được set/get/delete?
- Kafka topics: Nào sẽ publish/consume?
- Tuyệt đối KHÔNG tự ý import thư viện mới ngoài `pom.xml` hiện tại

### Bước 3: Viết Code
- Tuân theo Clean Architecture layers
- Viết Javadoc tiếng Việt đầy đủ (xem guideline)
- Không viết code giả (TODO, return null, throw new RuntimeException)

### Bước 4: Security Review (Banking-Specific)
Tự check trước khi hoàn thành:
- `[ ]` Có log sensitive data không? (OTP, password, CVV, JWT, full card number)
- `[ ]` Endpoint có cần `@PreAuthorize("hasRole('CUSTOMER')")` không?
- `[ ]` Có kiểm tra ownership không? (User A không được xem tài khoản của User B)
- `[ ]` Số tiền có validation không? (> 0, không null, không vượt limit)
- `[ ]` Có SQL injection risk không? (Phải dùng parameterized query)

### Bước 5: Viết Test
- Unit Test: Domain logic, Use Case, Validator
- Integration Test: `@SpringBootTest` + Testcontainers (real PostgreSQL + Redis)
- Banking scenarios: Happy path + tất cả edge cases đã nêu trong backlog

---

## 2. Mẫu Prompt Chuẩn (Prompt Templates)

### Template 1: Implement Feature Mới

```
Bạn là Senior Java Banking Developer trong dự án BankX Digital Banking Platform.

NHIỆM VỤ: Implement [Tên Feature] theo Sprint [N] trong docs/04_Sprint_Plan.md

QUY TRÌNH BẮT BUỘC:
1. Đọc docs/01_Architecture_Bible.md để nắm module flow và dependency
2. Đọc docs/02_Coding_Guideline.md để biết coding standards và comment chuẩn
3. Đọc docs/05_Database_Schema.md để biết bảng DB hiện tại

YÊU CẦU CODE:
- Module: banking/backend/bankx-banking-core/src/.../[module-name]/
- Layer: Implement đầy đủ từ Domain → Application → Infrastructure → Presentation
- Javadoc: Tiếng Việt cho mọi class public và method public/protected
- Tests: JUnit 5 + Mockito cho Unit Test, Testcontainers cho Integration Test
- Flyway: Tạo Vx__description.sql nếu cần schema mới
- KHÔNG để code giả (TODO, return null)
- KHÔNG log sensitive data (OTP, password, CVV, card number, JWT token)

BANKING REQUIREMENTS:
- Idempotency: [Có/Không cần?]
- Concurrency control: [Optimistic Lock/Pessimistic Lock/None?]
- Outbox Pattern: [Có/Không publish Kafka event?]
- OTP Required: [Có/Không?]
- Audit Log: [Ghi event gì vào audit?]

ANGULAR (nếu cần):
- Feature module: frontend-web/src/app/features/[feature]/
- Sử dụng Angular Signals cho local state
- Pipe currency-vnd.pipe.ts để format số tiền
- Pipe mask-account.pipe.ts để mask số tài khoản
```

### Template 2: Fix Bug Concurrency / Idempotency

```
Hệ thống gặp lỗi liên quan đến [race condition / duplicate request / deadlock] tại [Service Name].

MÔ TẢ LỖI:
[Stacktrace hoặc mô tả hành vi sai]

YÊU CẦU:
1. Root Cause Analysis (RCA): Nguyên nhân gốc rễ là gì?
2. Giải pháp: Không làm phá vỡ Clean Architecture
3. Comment tiếng Việt: Giải thích tại sao lỗi xảy ra và cách phòng ngừa
4. Non-regression test: Viết test tái hiện lại điều kiện gây lỗi

BANKING CONTEXT: Mọi fix đều phải đảm bảo:
- Atomicity: Không để trạng thái nửa vời
- Consistency: Balance không được âm
- Isolation: Concurrent request không ảnh hưởng lẫn nhau
- Durability: Sau commit thì không mất dữ liệu
```

### Template 3: Code Review Banking

```
Review code sau đây theo tiêu chuẩn banking-grade:

[Code cần review]

CHECKLIST REVIEW:
1. Clean Architecture: Dependency đúng hướng không? (Domain ← Infrastructure)
2. Domain Model: Có dùng Value Objects không? (Money, AccountId)
3. Idempotency: Endpoint viết có cần idempotency key không?
4. Concurrency: Có race condition risk không? Nếu có → đề xuất fix
5. Transactions: @Transactional đúng scope không?
6. Security: Có lộ sensitive data không? Có kiểm tra ownership không?
7. Error Handling: Exception có được handle đúng không? HTTP status code đúng không?
8. Logging: Log có an toàn không? (No sensitive data)
9. Tests: Test coverage đủ các banking scenarios chưa?
10. Comment: Javadoc tiếng Việt có đầy đủ không?
```

---

## 3. Banking-Specific Rules Bắt Buộc Cho AI

### 3.1 Tuyệt Đối Cấm

```
❌ CẤM log sensitive data:
  log.info("OTP: {}", otp)                    // NGHIÊM CẤM
  log.info("Password: {}", password)           // NGHIÊM CẤM
  log.info("CVV: {}", cvv)                     // NGHIÊM CẤM
  log.info("Card: {}", fullCardNumber)         // NGHIÊM CẤM
  log.info("Token: {}", jwtToken)              // NGHIÊM CẤM

❌ CẤM trả Entity trực tiếp ra API
❌ CẤM dùng BigDecimal mà không có Money Value Object
❌ CẤM account.balance -= amount mà không có Optimistic Lock
❌ CẤM publish Kafka trong @Transactional (dùng Outbox thay thế)
❌ CẤM để balance âm (validation ở Domain layer)
❌ CẤM hard-code business rules (max transfer amount, daily limit)
❌ CẤM dùng System.currentTimeMillis() — dùng Clock/Instant thay thế (testability)
❌ CẤM catch Exception tổng mà không re-throw hoặc log đầy đủ
❌ CẤM Hibernate ddl-auto = create/update (chỉ validate)
```

### 3.2 Luôn Luôn Phải Làm

```
✅ Mọi endpoint cần @PreAuthorize annotation đúng role
✅ Mọi Transfer/Payment endpoint cần Idempotency-Key header
✅ Mọi sensitive data dùng MaskingUtils trước khi log
✅ Mọi Account operation cần kiểm tra ownership (user chỉ thao tác account của mình)
✅ Mọi số tiền dùng BigDecimal.setScale(0, HALF_UP) cho VND
✅ Flyway migration thay vì Hibernate auto DDL
✅ Outbox Pattern cho mọi DB transaction kèm Kafka publish
✅ Redis cache với TTL hợp lý (không cache vô thời hạn)
✅ Structured logging với MDC (traceId, userId, accountId)
```

---

## 4. Banking Domain Knowledge Cần Nắm

### 4.1 Các Câu Hỏi Phỏng Vấn Banking Thường Gặp

AI cần đảm bảo code trong dự án này **trả lời được** tất cả câu hỏi sau:

| Câu Hỏi | Giải Pháp Trong BankX |
|---|---|
| User bấm chuyển tiền 2 lần → sao không trừ 2 lần? | Idempotency Key (Sprint 08) |
| Hai người cùng rút tiền → sao không âm balance? | Optimistic Lock @Version (Sprint 09) |
| Kafka down khi đang transfer → event có bị mất? | Outbox Pattern (Sprint 11) |
| Consumer chết giữa chừng → có xử lý message 2 lần? | Idempotent Consumer (Sprint 12) |
| Transfer sang service khác fail → rollback thế nào? | Saga + Compensating (Sprint 22) |
| Token bị đánh cắp → logout tất cả session thế nào? | Refresh Token Revocation in Redis |
| 1 tỷ transaction → query history thế nào? | CQRS Read Model + Partition (Sprint 19) |
| Tìm được transaction lỗi ở service nào? | Distributed Tracing Jaeger (Sprint 21) |
| OTP bị brute force → protect thế nào? | Attempt counter Redis + Block |
| Card number bị lộ DB → impact thế nào? | Tokenization, không lưu full PAN |

### 4.2 Ledger Entries — Kiến Thức Bắt Buộc

```
Mọi thay đổi balance PHẢI đi qua ledger. Không được set balance trực tiếp.

Nguyên tắc kế toán kép (Double-Entry):
  - Mỗi transaction có ít nhất 1 DEBIT và 1 CREDIT entry
  - Tổng DEBIT = Tổng CREDIT (điều kiện balance của sổ cái)
  - Ledger entries là IMMUTABLE (không update, không delete)
  - Balance = Initial Balance + SUM(CREDIT entries) - SUM(DEBIT entries)

Ví dụ Transfer 2,000,000 VND từ A sang B:
  Ledger Entry 1: account_A, type=DEBIT,  amount=2,000,000  ← Tài khoản A bị nợ
  Ledger Entry 2: account_B, type=CREDIT, amount=2,000,000  ← Tài khoản B được có
```

---

## 5. Môi Trường Phát Triển

### 5.1 Chạy Local

```bash
# Bước 1: Khởi chạy tất cả infrastructure
docker-compose -f banking/infrastructure/docker-compose.yml up -d

# Bước 2: Chạy backend
cd banking/backend
mvn spring-boot:run -pl bankx-api-gateway -Dspring-boot.run.profiles=local
mvn spring-boot:run -pl bankx-banking-core -Dspring-boot.run.profiles=local

# Bước 3: Chạy Angular
cd banking/frontend-web
ng serve --port 4200

# URLs:
# Angular:      http://localhost:4200
# API Gateway:  http://localhost:8090
# Kafka UI:     http://localhost:8080
# Grafana:      http://localhost:3000
# Jaeger:       http://localhost:16686
# Prometheus:   http://localhost:9090
```

### 5.2 Test Credentials (Dev Only)

```yaml
# admin user (ROLE_ADMIN)
username: admin@bankx.vn
password: Admin@123456

# customer user (ROLE_CUSTOMER)
username: nguyen.van.a@gmail.com
password: Customer@123456
phone: 0987654321

# Seeded accounts (xem Flyway V1 seed data):
# Account 1: 1012345678 — Balance: 145,000,000 VND
# Account 2: 1098765432 — Balance: 50,000,000 VND (tiết kiệm)
```

---

## 6. Scope Code Theo Thư Mục

```
banking/
├── backend/
│   ├── bankx-api-gateway/              # Sprint 01: Spring Cloud Gateway
│   ├── bankx-common/                   # Shared: ApiResponse, Exceptions, MaskingUtils
│   └── bankx-banking-core/
│       └── src/main/java/com/bankx/
│           ├── auth/                   # Sprint 02-03: Auth + OTP
│           ├── customer/               # Sprint 04: Customer + KYC
│           ├── account/                # Sprint 05: Account + Balance
│           ├── ledger/                 # Sprint 06: Double-Entry Ledger
│           ├── transfer/               # Sprint 07-11: Transfer + Idempotency + Outbox
│           ├── notification/           # Sprint 12: Notification Consumer
│           ├── payment/                # Sprint 13-14: Bill + QR
│           ├── card/                   # Sprint 15: Card Management
│           ├── fraud/                  # Sprint 16: Fraud Detection
│           ├── beneficiary/            # Sprint 17: Beneficiary
│           ├── audit/                  # Sprint 11+: Audit Trail
│           └── config/                 # Global configs
│
├── frontend-web/                       # Angular 22 Web App
│   └── src/app/
│       ├── core/                       # Auth, Guards, Interceptors
│       ├── shared/                     # Pipes, Components tái dùng
│       ├── features/                   # Feature modules (lazy loaded)
│       └── layouts/                    # Page layouts
│
├── frontend-mobile/                    # Ionic + Capacitor Mobile App
│
├── infrastructure/
│   ├── docker-compose.yml
│   ├── kafka/                          # Kafka config
│   ├── postgres/                       # Init scripts
│   └── monitoring/                     # Prometheus, Grafana configs
│
└── docs/                               # Tất cả docs (đây!)
    ├── 00_Project_Vision.md
    ├── 01_Architecture_Bible.md
    ├── 02_Coding_Guideline.md
    ├── 03_Backlog.md
    ├── 04_Sprint_Plan.md
    ├── 05_AI_Coding_Guide.md       ← File này
    ├── 06_Database_Schema.md
    ├── 07_UI_UX_Standard.md
    ├── 08_Advanced_Java_Banking.md
    ├── 09_Interview_QA_Banking.md
    └── adr/                           # Architecture Decision Records
        ├── ADR-001-why-outbox-pattern.md
        ├── ADR-002-why-optimistic-lock.md
        └── ADR-003-why-modular-monolith-first.md
```
