# BankX Digital Banking Platform — Lộ Trình 30 Sprint (Sprint Plan)

Tài liệu này là chỉ mục lộ trình 30 Sprint của dự án BankX. Mỗi Sprint được thiết kế để học được một hoặc nhiều kỹ thuật banking-grade quan trọng.

> 📌 **Nguyên tắc:** Kết thúc mỗi Sprint → Hệ thống phải compile, pass tests, chạy được qua Docker Compose.

---

## 🗺️ Tổng Quan Roadmap

| Phase | Sprint | Chủ Đề | Kỹ Thuật Học Được |
|---|---|---|---|
| **Phase 0** | Sprint 00 | Foundation & Infrastructure | Docker, Kafka, Redis, Postgres setup |
| **Phase 1** | Sprint 01-05 | Auth & Account Core | JWT, OTP, Rate Limit, JPA, Flyway |
| **Phase 2** | Sprint 06-12 | Transfer Core (Banking Heart) | Ledger, Idempotency, Optimistic Lock, Outbox, Saga |
| **Phase 3** | Sprint 13-18 | Payment & Card | Strategy Pattern, QR, Fraud Detection |
| **Phase 4** | Sprint 19-24 | Distributed & Observability | CQRS, Prometheus, Jaeger, Circuit Breaker |
| **Phase 5** | Sprint 25-30 | Hardening & Production | Security Audit, CI/CD, K8s, Load Test, Chaos Engineering |

---

## 🏗️ Phase 0: Foundation (Sprint 00)

**Trạng thái:** `[x]` Đã hoàn thành

### Sprint 00: Infrastructure Setup
**Mục tiêu:** Dựng toàn bộ hạ tầng local, khởi tạo cấu trúc dự án.

**Checklist:**
- `[x]` Tạo thư mục `banking/` monorepo structure
  ```
  banking/
  ├── backend/            # Spring Boot multi-module Maven
  ├── frontend-web/       # Angular 22 Customer & Admin Web Portal (Responsive)
  ├── infrastructure/     # Docker, K8s configs
  └── docs/               # Architecture docs
  ```
- `[x]` `docker-compose.yml` khởi chạy:
  - PostgreSQL 16 (port 5432)
  - Redis 7 (port 6379)
  - Apache Kafka + Zookeeper/KRaft (port 9092)
  - Kafka UI (port 8080)
  - Prometheus (port 9090)
  - Grafana (port 3000)
  - Jaeger (port 16686)
- `[x]` Maven multi-module `pom.xml` với modules:
  - `bankx-api-gateway`
  - `bankx-banking-core` (chứa các banking core modules)
  - `bankx-common` (shared library)
- `[x]` Khởi tạo Angular project với Standalone Components + SCSS (frontend-web compile sạch)
- `[x]` Đọc và nắm vững tất cả docs trong `banking/docs/`

**Kết quả bàn giao:** `docker-compose up` → Tất cả services UP. Frontend compile sạch 100%.

---

## 💡 Phase 1: Auth & Account Core (Sprint 01–05)

### Sprint 01: API Gateway & Project Structure
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** Spring Cloud Gateway, Route configuration, Filter Chain

**Checklist:**
- `[x]` API Gateway với Spring Cloud Gateway (`bankx-api-gateway`, port 8080)
- `[x]` Routes: `/api/v1/auth/**`, `/api/v1/customers/**`, `/api/v1/accounts/**`, `/api/v1/transfers/**`
- `[x]` Global filter: Request logging, Correlation ID (`X-Trace-Id`)
- `[x]` CORS configuration (cho phép Angular dev server `http://localhost:4200`)
- `[x]` Health check endpoint: `/api/v1/health`
- `[x]` Clean Architecture package structure cho banking-core module
- `[x]` `ApiResponse<T>` và `ApiErrorResponse` wrapper records trong `bankx-common`
- `[x]` `GlobalExceptionHandler` với `@RestControllerAdvice` trong `bankx-common`
- `[x]` Flyway V1 migration: Tạo bảng cơ sở (`V1__init_schema.sql`)

**Kết quả bàn giao:** Gateway nhận request, forward đúng service, compile 100% clean.

---

### Sprint 02: Auth Module — JWT & Refresh Token
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** Spring Security 6, JWT, Refresh Token Rotation, BCrypt

