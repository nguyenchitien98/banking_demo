-- =============================================================================
-- Flyway Migration V4 — Account Module & Transfer Limits
-- =============================================================================

-- 1. Bảng hạn mức giao dịch chuyển tiền (transfer_limits)
CREATE TABLE IF NOT EXISTS transfer_limits (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    single_limit NUMERIC(19, 4) NOT NULL DEFAULT 50000000.0000, -- 50 triệu/giao dịch
    daily_limit NUMERIC(19, 4) NOT NULL DEFAULT 500000000.0000, -- 500 triệu/ngày
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transfer_limits_customer ON transfer_limits(customer_id);
