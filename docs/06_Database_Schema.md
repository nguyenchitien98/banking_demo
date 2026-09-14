# BankX Banking Platform — Database Schema Reference

Tài liệu này là nguồn sự thật duy nhất về cấu trúc database. Mọi thay đổi schema PHẢI đi qua Flyway migration mới — KHÔNG chỉnh sửa file cũ.

---

## 1. Quy Ước Schema Toàn Cục

```sql
-- Mọi bảng phải có:
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
version         BIGINT NOT NULL DEFAULT 0,  -- Optimistic Lock

-- Bảng financial (transactions, ledger) KHÔNG có:
-- is_deleted, deleted_at → Financial records PHẢI immutable

-- Bảng non-financial (customers, accounts) dùng soft delete:
is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
deleted_at      TIMESTAMP WITH TIME ZONE,
deleted_by      VARCHAR(100)
```

---

## 2. Auth Module Tables

### `users`
```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(15) UNIQUE NOT NULL,       -- SĐT login
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(512) NOT NULL,              -- BCrypt/Argon2
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, LOCKED, SUSPENDED
    failed_login_attempts   INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP WITH TIME ZONE,           -- NULL = không bị lock
    last_login_at   TIMESTAMP WITH TIME ZONE,
    last_login_ip   INET,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_phone ON users(phone);
```

### `user_roles`
```sql
CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id),
    role        VARCHAR(50) NOT NULL,  -- ROLE_CUSTOMER, ROLE_ADMIN, ROLE_TELLER
    PRIMARY KEY (user_id, role)
);
```

### `refresh_tokens` (Backup storage — chính là Redis)
```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    token_hash      VARCHAR(512) NOT NULL UNIQUE,  -- Hash của token (không lưu plaintext)
    device_id       VARCHAR(255),                  -- Fingerprint thiết bị
    device_name     VARCHAR(255),                  -- "iPhone 15 Pro"
    ip_address      INET,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at      TIMESTAMP WITH TIME ZONE,      -- NULL = còn hợp lệ
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

---

## 3. Customer Module Tables

### `customers`
```sql
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id),
    customer_code   VARCHAR(20) UNIQUE NOT NULL,   -- CUS-20240101-0001
    full_name       VARCHAR(255) NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(10),                   -- MALE, FEMALE, OTHER
    national_id     VARCHAR(20),                   -- Số CCCD/CMND (encrypted)
    national_id_encrypted   VARCHAR(512),          -- AES-256 encrypted
    phone           VARCHAR(15) NOT NULL,
    email           VARCHAR(255),
    address         TEXT,
    kyc_status      VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED', -- UNVERIFIED, PENDING, VERIFIED, REJECTED
    kyc_verified_at TIMESTAMP WITH TIME ZONE,
    customer_tier   VARCHAR(20) NOT NULL DEFAULT 'STANDARD',   -- STANDARD, SILVER, GOLD, PLATINUM
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_customers_user_id ON customers(user_id);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_customer_code ON customers(customer_code);
```

---

## 4. Account Module Tables

### `bank_accounts`
```sql
CREATE TABLE bank_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number  VARCHAR(20) UNIQUE NOT NULL,   -- 10 chữ số, unique
    customer_id     UUID NOT NULL REFERENCES customers(id),
    account_type    VARCHAR(20) NOT NULL,           -- PAYMENT, SAVINGS, CREDIT
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance         NUMERIC(18, 2) NOT NULL DEFAULT 0.00,  -- Luôn >= 0 với payment account
    credit_limit    NUMERIC(18, 2),                -- Chỉ dùng cho CREDIT account
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED, SUSPENDED
    frozen_reason   VARCHAR(500),                  -- Lý do khóa (nếu có)
    interest_rate   NUMERIC(5, 4),                 -- Lãi suất (chỉ cho SAVINGS)
    opened_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP WITH TIME ZONE,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE, -- Tài khoản chính
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0      -- QUAN TRỌNG: Optimistic Lock!

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);
CREATE INDEX idx_accounts_customer_id ON bank_accounts(customer_id);
CREATE INDEX idx_accounts_account_number ON bank_accounts(account_number);
CREATE INDEX idx_accounts_status ON bank_accounts(status);
```

---

## 5. Ledger & Transaction Tables

### `transactions`
```sql
-- Giao dịch tài chính — IMMUTABLE (KHÔNG soft delete)
CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_ref     VARCHAR(50) UNIQUE NOT NULL,   -- TXN-20240901-000001
    transaction_type    VARCHAR(30) NOT NULL,           -- TRANSFER, PAYMENT, DEPOSIT, WITHDRAWAL
    status              VARCHAR(20) NOT NULL,           -- PENDING, COMPLETED, FAILED, REVERSED
    total_amount        NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'VND',
    description         VARCHAR(500),
    reference_id        UUID,                           -- ID của transfer/payment (polymorphic)
    reference_type      VARCHAR(50),                    -- 'TRANSFER', 'BILL_PAYMENT'
    initiated_by        UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    failed_reason       VARCHAR(500)
    -- KHÔNG có updated_at, version, is_deleted
);
CREATE INDEX idx_transactions_ref ON transactions(transaction_ref);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_initiated_by ON transactions(initiated_by);
```

### `ledger_entries`
```sql
-- Sổ cái kép — TUYỆT ĐỐI IMMUTABLE
CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    account_id      UUID NOT NULL REFERENCES bank_accounts(id),
    entry_type      VARCHAR(10) NOT NULL,           -- DEBIT hoặc CREDIT
    amount          NUMERIC(18, 2) NOT NULL,        -- Luôn dương
    balance_after   NUMERIC(18, 2) NOT NULL,        -- Số dư sau khi ghi entry
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    description     VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    -- KHÔNG có updated_at, version, is_deleted
);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);
```

---

## 6. Transfer Module Tables

### `bank_transfers`
```sql
CREATE TABLE bank_transfers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     UUID UNIQUE NOT NULL,           -- Client-generated, chống duplicate
    from_account_id     UUID NOT NULL REFERENCES bank_accounts(id),
    to_account_id       UUID REFERENCES bank_accounts(id),      -- NULL nếu liên ngân hàng
    to_account_number   VARCHAR(20) NOT NULL,           -- Số TK đích (lưu string cho liên ngân hàng)
    to_bank_code        VARCHAR(20),                    -- NULL nếu nội bộ, "TCB", "VCB", etc.
    to_account_name     VARCHAR(255),                   -- Tên người nhận (lookup lúc tạo)
    amount              NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'VND',
    fee                 NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    description         VARCHAR(200),
    transfer_type       VARCHAR(20) NOT NULL,           -- INTERNAL, INTERBANK
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING_OTP',
    -- PENDING_OTP → PROCESSING → COMPLETED / FAILED / REVERSED
    otp_required        BOOLEAN NOT NULL DEFAULT FALSE,
    otp_verified_at     TIMESTAMP WITH TIME ZONE,
    transaction_id      UUID REFERENCES transactions(id), -- Liên kết sau khi execute
    executed_at         TIMESTAMP WITH TIME ZONE,
    failed_reason       VARCHAR(500),
    customer_ip         INET,
    device_fingerprint  VARCHAR(512),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_transfers_from_account ON bank_transfers(from_account_id);