**Checklist:**
- `[x]` Domain model: `User`, `UserRole`, `RefreshToken`
- `[x]` Flyway V2: `refresh_tokens`, `auth_audit_logs` tables
- `[x]` `AuthApplicationService`: `login()`, `logout()`, `refreshToken()`
- `[x]` JWT generation (Access Token 15 phút, Secret Signature)
- `[x]` Refresh Token lưu CSDL & Redis (TTL 7 ngày, rotation on each refresh)
- `[x]` Spring Security 6 filter chain (`JwtAuthenticationFilter`, `SecurityConfig`)
- `[x]` API: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- `[x]` Account Lock sau 5 lần fail (tự động khóa 30 phút)
- `[x]` Audit log: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT` events
- `[x]` Angular: Login screen phong cách TPBank Purple Theme (tách `.ts`, `.html`, `.scss`), TokenService, Signal State & Auth Interceptor

**Kỹ thuật phỏng vấn cần giải thích được:**
- Refresh Token Rotation là gì? Tại sao cần?
- Tại sao dùng Redis để lưu Refresh Token thay vì DB?
- Nếu Refresh Token bị đánh cắp → xử lý thế nào?

---

### Sprint 03: OTP Module
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** Redis TTL, OTP security, Spring Events

**Checklist:**
- `[x]` OTP Service: Generate (6 chữ số random an toàn), Store (Redis TTL 120s), Verify
- `[x]` Redis key pattern: `otp:{purpose}:{phone}` (LOGIN, TRANSFER, PROFILE_UPDATE)
- `[x]` Attempt counter: `otp_attempts:{purpose}:{phone}` (max 3, block 5 phút)
- `[x]` Mock SMS sender (an toàn thông tin, mask số điện thoại trong log)
- `[x]` API: `POST /api/v1/auth/otp/send`, `POST /api/v1/auth/otp/verify`
- `[x]` Angular: OTP screen giao diện TPBank với 6-digit input auto focus, countdown timer 120s (tách `.ts`, `.html`, `.scss`)
- `[x]` Unit Tests & Build verification: Valid OTP, expired OTP, wrong OTP, max attempts exceeded (Compile 100% SUCCESS)

---

### Sprint 04: Customer Module & KYC Basic
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** DDD Domain Model, Repository Pattern, JPA Auditing

**Checklist:**
- `[x]` Domain: `Customer` aggregate, `cifNumber`, `identityNumber`, `CustomerStatus`
- `[x]` Flyway V3: `kyc_documents` table
- `[x]` APIs: `GET /api/v1/customers/me`, `PUT /api/v1/customers/me/profile`
- `[x]` `@EntityListeners(AuditingEntityListener.class)`: Auto-set `createdAt`, `updatedAt`
- `[x]` Angular: Profile screen hiển thị thông tin CIF, eKYC status và form cập nhật hồ sơ cá nhân (tách `.ts`, `.html`, `.scss`)
- `[x]` Security: Che giấu thông tin nhạy cảm (`MaskingUtils`), phân quyền tài khoản (Compile 100% SUCCESS)

---

### Sprint 05: Account Module — Core & Main Dashboard Layout
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** JPA Optimistic Locking (`@Version`), Money Value Object, Balance Redis Cache 30s, TPBank Dashboard UI

**Checklist:**
- `[x]` Domain: `BankAccount` aggregate root, `Money` Value Object (BigDecimal scale 4, RoundingMode.HALF_UP)
- `[x]` Flyway V4: `transfer_limits` table & `bank_accounts` extensions với cột `version` (Optimistic Lock)
- `[x]` Account Number generation: `BankAccountFactory` tự động sinh 10-12 số ngẫu nhiên an toàn
- `[x]` APIs:
  - `GET /api/v1/accounts` — List accounts của user hiện tại
  - `POST /api/v1/accounts` — Mở tài khoản mới
  - `PATCH /api/v1/accounts/{id}/freeze` — Phong tỏa tài khoản
- `[x]` Redis: Cache balance với TTL 30s (`account_balance:{accountNumber}`)
- `[x]` Angular: AccountService & MainLayoutComponent với TPBank Header & Sidebar (tách `.ts`, `.html`, `.scss`)
- `[x]` Angular: DashboardPage hiển thị số dư, ẩn/hiện số dư, sao chép STK, mở tài khoản mới, thao tác nhanh & Panel kiểm thử thủ công
- `[x]` Test: Open account, freeze account, balance cache, optimistic lock (Maven & Angular Build 100% SUCCESS)

---

## 🏦 Phase 2: Transfer Core — Banking Heart (Sprint 06–12)

### Sprint 06: Ledger Module — Double-Entry Bookkeeping
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** Double-Entry Bookkeeping, Ledger pattern, Financial immutability

**Checklist:**
- `[x]` Domain: `LedgerEntry` (DEBIT/CREDIT), `Transaction` aggregate
- `[x]` Flyway V5: `transactions`, `ledger_entries` tables (Bất biến IMMUTABLE — không có soft delete/update)
- `[x]` Rule: Mỗi transaction phải có ít nhất 1 DEBIT và 1 CREDIT entry
- `[x]` Rule: SUM(DEBIT) == SUM(CREDIT) trong cùng transaction — validation invariant
- `[x]` `LedgerApplicationService.recordDoubleEntry(debitAccount, creditAccount, amount)`
- `[x]` APIs:
  - `GET /api/v1/accounts/{id}/transactions?limit=20`
  - `GET /api/v1/transactions/{id}` — Chi tiết giao dịch + ledger entries
  - `POST /api/v1/ledger/record` — Thử nghiệm hạch toán sổ kép
- `[x]` Angular: `LedgerService`, `AccountsPage` hiển thị sổ nhật ký bút toán, Modal bằng chứng cân bằng sổ sách & Panel kiểm thử thủ công (tách `.ts`, `.html`, `.scss`)
- `[x]` Test: Verify SUM(DEBIT) == SUM(CREDIT), immutability (Maven & Angular Build 100% SUCCESS)

**Kỹ thuật phỏng vấn cần giải thích được:**
- Double-Entry Bookkeeping là gì? Tại sao banking dùng?
- Tại sao ledger_entries không có cột `updated_at` hay soft delete?

---

### Sprint 07: Transfer Module — Core Flow
**Trạng thái:** `[x]` Đã hoàn thành

**Kỹ thuật học:** Saga stub, @Transactional, Business validation chain, Transfer Limit Checks

**Checklist:**
- `[x]` Domain: `BankTransfer` aggregate, `TransferStatus`, `TransferType` (INTERNAL, NAPAS247)
- `[x]` Flyway V6: `bank_transfers` table
- `[x]` `TransferApplicationService` implementation
- `[x]` Business validation chain: Balance check → Account status → Daily limit → Amount limit
- `[x]` `@Transactional`: Debit → Credit → Ledger entries (atomic)
- `[x]` Transfer limits: Max 50M/transaction, Max 500M/day (lưu trữ trong `transfer_limits`)
- `[x]` APIs:
  - `GET /api/v1/transfers/recipient-inquiry` — Truy vấn tên người thụ hưởng
  - `POST /api/v1/transfers/internal` — Tạo lệnh chuyển tiền nội bộ
  - `GET /api/v1/transfers/{id}` — Transfer detail
  - `GET /api/v1/transfers/accounts/{accountId}` — Transfer history
- `[x]` Angular: `TransferService`, `TransfersPage` với form 4 bước (Nhập thông tin, Truy vấn tên, Modal xác nhận, Biên lai chuyển tiền thành công)
- `[x]` Test: Happy path, insufficient balance, account frozen, amount over limit (Maven & Angular Build 100% SUCCESS)

---

### Sprint 08: Idempotency — Chống Transfer Trùng Lặp
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Idempotency Key, Redis SETNX, AOP @Idempotent

**Checklist:**
- `[ ]` `@Idempotent` custom annotation + AOP Aspect
- `[ ]` Redis: `idempotency:{uuid}` → SETNX với TTL 10 phút
- `[ ]` Response caching: Sau khi success, lưu response vào Redis → Lần 2 trả về cached response
- `[ ]` Angular: Auto-generate UUID v4 trước khi submit transfer
- `[ ]` API header: `Idempotency-Key: {uuid}`
- `[ ]` Test: Gửi cùng request 3 lần → chỉ 1 transfer được tạo, response giống nhau

**Kỹ thuật phỏng vấn:** "User bấm transfer 2 lần (double click) → xử lý thế nào?"

---

### Sprint 09: Concurrency Control — Optimistic Lock
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Optimistic Locking, @Version, Retry on conflict

**Checklist:**
- `[ ]` Kiểm tra race condition thực tế bằng JMeter/k6 (2 request đồng thời)
- `[ ]` `@Version` trên `BankAccount` entity
- `[ ]` Retry 3 lần khi `OptimisticLockingFailureException`
- `[ ]` Test concurrent: Thread A và B cùng transfer vượt balance → chỉ 1 thành công
- `[ ]` So sánh Pessimistic vs Optimistic Lock:
  - Pessimistic: `SELECT FOR UPDATE` → Lock DB row → Safe nhưng slow
  - Optimistic: `UPDATE WHERE version=?` → Fast nhưng cần retry

**Kỹ thuật phỏng vấn:** "Hai người cùng rút tiền 1 tài khoản → xử lý thế nào?"

---

### Sprint 10: Transfer OTP Integration
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Step-based flow, OTP trong banking context, Risk-based OTP

**Checklist:**
- `[ ]` Transfer flow thêm OTP bước trước khi execute
- `[ ]` Risk-based: Transfer < 5M → No OTP; Transfer >= 5M → OTP required
- `[ ]` `TransferState`: `PENDING_OTP` → `PROCESSING` → `COMPLETED`/`FAILED`
- `[ ]` Redis: Lưu pending transfer state (TTL 5 phút để user nhập OTP)
- `[ ]` APIs: `POST /api/transfers` → Response 202 (pending OTP); `POST /api/transfers/{id}/confirm-otp`
- `[ ]` Angular: Kết nối OTP screen vào Transfer flow

---

### Sprint 11: Outbox Pattern — Reliable Messaging
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Transactional Outbox, Debezium/Scheduled Poller, At-Least-Once delivery

**Checklist:**
- `[ ]` Flyway V7: `outbox_events` table
- `[ ]` `OutboxEvent` entity (type, aggregateId, payload JSON, status, retryCount)
- `[ ]` Trong `@Transactional` Transfer: Save outbox event cùng transaction
- `[ ]` `OutboxPollingService`: `@Scheduled(fixedDelay=1000)` → Read PENDING → Publish Kafka → Mark SENT
- `[ ]` Retry: `retryCount` max 5, sau đó → FAILED (alert admin)
- `[ ]` Kafka topics: `transfer.completed`, `transfer.failed`
- `[ ]` Test: Simulate Kafka down → Transfer vẫn thành công → Outbox ghi lại → Kafka lên → Events được publish

**Kỹ thuật phỏng vấn:** "DB commit OK nhưng Kafka publish fail → xử lý thế nào?"

---

### Sprint 12: Notification Module — Event-Driven
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Kafka Consumer, Idempotent Consumer, DLQ

**Checklist:**
- `[ ]` Kafka Consumer: Consume `transfer.completed` → Send push notification + email
- `[ ]` Idempotent Consumer: Check processed event by `eventId` in Redis (TTL 1 hour)
- `[ ]` Dead Letter Queue: Sau 3 retry fail → DLQ topic `notification.dlq`
- `[ ]` Mock Email Sender (log to console)
- `[ ]` Mock Push Notification (Firebase Cloud Messaging mock)
- `[ ]` In-app notification: Lưu vào DB, hiển thị trong Notification Center
- `[ ]` APIs: `GET /api/notifications?page=0&size=20`, `PATCH /api/notifications/{id}/read`
- `[ ]` Angular: Notification center screen, badge unread count

---

## 💳 Phase 3: Payment & Advanced Features (Sprint 13–18)

### Sprint 13: Payment Module — Bill Payment
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Strategy Pattern, Provider abstraction, Payment flow

**Checklist:**
- `[ ]` Domain: `BillPayment` aggregate, `PaymentProvider` interface (Strategy Pattern)
- `[ ]` Flyway V8: `bill_payments`, `payment_providers` tables
- `[ ]` Implement providers: `EVNProvider`, `WaterProvider`, `ViettelProvider`, `MockProvider`
- `[ ]` `PaymentProviderFactory` dùng Map injection của Spring (`@PostConstruct` build map)
- `[ ]` APIs:
  - `GET /api/payments/providers` — Danh sách nhà cung cấp
  - `GET /api/payments/bills?providerCode=EVN_HN&billNumber=xxx` — Tra cứu hóa đơn
  - `POST /api/payments/bills` — Thanh toán hóa đơn (có Idempotency-Key)
  - `GET /api/payments/history` — Lịch sử thanh toán
- `[ ]` Ledger: Ghi Double-Entry cho mỗi payment
- `[ ]` Outbox: Publish `payment.completed` event
- `[ ]` Angular: Màn hình danh mục dịch vụ, nhập mã KH, xác nhận, kết quả
- `[ ]` Test: Happy path từng provider, invalid bill number, insufficient balance

**Kỹ thuật phỏng vấn:** "Thêm provider mới (ví dụ MoMo) thì phải sửa code ở đâu?"

---

### Sprint 14: QR Payment Module
**Trạng thái:** `[ ]`

**Kỹ thuật học:** VietQR standard, QR code parsing, Capacitor Camera Plugin

**Checklist:**
- `[ ]` VietQR parser: Decode QR string → Extract `bankCode`, `accountNumber`, `amount`, `description`
- `[ ]` QR Generator: Tạo QR từ `accountNumber + amount + description` (theo chuẩn VietQR EMV)
- `[ ]` API:
  - `POST /api/payments/qr/parse` — Parse QR string → Return transfer info
  - `GET /api/accounts/{id}/qr` — Generate QR nhận tiền
- `[ ]` Flow: Quét QR → Auto-fill Transfer Form → Confirm → OTP → Complete
- `[ ]` Web: Upload ảnh QR (sử dụng `@zxing/browser` để decode)
- `[ ]` Mobile (Ionic): Capacitor Camera Plugin → Scan realtime
- `[ ]` Angular: Màn hình Quét QR, animation scanning, tạo QR với logo BankX
- `[ ]` Test: Valid VietQR, invalid QR, QR không có số tiền (manual input)

---

### Sprint 15: Card Module
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Tokenization, Virtual Card, Card lifecycle State Machine

**Checklist:**
- `[ ]` Domain: `BankCard` aggregate, `CardStatus` FSM (ACTIVE → FROZEN → BLOCKED → EXPIRED)
- `[ ]` Flyway V9: `bank_cards` table (KHÔNG lưu CVV thật, dùng `pan_token`)
- `[ ]` Card Number Tokenization: Masked number hiển thị (`**** **** **** 9988`), PAN token lưu DB
- `[ ]` Virtual Card Generator: Random 16-digit PAN, CVV, expiry (Mock)
- `[ ]` APIs:
  - `GET /api/cards` — Danh sách thẻ của user
  - `GET /api/cards/{id}` — Chi tiết thẻ
  - `POST /api/cards/virtual` — Tạo virtual card (set spending limit)
  - `PATCH /api/cards/{id}/freeze` — Khóa thẻ tạm thời (không cần OTP)
  - `PATCH /api/cards/{id}/unfreeze` — Mở khóa thẻ
  - `PATCH /api/cards/{id}/limits` — Cập nhật hạn mức online
- `[ ]` Audit log: CARD_FROZEN, CARD_UNFROZEN, VIRTUAL_CARD_CREATED
- `[ ]` Angular: Màn hình danh sách thẻ, chi tiết thẻ, toggle freeze, tạo virtual card
- `[ ]` Test: Freeze/unfreeze, virtual card generation, card limit update

---

### Sprint 16: Fraud Detection — Rule Engine
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Rule Engine custom, Risk Score calculation, Kafka event integration

**Checklist:**
- `[ ]` Domain: `FraudRule`, `FraudAlert`, `RiskScore` value object
- `[ ]` Flyway V10: `fraud_rules`, `fraud_alerts` tables
- `[ ]` Rule Engine: Evaluate list of rules → Tính tổng risk score (0–100)
- `[ ]` Built-in rules:
  - Rule 1: Amount > 100M → score +40
  - Rule 2: > 5 transactions/1 minute → score +30
  - Rule 3: New device + amount > 50M → score +50
  - Rule 4: Transfer to new beneficiary + amount > 20M → score +20
  - Rule 5: Đêm khuya (0h–4h) + large amount → score +15
- `[ ]` Risk Action: score < 40 → ALLOW; 40–70 → OTP Required; > 70 → BLOCK + Alert
- `[ ]` Transfer flow: Gọi FraudService.evaluate() trước khi execute transfer
- `[ ]` Kafka Consumer: Consume `transfer.completed` → Update fraud model
- `[ ]` API Admin: `GET /api/admin/fraud/alerts`, `PATCH /api/admin/fraud/alerts/{id}/review`
- `[ ]` Angular Admin: Fraud alert dashboard, rule management UI
- `[ ]` Test: Amount threshold rule, velocity rule, new device rule

---

### Sprint 17: Beneficiary Management
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Frequent pattern tracking, Smart suggestions

**Checklist:**
- `[ ]` Domain: `Beneficiary` entity
- `[ ]` Flyway V11: `beneficiaries` table
- `[ ]` Auto-save beneficiary sau mỗi transfer thành công (increment `transfer_count`)
- `[ ]` APIs:
  - `GET /api/beneficiaries` — Danh sách (sort by transfer_count DESC)
  - `POST /api/beneficiaries` — Thêm thủ công
  - `PUT /api/beneficiaries/{id}` — Đổi nickname
  - `DELETE /api/beneficiaries/{id}` — Soft delete
  - `GET /api/accounts/lookup?accountNumber=xxx` — Tra cứu tên tài khoản BankX
- `[ ]` Angular: Màn hình beneficiary list, search, add/edit, xóa
- `[ ]` Angular Transfer: Quick-pick từ beneficiary list
- `[ ]` Test: Auto-save on transfer, sort by frequency, lookup

---

### Sprint 18: Admin Portal — Core
**Trạng thái:** `[ ]`

**Kỹ thuật học:** RBAC, Admin-specific API layer, Real-time dashboard

**Checklist:**
- `[ ]` Spring Security: Roles `ROLE_ADMIN`, `ROLE_TELLER`, `ROLE_AUDITOR`
- `[ ]` `@PreAuthorize` phân quyền chi tiết theo operation
- `[ ]` Admin APIs:
  - `GET /api/admin/customers` — Danh sách KH (paging, filter)
  - `GET /api/admin/customers/{id}` — Chi tiết KH + accounts
  - `PATCH /api/admin/customers/{id}/kyc` — Approve/Reject KYC
  - `GET /api/admin/transactions` — Monitor giao dịch realtime
  - `GET /api/admin/accounts/{id}/statement` — Xem sao kê bất kỳ account
  - `POST /api/admin/accounts/{id}/freeze` — Admin freeze account (cần ghi audit)
- `[ ]` Admin Dashboard KPIs: Tổng GD hôm nay, Tổng giá trị, GD lỗi, Khách hàng mới
- `[ ]` Angular Admin Portal: Sidebar layout, Customer list, Transaction monitor
- `[ ]` Audit: Mọi admin action phải ghi `audit_logs` với admin userId
- `[ ]` Test: RBAC enforcement (TELLER không được approve KYC), Audit trail

---

## 📊 Phase 4: Distributed & Observability (Sprint 19–24)

### Sprint 19: CQRS — Transaction History Read Model
**Trạng thái:** `[ ]`

**Kỹ thuật học:** CQRS pattern, Read Model, Kafka Projection, Index optimization

**Checklist:**
- `[ ]` Tách Write Model (hiện tại) khỏi Read Model cho Transaction History
- `[ ]` Flyway V12: `transaction_history_view` table — Denormalized, index tối ưu
  - Index: `(customer_id, created_at DESC)` → Query nhanh theo customer + time
  - Index: `(account_id, created_at DESC)` → Query theo account
  - Partition by month (PostgreSQL Range Partition) nếu cần
- `[ ]` Kafka Consumer: Consume `transfer.completed`, `payment.completed` → Project vào Read Model
- `[ ]` `TransactionHistoryQueryService`: Đọc từ Read Model, không đụng vào Write tables
- `[ ]` Cursor-based pagination thay offset-based (performance với dataset lớn)
  - Dùng `created_at + id` làm cursor thay vì `OFFSET N`
- `[ ]` API: `GET /api/accounts/{id}/history?cursor=xxx&size=20&type=TRANSFER&from=date`
- `[ ]` Angular: Infinite scroll transaction history (thay thế pagination cũ)
- `[ ]` Test: Query 1M records performance, cursor pagination correctness

**Kỹ thuật phỏng vấn:** "1 tỷ transaction → query lịch sử thế nào?"

---

### Sprint 20: Prometheus & Grafana Dashboard
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Micrometer, Custom metrics, Grafana dashboard design

**Checklist:**
- `[ ]` Micrometer integration: `spring-boot-starter-actuator` + Prometheus registry
- `[ ]` Default metrics: JVM heap, GC pause, HTTP request rate, DB connection pool
- `[ ]` Custom Business Metrics:
  - `bankx_transfer_total{status="COMPLETED"}` — Tổng transfer thành công
  - `bankx_transfer_amount_vnd` — Histogram số tiền transfer
  - `bankx_fraud_alert_total{severity="HIGH"}` — Fraud alerts
  - `bankx_otp_attempts_total` — OTP attempts
  - `bankx_active_sessions_gauge` — Active sessions
- `[ ]` Grafana Dashboard với panels:
  - Transfer Rate (req/s), Error Rate (%), P99 Latency
  - Active Users (gauge), Daily Transfer Volume (VND)
  - Kafka Consumer Lag, Redis Hit Rate
  - JVM Memory Usage, DB Connection Pool
- `[ ]` Alerts: Error rate > 5% → Grafana alert → Notification
- `[ ]` Test: Metrics exposed tại `/actuator/prometheus`, Grafana import dashboard

---

### Sprint 21: Distributed Tracing — Jaeger
**Trạng thái:** `[ ]`

**Kỹ thuật học:** OpenTelemetry, Span, Trace propagation, MDC

**Checklist:**
- `[ ]` OpenTelemetry SDK + Jaeger Exporter dependency
- `[ ]` Auto-instrumentation: HTTP requests, DB queries, Kafka messages
- `[ ]` Trace propagation qua HTTP header `traceparent` (W3C standard)
- `[ ]` Kafka: Inject `traceId` vào Kafka message header → Consumer extract + continue span
- `[ ]` MDC integration: `traceId` và `spanId` tự động được thêm vào mọi log line
- `[ ]` Custom spans cho business operations:
  - `transfer.validate`, `transfer.debit`, `transfer.credit`, `transfer.ledger`
  - `fraud.evaluate`, `otp.verify`
- `[ ]` Jaeger UI: Xem trace end-to-end từ Gateway → Transfer → Ledger → Kafka → Notification
- `[ ]` Test: Gửi 1 transfer → Tìm trace trong Jaeger → Verify span tree đúng

**Kỹ thuật phỏng vấn:** "Transfer thất bại → tìm được ở service nào không?"

---

### Sprint 22: Saga Orchestration — Distributed Transfer
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Saga Orchestrator, State Machine, Compensating Transactions

**Checklist:**
- `[ ]` Kịch bản: Transfer sang service Account độc lập (giả lập Phase 2 microservices)
- `[ ]` `TransferSagaOrchestrator` — Quản lý flow qua State Machine:
  ```
  STARTED → DEBIT_INITIATED → DEBIT_COMPLETED → CREDIT_INITIATED
           → CREDIT_COMPLETED → LEDGER_RECORDED → COMPLETED
  ```
- `[ ]` Compensation steps:
  - CREDIT_FAILED → Reverse Debit (credit lại account nguồn) → FAILED
  - LEDGER_FAILED → Reverse Credit + Reverse Debit → FAILED
- `[ ]` Flyway V13: `saga_instances` table (lưu state, saga_id, correlation_id)
- `[ ]` Saga persistence: Mỗi bước update saga state vào DB → Idempotent recovery nếu restart
- `[ ]` Test: Happy path, credit fails → verify debit reversed, saga timeout handling

**Kỹ thuật phỏng vấn:** "Transfer fail giữa chừng → rollback thế nào khi 2 DB khác nhau?"

---

### Sprint 23: Circuit Breaker & Resilience
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Resilience4j, Circuit Breaker 3 states, Retry, Rate Limiter, Bulkhead

**Checklist:**
- `[ ]` Resilience4j dependency + Spring Boot auto-configuration
- `[ ]` Circuit Breaker cho InterBank Adapter:
  - CLOSED: Bình thường
  - OPEN: Sau 5 failures/10s → Reject ngay, không gọi external
  - HALF-OPEN: Sau 30s, cho 1 request test → Nếu OK → CLOSED
- `[ ]` Retry với exponential backoff + jitter (cho idempotent operations)
- `[ ]` Rate Limiter: Giới hạn calls/second đến external bank API
- `[ ]` Bulkhead: Giới hạn concurrent calls đến slow external service
- `[ ]` Fallback: Circuit open → Return cached response hoặc PENDING status
- `[ ]` Metrics: Circuit state exposed qua `/actuator/circuitbreakers` + Grafana
- `[ ]` Engineering Portal: Nút toggle để simulate circuit breaker open/close
- `[ ]` Test: Simulate external bank timeout → Circuit opens → Fallback → Recovery

---

### Sprint 24: Engineering Portal — System Health Dashboard
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Real-time dashboard, WebSocket, Chaos Engineering

**Checklist:**
- `[ ]` Angular Engineering Portal (route: `/engineering`)
- `[ ]` WebSocket (SockJS + STOMP): Push realtime metrics về Angular
- `[ ]` Panels:
  - **Service Health Grid:** Mỗi module (Auth/Account/Transfer/Payment...) → badge UP/DOWN/DEGRADED
  - **Kafka Lag Monitor:** Consumer group lag per topic, cảnh báo đỏ nếu lag > 1000
  - **Redis Stats:** Hit rate, Memory, Connections
  - **Transfer KPIs:** TPS, Error rate, P99 latency (live chart, update mỗi 2s)
  - **Circuit Breaker States:** Hiển thị trạng thái CLOSED/OPEN/HALF-OPEN
- `[ ]` **Chaos Engineering Buttons** (AGENTS.md rule: phải có Verification Guide Panel):
  - "Delay DB 2s" → Simulate slow database → Kiểm tra Circuit Breaker
  - "Kill Kafka" → Simulate Kafka down → Kiểm tra Outbox Pattern
  - "Flood Transfer" → Gửi 100 requests đồng thời → Kiểm tra rate limit + concurrency
- `[ ]` **Verification Guide Panel** (bắt buộc theo AGENTS.md Sprint 41+ rule):
  - Kịch bản 1 (Happy Path): Steps thực hiện + Expected result trên dashboard
  - Kịch bản 2 (Chaos - Kafka Down): Steps + Expected (Outbox records tăng, lag giảm khi Kafka lên)
  - Kịch bản 3 (Concurrent Transfer): Steps + Expected (1 success, rest optimistic lock retry)
- `[ ]` Test: Nhấn chaos button → Verify metric thay đổi đúng → Verify recovery

---

## 📱 Phase 5: Mobile & Production (Sprint 25–30)

### Sprint 25: Ionic + Capacitor Setup
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Ionic Framework, Capacitor, Cross-platform deployment

**Checklist:**
- `[ ]` Tạo `banking/frontend-mobile/` — Ionic + Angular (reuse code từ `frontend-web`)
- `[ ]` Shared library: Tách models, API services, pipes vào `banking/packages/shared/`
- `[ ]` Capacitor setup: `npx cap init`, add iOS + Android platforms
- `[ ]` Capacitor Plugins cài đặt:
  - `@capacitor/camera` — Cho QR scanner
  - `@capacitor/biometrics` — Face ID / Touch ID
  - `@capacitor/secure-storage` — Lưu token an toàn
  - `@capacitor/push-notifications` — Firebase Push
  - `@capacitor/haptics` — Rung khi transfer success
- `[ ]` Mobile layout: Bottom Navigation, Safe area insets (iPhone notch)
- `[ ]` Build test: `ionic build` + `npx cap sync` chạy được trên Android Emulator
- `[ ]` TPBank mobile UI: Implement theo `docs/07_UI_UX_Standard.md`

---

### Sprint 26: Mobile — Auth + Biometric Login
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Biometric authentication, Secure Storage, Mobile-specific UX

**Checklist:**
- `[ ]` Login screen mobile: Phone + Password, Face ID button, OTP flow
- `[ ]` Biometric Setup flow:
  - Lần đầu login → Hỏi "Bật Face ID?" → Lưu encrypted token vào Secure Storage
  - Các lần sau → Chạm vào Face ID icon → Biometric verify → Load token → Refresh API
- `[ ]` Fallback: Biometric fail 3 lần → Chuyển về PIN/Password
- `[ ]` Secure Storage: KHÔNG dùng localStorage trên mobile — dùng Capacitor Secure Storage
- `[ ]` Deep link: `bankx://auth/otp?session=xxx` — Mở app từ SMS link
- `[ ]` Test trên device thực (Android): Biometric success, biometric fail, fallback

