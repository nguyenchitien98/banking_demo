-- =============================================================================
-- Flyway Migration V6 — Transfer Module & Internal Bank Transfer Tables
-- =============================================================================

-- Bảng lưu trữ lệnh chuyển tiền (bank_transfers)
CREATE TABLE IF NOT EXISTS bank_transfers (
    id UUID PRIMARY KEY,
    transfer_code VARCHAR(64) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL REFERENCES bank_accounts(id) ON DELETE RESTRICT,
    target_account_id UUID NOT NULL REFERENCES bank_accounts(id) ON DELETE RESTRICT,
    target_account_number VARCHAR(32) NOT NULL,
    target_account_name VARCHAR(128) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    fee NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    description VARCHAR(255),
    transfer_type VARCHAR(32) NOT NULL, -- 'INTERNAL', 'NAPAS247'
    status VARCHAR(32) NOT NULL,        -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bank_transfers_source ON bank_transfers(source_account_id, created_at DESC);
CREATE INDEX idx_bank_transfers_target ON bank_transfers(target_account_id, created_at DESC);
CREATE INDEX idx_bank_transfers_code ON bank_transfers(transfer_code);
