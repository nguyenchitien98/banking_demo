# BankX — Progress Tracker (Task.md)

> Cập nhật file này sau mỗi task hoàn thành. Format: `[x]` done, `[/]` in progress, `[ ]` todo.

---

## 📦 Sprint 00 — Infrastructure Foundation

- `[x]` Tạo cấu trúc thư mục monorepo banking/
- `[x]` Docker Compose với PostgreSQL + Redis + Kafka + Kafka UI
- `[x]` Docker Compose với Prometheus + Grafana + Jaeger
- `[x]` Maven multi-module `pom.xml` (bankx-api-gateway, bankx-banking-core, bankx-common)
- `[x]` Angular 22 project với Standalone Components (frontend-web)
- `[x]` Đọc và review tất cả docs trong banking/docs/

---

## 🔐 Sprint 01 — API Gateway & Base Framework Setup

- `[x]` Spring Cloud Gateway setup (`bankx-api-gateway`, port 8080)
- `[x]` Route config: auth, customers, accounts, transfers, payments, cards, notifications
- `[x]` Correlation ID filter (`X-Trace-Id` Global Filter)
- `[x]` CORS config (`CorsWebFilter` hỗ trợ Angular port 4200)
- `[x]` `ApiResponse<T>` và `ApiErrorResponse` Java Records trong `bankx-common`
- `[x]` `GlobalExceptionHandler` với `@RestControllerAdvice` trong `bankx-common`
- `[x]` Health check endpoint (`GET /api/v1/health`)
- `[x]` Flyway V1 migration file (`V1__init_schema.sql`: users, user_roles, customers, bank_accounts, outbox_events)

---

## 🔐 Sprint 02 — Auth Module (JWT & Refresh Token)