---

### Sprint 27: Mobile — Transfer + QR Scanner
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Camera permission, Real-time QR decode, Mobile transfer UX

**Checklist:**
- `[ ]` Transfer flow mobile: 4 screens (Form → Confirm → OTP → Result) với mobile UX
- `[ ]` Gesture: Swipe down để dismiss OTP screen, pull-to-refresh history
- `[ ]` QR Scanner: Capacitor Camera → Live preview → `@zxing/browser` decode realtime
- `[ ]` Haptic feedback: Rung nhẹ khi QR scan thành công, mạnh hơn khi transfer thành công
- `[ ]` Push Notification: Firebase setup, nhận notification khi có GD mới
  - Click notification → Deep link → Mở màn hình transaction detail
- `[ ]` Background refresh: Khi app từ background về foreground → Auto refresh balance
- `[ ]` Test: QR scan trên device thực, push notification nhận được, haptics

---

### Sprint 28: Load Testing & Performance Tuning
**Trạng thái:** `[ ]`

**Kỹ thuật học:** k6 load testing, Bottleneck analysis, JVM + DB tuning

**Checklist:**
- `[ ]` k6 load test scripts:
  - `test-transfer-normal.js`: 100 VU × 5 phút → Target TPS > 200
  - `test-transfer-concurrent.js`: 50 VU cùng gửi từ 1 account → Verify no race condition
  - `test-balance-query.js`: 1000 VU × cache hit → P99 < 20ms
  - `test-login-otp.js`: 500 VU login flow → Verify rate limit works
