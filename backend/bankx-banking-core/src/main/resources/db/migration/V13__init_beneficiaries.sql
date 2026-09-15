-- V13__init_beneficiaries.sql: Flyway migration for Beneficiary Management Module

CREATE TABLE IF NOT EXISTS beneficiaries (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    bank_bin VARCHAR(16) NOT NULL DEFAULT '970400',
    bank_name VARCHAR(128) NOT NULL DEFAULT 'BankX Digital Bank',
    account_holder_name VARCHAR(128) NOT NULL,
    nickname VARCHAR(128),
    transfer_count INT NOT NULL DEFAULT 1,
    last_transfer_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_account_bin UNIQUE (customer_id, account_number, bank_bin)
);

CREATE INDEX IF NOT EXISTS idx_beneficiaries_customer ON beneficiaries(customer_id);
CREATE INDEX IF NOT EXISTS idx_beneficiaries_count ON beneficiaries(transfer_count DESC);

-- Seed sample frequent beneficiaries for customer CUST-001
INSERT INTO beneficiaries (id, customer_id, account_number, bank_bin, bank_name, account_holder_name, nickname, transfer_count, last_transfer_at, created_at)
VALUES
('BEN-001', 'CUST-001', '1088889999', '970423', 'TPBank', 'NGUYEN CHITIEN', 'Anh Nam Công Ty (Sếp)', 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-002', 'CUST-001', '9988776655', '970436', 'Vietcombank', 'TRAN THI MAI', 'Mẹ ở Quê', 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BEN-003', 'CUST-001', '1000998877', '970400', 'BankX Digital Bank', 'LE HOANG PHUC', 'Bạn Thân Đại Học', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
