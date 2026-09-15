-- V11__init_cards.sql: Flyway migration for Card Module (Virtual Cards & Tokenization)

CREATE TABLE IF NOT EXISTS bank_cards (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    card_holder_name VARCHAR(128) NOT NULL,
    masked_pan VARCHAR(32) NOT NULL,
    pan_token VARCHAR(128) NOT NULL UNIQUE,
    card_type VARCHAR(32) NOT NULL DEFAULT 'VIRTUAL_DEBIT',
    card_brand VARCHAR(32) NOT NULL DEFAULT 'VISA',
    expiry_month VARCHAR(2) NOT NULL,
    expiry_year VARCHAR(2) NOT NULL,
    spending_limit DECIMAL(19, 4) NOT NULL DEFAULT 50000000.0000,
    daily_limit DECIMAL(19, 4) NOT NULL DEFAULT 100000000.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bank_cards_customer ON bank_cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_bank_cards_account ON bank_cards(account_number);

-- Seed initial sample virtual cards for customer
INSERT INTO bank_cards (id, customer_id, account_number, card_holder_name, masked_pan, pan_token, card_type, card_brand, expiry_month, expiry_year, spending_limit, daily_limit, status, created_at)
VALUES 
('CARD-001', 'CUST-001', '1088889999', 'NGUYEN CHITIEN', '4000 12** **** 8899', 'TOK-CARD-VISA-001-A98B', 'VIRTUAL_DEBIT', 'VISA', '12', '28', 50000000.0000, 100000000.0000, 'ACTIVE', CURRENT_TIMESTAMP),
('CARD-002', 'CUST-001', '1088889999', 'NGUYEN CHITIEN', '5123 45** **** 9911', 'TOK-CARD-MC-002-C34D', 'VIRTUAL_DEBIT', 'MASTERCARD', '08', '29', 20000000.0000, 50000000.0000, 'FROZEN', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