- `[ ]` Phân tích bottleneck từ kết quả k6:
  - HikariCP pool size tuning
  - JVM heap size, GC algorithm (ZGC vs G1GC)
  - PostgreSQL `max_connections`, `shared_buffers`
  - Redis connection pool
- `[ ]` Virtual Threads (Java 21): Enable `spring.threads.virtual.enabled=true` → Benchmark
- `[ ]` So sánh before/after tuning: TPS, P99, Error rate
- `[ ]` Document kết quả trong `docs/10_Performance_Report.md`

**Kỹ thuật phỏng vấn:** "Hệ thống đang slow → debug và fix thế nào?"

---

### Sprint 29: CI/CD Pipeline
**Trạng thái:** `[ ]`

**Kỹ thuật học:** GitHub Actions, Docker multi-stage build, Automated quality gates

**Checklist:**
- `[ ]` GitHub Actions workflow `.github/workflows/ci.yml`:
  ```
  Push/PR → Build → Unit Test → Integration Test → Spotless Check
           → Docker Build → Push Registry → Deploy (dev)
  ```
- `[ ]` Maven multi-stage build: Compile → Test → Package (JAR)
- `[ ]` Docker multi-stage `Dockerfile`:
  - Stage 1 (builder): JDK 21 + Maven
  - Stage 2 (runtime): JRE 21 slim → Copy JAR