- `[x]` Domain: User aggregate root, UserRole, RefreshToken
- `[x]` Flyway V2: `refresh_tokens`, `auth_audit_logs` tables
- `[x]` JWT generation (Access Token 15m + Refresh Token 7d, RS256/HS256)
- `[x]` Refresh Token lưu Database / Redis
- `[x]` Refresh Token Rotation (vô hiệu hóa token cũ khi cấp lại)
- `[x]` Rate Limit login & Account Lock (khóa 30 phút khi đăng nhập sai quá 5 lần)
- `[x]` API: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- `[x]` Audit log: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`
- `[x]` Angular: Login screen phong cách TPBank Purple Theme (tách `.ts`, `.html`, `.scss`)
- `[x]` Angular: Auth Interceptor & TokenService (Signal-based state)
- `[x]` Test: Login success, wrong password, account locked, token rotation (Compile 100% SUCCESS)

---

## 📱 Sprint 03 — OTP Module

- `[x]` OTP Service: Generate 6-digit random code, Store (Redis TTL 120s), Verify
- `[x]` Attempt counter: max 3 failed attempts, block 5 phút (300s TTL)
- `[x]` Mock SMS Sender (an toàn thông tin, mask số điện thoại trong log)
- `[x]` API: `POST /api/v1/auth/otp/send`, `POST /api/v1/auth/otp/verify`
- `[x]` Angular: OTP Screen giao diện TPBank với đếm ngược 120s & 6-digit pin code inputs (tách `.ts`, `.html`, `.scss`)
- `[x]` Test: Valid OTP, expired OTP, wrong code, max attempts (Compile 100% SUCCESS)

---

## 👤 Sprint 04 — Customer Module & KYC Basic

- `[x]` Domain: Customer aggregate root, cifNumber, identityNumber, status
- `[x]` Flyway V3: `kyc_documents` table
- `[x]` JPA Auditing (`@EntityListeners(AuditingEntityListener.class)`)
- `[x]` API: `GET /api/v1/customers/me`, `PUT /api/v1/customers/me/profile`
- `[x]` Angular: Profile screen hiển thị thông tin CIF, eKYC status và form cập nhật hồ sơ cá nhân (tách `.ts`, `.html`, `.scss`)
- `[x]` Test: Profile update, unauthorized access, identity masking (Compile 100% SUCCESS)

---

## 💰 Sprint 05 — Account Module

- `[ ]` Domain: BankAccount, Money, AccountId
- `[ ]` Flyway V4: bank_accounts, transfer_limits
- `[ ]` @Version column (Optimistic Lock)
- `[ ]` Account Number generation
- `[ ]` Balance cache Redis (30s TTL)
- `[ ]` API: GET /accounts, POST /accounts, PATCH /accounts/:id/freeze
- `[ ]` Angular: Account list screen
- `[ ]` Angular: Account detail screen
- `[ ]` Test: Open account, freeze/unfreeze, balance cache

---

## 📒 Sprint 06 — Ledger Module

- `[ ]` Domain: LedgerEntry, Transaction aggregate
- `[ ]` Flyway V5: transactions, ledger_entries (IMMUTABLE)
- `[ ]` Double-Entry validation: SUM(DEBIT) == SUM(CREDIT)
- `[ ]` LedgerService.recordDoubleEntry()
- `[ ]` API: GET /accounts/:id/transactions, GET /transactions/:id
- `[ ]` Test: Balance invariant, immutability

---

## 💸 Sprint 07 — Transfer Core

- `[ ]` Domain: BankTransfer, TransferStatus
- `[ ]` Flyway V6: bank_transfers
- `[ ]` CreateTransferUseCase implementation
- `[ ]` Validation chain: balance, status, limits
- `[ ]` @Transactional: Debit → Credit → Ledger
- `[ ]` API: POST /transfers, GET /transfers/:id
- `[ ]` Angular: Transfer form (4 screens)
- `[ ]` Test: Happy path, insufficient balance, frozen account

---

## 🔁 Sprint 08 — Idempotency

- `[ ]` @Idempotent AOP Aspect
- `[ ]` Redis SETNX idempotency:uuid TTL 10m
- `[ ]` Response caching
- `[ ]` Angular: UUID generation trước submit
- `[ ]` Test: 3x same request → 1 transfer created

---

## ⚡ Sprint 09 — Concurrency (Optimistic Lock)

- `[ ]` @Version trên BankAccount
- `[ ]` Retry 3 lần khi OptimisticLockException
- `[ ]` Concurrent transfer test (2 threads)
- `[ ]` Compare Pessimistic vs Optimistic in comments

---

## 📲 Sprint 10 — Transfer + OTP Integration

- `[ ]` Risk-based OTP: < 5M không cần, >= 5M cần OTP
- `[ ]` TransferState machine: PENDING_OTP → PROCESSING → COMPLETED
- `[ ]` Redis: pending transfer state TTL 5m
- `[ ]` API: POST /transfers (202 Accepted), POST /transfers/:id/confirm-otp
- `[ ]` Angular: OTP screen trong transfer flow

---

## 📤 Sprint 11 — Outbox Pattern

- `[ ]` Flyway V7: outbox_events table
- `[ ]` OutboxEvent entity
- `[ ]` Ghi Outbox trong @Transactional Transfer
- `[ ]` OutboxPollingService (Scheduled 1s)
- `[ ]` Retry max 5, sau đó FAILED
- `[ ]` Kafka topics: transfer.completed, transfer.failed
- `[ ]` Test: Kafka down → Transfer success → Outbox records → Kafka up → Publish

---

## 🔔 Sprint 12 — Notification Module

- `[ ]` Kafka Consumer: transfer.completed → Send push + email
- `[ ]` Idempotent Consumer: check eventId Redis
- `[ ]` DLQ: notification.dlq sau 3 retries
- `[ ]` Mock Email + Push sender
- `[ ]` In-app notifications DB
- `[ ]` API: GET /notifications, PATCH /notifications/:id/read
- `[ ]` Angular: Notification center + badge count

---

## 📊 Progress Summary

```
Phase 0 (Sprint 00):          6/6   tasks  [100%]
Phase 1 (Sprint 01-05):       31/35 tasks  [ 88%]
Phase 2 (Sprint 06-12):       0/49  tasks  [  0%]
Phase 3+ (Sprint 13-30):      Not broken down yet

OVERALL: 37/90 tasks completed
```

---

## 📝 Ghi Chú / Blockers

> Thêm ghi chú, vấn đề gặp phải, quyết định đột xuất vào đây.

- [2026-09-14] Khởi tạo project - Đang setup docs và planning
