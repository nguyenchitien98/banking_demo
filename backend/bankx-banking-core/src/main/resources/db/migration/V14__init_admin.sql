-- V14__init_admin.sql: Flyway migration for Admin Portal & Audit Trail

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    admin_username VARCHAR(128) NOT NULL,
    admin_role VARCHAR(32) NOT NULL DEFAULT 'ROLE_ADMIN',
    action_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    details VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_username ON admin_audit_logs(admin_username);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON admin_audit_logs(action_type);

-- Seed initial admin audit logs
INSERT INTO admin_audit_logs (id, admin_username, admin_role, action_type, target_id, details, created_at)
VALUES
('LOG-001', 'admin_sys', 'ROLE_ADMIN', 'SYSTEM_INITIALIZATION', 'SYSTEM', 'Khởi tạo hệ thống Admin Portal & RBAC Security Rules', CURRENT_TIMESTAMP),
('LOG-002', 'teller_01', 'ROLE_TELLER', 'KYC_APPROVE', 'CUST-001', 'Duyệt hồ sơ eKYC cho khách hàng NGUYEN CHITIEN', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