- `[ ]` Testcontainers trong CI: Dùng real PostgreSQL + Redis + Kafka container
- `[ ]` Angular build: `ng build --configuration production` → Docker Nginx
- `[ ]` Quality gates: Test coverage > 70% (JaCoCo), spotless format check
- `[ ]` Secrets management: GitHub Secrets cho DB password, JWT key
- `[ ]` Test: Push code → Pipeline chạy xanh → Image xuất hiện trong registry

---

### Sprint 30: Production Hardening & Final Documentation
**Trạng thái:** `[ ]`

**Kỹ thuật học:** Security hardening, OpenAPI docs, Interview preparation

**Checklist:**
- `[ ]` **Security Hardening:**
  - HTTPS only (Nginx SSL termination)
  - CORS config: Chỉ allow production domain
  - Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `HSTS`
  - SQL injection scan (OWASP ZAP)
  - Dependency vulnerability scan (`mvn dependency-check`)
  - Remove all `System.out.println`, hardcoded secrets
- `[ ]` **OpenAPI Documentation:**
  - Springdoc OpenAPI: Auto-generate từ Controller annotations
  - Mô tả đầy đủ mỗi endpoint: description, parameters, responses, error codes
  - Swagger UI accessible tại `/api/swagger-ui.html`
- `[ ]` **Final Documentation:**
  - Cập nhật `banking/README.md` với architecture diagram, screenshots
  - `docs/10_Performance_Report.md`: k6 results, tuning decisions
  - `docs/11_Deployment_Guide.md`: Docker Compose → Production guide
- `[ ]` **Interview Preparation:**
  - Review và cập nhật `docs/09_Interview_QA_Banking.md`
  - Tập giải thích transfer flow trên whiteboard (không nhìn code)
  - Tập vẽ sequence diagram Outbox Pattern từ đầu
  - Tập giải thích Optimistic Lock với ví dụ số cụ thể
- `[ ]` Final demo: Chạy full demo từ Login → Transfer → OTP → Notification → History

---

## 📊 Sprint Tracking Dashboard

```
Tổng Sprint: 30 (không kể Sprint 00)
Hoàn thành:   0 / 30  [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0%
Đang làm:     0
Chưa làm:    30
```

> **Cập nhật dashboard sau mỗi Sprint hoàn thành!**
