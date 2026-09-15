-- =============================================================================
-- Flyway Migration V2 — Auth Module Tables (refresh_tokens & audit_logs)
-- =============================================================================

-- 1. Bảng quản lý Refresh Token (refresh_tokens)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_token ON refresh_tokens(token);

-- 2. Bảng Audit Log xác thực (auth_audit_logs)
CREATE TABLE IF NOT EXISTS auth_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    username VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, REFRESH_TOKEN
    ip_address VARCHAR(50),
    user_agent TEXT,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_audit_user ON auth_audit_logs(user_id);
CREATE INDEX idx_auth_audit_created ON auth_audit_logs(created_at);
