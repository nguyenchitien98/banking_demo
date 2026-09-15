-- =============================================================================
-- Flyway Migration V3 — Customer Module & KYC Tables
-- =============================================================================

-- 1. Bảng tài liệu KYC khách hàng (kyc_documents)
CREATE TABLE IF NOT EXISTS kyc_documents (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- IDENTITY_CARD, PASSPORT, DRIVER_LICENSE
    document_number VARCHAR(50) NOT NULL,
    front_image_url VARCHAR(512),
    back_image_url VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_customer ON kyc_documents(customer_id);
CREATE INDEX idx_kyc_status ON kyc_documents(status);
