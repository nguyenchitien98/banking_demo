-- V15__init_cqrs_read_model.sql: CQRS Transaction History Read Model Table & Indexes

CREATE TABLE IF NOT EXISTS transaction_history_views (
    id VARCHAR(64) PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    opposite_account_number VARCHAR(64),
    opposite_account_name VARCHAR(128),
    amount NUMERIC(19, 4) NOT NULL,
    direction VARCHAR(16) NOT NULL, -- 'DEBIT' hoặc 'CREDIT'
    transaction_type VARCHAR(32) NOT NULL, -- 'INTERNAL_TRANSFER', 'BILL_PAYMENT', 'QR_PAYMENT'
    category VARCHAR(64),
    description VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index tối ưu cho truy vấn lọc lịch sử theo Khách hàng và Thời gian (Cursor-based Pagination)
CREATE INDEX IF NOT EXISTS idx_tx_history_customer_created ON transaction_history_views(customer_id, created_at DESC);

-- Index tối ưu cho truy vấn lọc lịch sử theo Số tài khoản
CREATE INDEX IF NOT EXISTS idx_tx_history_account_created ON transaction_history_views(account_number, created_at DESC);

-- Index compound hỗ trợ Cursor Pagination bằng (created_at, id)
CREATE INDEX IF NOT EXISTS idx_tx_history_cursor ON transaction_history_views(created_at DESC, id DESC);

-- Seed dữ liệu lịch sử giao dịch mẫu cho Read Model
INSERT INTO transaction_history_views (
    id, transaction_reference, customer_id, account_number, opposite_account_number, 
    opposite_account_name, amount, direction, transaction_type, category, description, status, created_at
) VALUES
('TX-READ-001', 'TX-1001', 'CUST-001', '1000188888', '1000299999', 'TRAN THI MAI', 1500000.0000, 'DEBIT', 'INTERNAL_TRANSFER', 'TRANSFER', 'Chuyển tiền mua quà sinh nhật', 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
('TX-READ-002', 'TX-1002', 'CUST-001', '1000188888', 'EVN_HCM_001', 'ĐIỆN LỰC EVN HCM', 450000.0000, 'DEBIT', 'BILL_PAYMENT', 'ELECTRICITY', 'Thanh toán tiền điện tháng 09/2026', 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1 hours'),
('TX-READ-003', 'TX-1003', 'CUST-001', '1000188888', 'MERCHANT_HIGHLANDS', 'HIGHLANDS COFFEE', 65000.0000, 'DEBIT', 'QR_PAYMENT', 'FOOD_BEVERAGE', 'Quét mã VietQR thanh toán cà phê', 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;
