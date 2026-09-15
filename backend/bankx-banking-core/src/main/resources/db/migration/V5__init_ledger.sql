-- =============================================================================
-- Flyway Migration V5 — Ledger Module & Double-Entry Bookkeeping Tables
-- =============================================================================

-- 1. Bảng lưu trữ giao dịch tổng (transactions)
-- Lưu ý: Không có cột updated_at hay deleted_at vì bản ghi tài chính là IMMUTABLE.
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    transaction_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_ref ON transactions(transaction_reference);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

-- 2. Bảng bút toán ghi sổ kép (ledger_entries)
-- Quy tắc Double-Entry: Mỗi transaction sinh ra ít nhất 1 DEBIT và 1 CREDIT entry.
CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES bank_accounts(id) ON DELETE RESTRICT,
    entry_type VARCHAR(10) NOT NULL, -- 'DEBIT' hoặc 'CREDIT'
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance_after NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_entries_tx ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id, created_at DESC);