CREATE INDEX idx_transfers_idempotency ON bank_transfers(idempotency_key);
CREATE INDEX idx_transfers_status ON bank_transfers(status);
CREATE INDEX idx_transfers_created_at ON bank_transfers(created_at DESC);
```

### `transfer_limits`
```sql
-- Hạn mức giao dịch theo customer tier
CREATE TABLE transfer_limits (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_tier       VARCHAR(20) NOT NULL UNIQUE,    -- STANDARD, SILVER, GOLD, PLATINUM
    max_per_transaction NUMERIC(18, 2) NOT NULL,        -- Max/giao dịch
    max_per_day         NUMERIC(18, 2) NOT NULL,        -- Max/ngày
    max_per_month       NUMERIC(18, 2),
    otp_threshold       NUMERIC(18, 2) NOT NULL DEFAULT 5000000, -- Ngưỡng yêu cầu OTP
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Seed data
INSERT INTO transfer_limits (customer_tier, max_per_transaction, max_per_day, otp_threshold) VALUES
    ('STANDARD', 200000000, 500000000, 5000000),
    ('SILVER',   500000000, 1000000000, 10000000),
    ('GOLD',     1000000000, 2000000000, 20000000),
    ('PLATINUM', 5000000000, 10000000000, 50000000);
```

---

## 7. Outbox Pattern Table

### `outbox_events`
```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100) NOT NULL,          -- 'TRANSFER_COMPLETED', 'PAYMENT_COMPLETED'
    aggregate_id    VARCHAR(100) NOT NULL,           -- ID của Transfer/Payment
    aggregate_type  VARCHAR(50) NOT NULL,            -- 'TRANSFER', 'PAYMENT'
    topic           VARCHAR(255) NOT NULL,           -- Kafka topic name
    payload         JSONB NOT NULL,                  -- Event payload
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count     INTEGER NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
```

---

## 8. Audit Module Table

### `audit_logs`
```sql
-- Nhật ký bất biến — KHÔNG có delete/update
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,                           -- NULL nếu là system action
    customer_id     UUID,
    action          VARCHAR(100) NOT NULL,           -- 'TRANSFER_CREATED', 'LOGIN_SUCCESS', 'CARD_FROZEN'
    resource_type   VARCHAR(50),                    -- 'TRANSFER', 'ACCOUNT', 'CARD'
    resource_id     VARCHAR(100),
    old_value       JSONB,                          -- Trạng thái trước
    new_value       JSONB,                          -- Trạng thái sau
    ip_address      INET,
    user_agent      VARCHAR(500),
    device_id       VARCHAR(255),
    trace_id        VARCHAR(100),                   -- Distributed tracing ID
    severity        VARCHAR(20) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, CRITICAL
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
```

---

## 9. Card Module Tables

### `bank_cards`
```sql
CREATE TABLE bank_cards (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL REFERENCES customers(id),
    account_id          UUID NOT NULL REFERENCES bank_accounts(id),
    card_type           VARCHAR(20) NOT NULL,           -- DEBIT, CREDIT, VIRTUAL
    masked_number       VARCHAR(20) NOT NULL,           -- **** **** **** 9988
    pan_token           VARCHAR(255) UNIQUE NOT NULL,   -- Tokenized PAN (không phải số thẻ thật)
    expiry_month        INTEGER NOT NULL,
    expiry_year         INTEGER NOT NULL,
    cardholder_name     VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, BLOCKED, EXPIRED
    card_network        VARCHAR(20) NOT NULL DEFAULT 'NAPAS',  -- NAPAS, VISA, MASTERCARD
    is_virtual          BOOLEAN NOT NULL DEFAULT FALSE,
    daily_limit         NUMERIC(18, 2),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
    -- KHÔNG lưu CVV thật
);
```

---

## 10. Payment Module Tables

### `bill_payments`
```sql
CREATE TABLE bill_payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     UUID UNIQUE NOT NULL,
    customer_id         UUID NOT NULL REFERENCES customers(id),
    from_account_id     UUID NOT NULL REFERENCES bank_accounts(id),
    provider_code       VARCHAR(50) NOT NULL,           -- 'EVN_HN', 'VIETTEL', 'FPT'
    provider_name       VARCHAR(255) NOT NULL,
    bill_number         VARCHAR(100) NOT NULL,          -- Mã số hóa đơn/khách hàng
    amount              NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'VND',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id      UUID REFERENCES transactions(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### `beneficiaries` (Người thụ hưởng hay dùng)
```sql
CREATE TABLE beneficiaries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL REFERENCES customers(id),
    nickname            VARCHAR(100) NOT NULL,          -- "Ba", "Vợ", "Tiền nhà"
    account_number      VARCHAR(20) NOT NULL,
    bank_code           VARCHAR(20),                    -- NULL = BankX
    bank_name           VARCHAR(255),
    account_name        VARCHAR(255) NOT NULL,
    transfer_count      INTEGER NOT NULL DEFAULT 0,     -- Đếm để sort "thường dùng"
    last_transfer_at    TIMESTAMP WITH TIME ZONE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_beneficiaries_unique ON beneficiaries(customer_id, account_number, bank_code)
    WHERE is_deleted = FALSE;
```

---

## 11. Fraud Detection Tables

### `fraud_alerts`
```sql
CREATE TABLE fraud_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    reference_id    UUID,                           -- Transfer/Payment ID
    reference_type  VARCHAR(50),
    alert_type      VARCHAR(100) NOT NULL,          -- 'LARGE_AMOUNT', 'SUSPICIOUS_DEVICE', 'VELOCITY'
    risk_score      INTEGER NOT NULL,               -- 0-100
    severity        VARCHAR(20) NOT NULL,           -- LOW, MEDIUM, HIGH, CRITICAL
    description     TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, REVIEWED, DISMISSED, ESCALATED
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_fraud_customer ON fraud_alerts(customer_id, created_at DESC);
CREATE INDEX idx_fraud_status ON fraud_alerts(status);
```

---

## 12. Flyway Migration History

| Version | File | Nội Dung | Sprint |
|---|---|---|---|
| V1 | `V1__init_auth_customer.sql` | users, user_roles, customers | Sprint 02-04 |
| V2 | `V2__create_accounts.sql` | bank_accounts, transfer_limits | Sprint 05 |
| V3 | `V3__create_ledger_transactions.sql` | transactions, ledger_entries | Sprint 06 |
| V4 | `V4__create_transfers.sql` | bank_transfers | Sprint 07 |
| V5 | `V5__create_outbox_audit.sql` | outbox_events, audit_logs | Sprint 11 |
| V6 | `V6__create_notifications.sql` | notifications, notification_preferences | Sprint 12 |
| V7 | `V7__create_payment_beneficiary.sql` | bill_payments, beneficiaries | Sprint 13, 17 |
| V8 | `V8__create_cards.sql` | bank_cards | Sprint 15 |
| V9 | `V9__create_fraud.sql` | fraud_alerts, fraud_rules | Sprint 16 |
| V10 | `V10__create_refresh_tokens.sql` | refresh_tokens | Sprint 02 (update) |

> ⚠️ **Quy tắc:** KHÔNG BAO GIỜ sửa file V cũ đã chạy. Chỉ tạo file Vx mới.
