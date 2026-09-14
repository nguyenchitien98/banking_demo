# BankX — Progress Tracker (Task.md)

> Cập nhật file này sau mỗi task hoàn thành. Format: `[x]` done, `[/]` in progress, `[ ]` todo.

---

## 📦 Sprint 00 — Infrastructure Foundation

- `[ ]` Tạo cấu trúc thư mục monorepo banking/
- `[ ]` Docker Compose với PostgreSQL + Redis + Kafka + Kafka UI
- `[ ]` Docker Compose với Prometheus + Grafana + Jaeger
- `[ ]` Maven multi-module `pom.xml` (bankx-gateway, bankx-core, bankx-common)
- `[ ]` Angular 22 project với Standalone Components
- `[ ]` Đọc và review tất cả docs trong banking/docs/

---

## 🔐 Sprint 01 — API Gateway

- `[ ]` Spring Cloud Gateway setup
- `[ ]` Route config: auth, accounts, transfers, payments
- `[ ]` Correlation ID filter (X-Trace-Id)
- `[ ]` CORS config
- `[ ]` `ApiResponse<T>` và `ApiErrorResponse` records
- `[ ]` `GlobalExceptionHandler` với @RestControllerAdvice
- `[ ]` Health check endpoint
- `[ ]` Flyway V1 migration file

---

## 🔐 Sprint 02 — Auth Module (JWT)

- `[ ]` Domain: User, UserRole, RefreshToken
- `[ ]` Flyway V2: users, user_roles tables
- `[ ]` JWT generation (Access 15m + Refresh 7d)
- `[ ]` Refresh Token lưu Redis
- `[ ]` Refresh Token Rotation
- `[ ]` Rate Limit login 5/minute/IP
- `[ ]` Account Lock sau 5 fails
- `[ ]` API: /auth/login, /auth/refresh, /auth/logout
- `[ ]` Audit log: LOGIN_SUCCESS, LOGIN_FAILED
- `[ ]` Angular: Login screen
- `[ ]` Angular: Auth Interceptor
- `[ ]` Test: Login success, wrong password, account locked

---

## 📱 Sprint 03 — OTP Module

- `[ ]` OTP Service: Generate, Store (Redis TTL 120s), Verify
- `[ ]` Attempt counter: max 3, block 5 phút
- `[ ]` Mock SMS Sender
- `[ ]` API: /auth/otp/send, /auth/otp/verify
- `[ ]` Angular: OTP Screen với countdown timer
- `[ ]` Test: Valid, expired, wrong, max attempts

---

## 👤 Sprint 04 — Customer Module

- `[ ]` Domain: Customer aggregate, Value Objects
- `[ ]` Flyway V3: customers table
- `[ ]` JPA Auditing (@EntityListeners)
- `[ ]` API: GET /customers/me, PUT /customers/me/profile
- `[ ]` Angular: Profile screen
- `[ ]` Test: Profile update, unauthorized access

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
Phase 0 (Sprint 00):          0/7   tasks  [ 0%]
Phase 1 (Sprint 01-05):       0/35  tasks  [ 0%]
Phase 2 (Sprint 06-12):       0/49  tasks  [ 0%]
Phase 3+ (Sprint 13-30):      Not broken down yet

OVERALL: 0/91+ tasks completed
```

---

## 📝 Ghi Chú / Blockers

> Thêm ghi chú, vấn đề gặp phải, quyết định đột xuất vào đây.

- [2026-09-14] Khởi tạo project - Đang setup docs và planning
