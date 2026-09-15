-- V12__init_fraud.sql: Flyway migration for Fraud Detection Module & Rule Engine

CREATE TABLE IF NOT EXISTS fraud_rules (
    id VARCHAR(64) PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    weight_score INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(64),
    source_account_id VARCHAR(64) NOT NULL,
    target_account_number VARCHAR(64),
    amount DECIMAL(19, 4) NOT NULL,
    risk_score INT NOT NULL,
    risk_action VARCHAR(32) NOT NULL,
    triggered_rules TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewer_notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_source_acc ON fraud_alerts(source_account_id);

-- Seed default built-in fraud rules
INSERT INTO fraud_rules (id, rule_code, rule_name, description, weight_score, is_active, created_at)
VALUES
('RULE-001', 'HIGH_AMOUNT', 'Giao dịch Giá trị Cao (>100M)', 'Giao dịch có số tiền vượt quá 100,000,000 VND', 40, TRUE, CURRENT_TIMESTAMP),
('RULE-002', 'HIGH_VELOCITY', 'Tần suất Giao dịch Dày đặc (>5 lệnh/phút)', 'Khách hàng chuyển tiền hơn 5 lần trong vòng 60 giây', 30, TRUE, CURRENT_TIMESTAMP),
('RULE-003', 'NEW_DEVICE', 'Thiết bị Lạ & Hạn mức Cao (>50M)', 'Giao dịch từ thiết bị chưa từng xác thực có giá trị >50M', 50, TRUE, CURRENT_TIMESTAMP),
('RULE-004', 'NEW_BENEFICIARY', 'Thụ hưởng Mới & Hạn mức Cao (>20M)', 'Chuyển tiền lần đầu cho tài khoản thụ hưởng mới số tiền >20M', 20, TRUE, CURRENT_TIMESTAMP),
('RULE-005', 'NIGHT_TIME', 'Giao dịch Đêm khuya (0h-4h) (>10M)', 'Giao dịch phát sinh trong khung giờ nhạy cảm từ 00:00 đến 04:00', 15, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Seed sample fraud alert for initial review testing
INSERT INTO fraud_alerts (id, transaction_id, source_account_id, target_account_number, amount, risk_score, risk_action, triggered_rules, status, created_at)
VALUES
('ALERT-001', 'TX-778899', '1088889999', '9988776655', 120000000.0000, 75, 'BLOCK', 'HIGH_AMOUNT, NIGHT_TIME, NEW_BENEFICIARY', 'PENDING_REVIEW', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
