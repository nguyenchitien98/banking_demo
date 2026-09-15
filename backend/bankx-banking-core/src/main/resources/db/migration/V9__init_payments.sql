-- Flyway Migration V9: Payment Module Schema & Provider Seed Data
-- Bảng payment_providers lưu danh sách nhà cung cấp dịch vụ và bill_payments lưu vết thanh toán hóa đơn

CREATE TABLE IF NOT EXISTS payment_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    logo_url VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bill_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_code VARCHAR(64) UNIQUE NOT NULL,
    source_account_id UUID NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    customer_bill_code VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    fee NUMERIC(19,4) NOT NULL DEFAULT 0,
    period VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bill_payments_account ON bill_payments(source_account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_providers_category ON payment_providers(category, status);

-- Seed Data cho các nhà cung cấp dịch vụ phổ biến (Sprint 13)
INSERT INTO payment_providers (code, name, category, status) VALUES
('EVN_HN', 'Điện lực Hà Nội (EVN Hà Nội)', 'ELECTRICITY', 'ACTIVE'),
('WATER_HCM', 'Nước sinh hoạt DNP Hồ Chí Minh', 'WATER', 'ACTIVE'),
('VIETTEL_TEL', 'Viettel Telecom (Cước Internet/Điện thoại)', 'TELECOM', 'ACTIVE'),
('MOCK_PROVIDER', 'Nhà cung cấp Thử nghiệm BankX', 'OTHER', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;
