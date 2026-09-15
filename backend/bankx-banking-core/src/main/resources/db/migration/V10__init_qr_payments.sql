-- V10__init_qr_payments.sql: Flyway migration for QR Code Payments module

CREATE TABLE IF NOT EXISTS qr_payments (
    id VARCHAR(64) PRIMARY KEY,
    source_account_id VARCHAR(64) NOT NULL,
    target_account_number VARCHAR(64) NOT NULL,
    target_bank_bin VARCHAR(16) NOT NULL,
    target_account_name VARCHAR(128),
    amount DECIMAL(19, 4) NOT NULL,
    description VARCHAR(255),
    qr_payload TEXT NOT NULL,
    is_dynamic BOOLEAN DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qr_payments_source_acc ON qr_payments(source_account_id);
CREATE INDEX IF NOT EXISTS idx_qr_payments_target_acc ON qr_payments(target_account_number);
