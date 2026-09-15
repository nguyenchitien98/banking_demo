-- V16__init_saga.sql: Saga Orchestration & State Machine Persistence

CREATE TABLE IF NOT EXISTS saga_instances (
    saga_id VARCHAR(64) PRIMARY KEY,
    transfer_code VARCHAR(64) NOT NULL UNIQUE,
    current_state VARCHAR(64) NOT NULL,
    source_account_number VARCHAR(64) NOT NULL,
    target_account_number VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    payload_json TEXT,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS saga_audit_steps (
    id VARCHAR(64) PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    state_before VARCHAR(64) NOT NULL,
    state_after VARCHAR(64) NOT NULL,
    is_compensating BOOLEAN NOT NULL DEFAULT FALSE,
    details VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_saga_instances_code ON saga_instances(transfer_code);
CREATE INDEX IF NOT EXISTS idx_saga_audit_steps_saga ON saga_audit_steps(saga_id);

-- Seed mẫu 1 Saga Instance cho kiểm thử
INSERT INTO saga_instances (
    saga_id, transfer_code, current_state, source_account_number, target_account_number, amount, payload_json, failure_reason, created_at, updated_at
) VALUES
('SAGA-1001', 'SAGA-TRF-001', 'COMPLETED', '1000188888', '1000299999', 2000000.0000, '{"description":"Transfer via Saga State Machine"}', NULL, CURRENT_TIMESTAMP - INTERVAL '1 hours', CURRENT_TIMESTAMP - INTERVAL '1 hours')
ON CONFLICT (saga_id) DO NOTHING;

INSERT INTO saga_audit_steps (id, saga_id, step_name, state_before, state_after, is_compensating, details, created_at)
VALUES
('STEP-001', 'SAGA-1001', 'INITIATE_SAGA', 'NOT_STARTED', 'STARTED', FALSE, 'Khởi tạo Saga Instance', CURRENT_TIMESTAMP - INTERVAL '1 hours'),
('STEP-002', 'SAGA-1001', 'EXECUTE_DEBIT', 'STARTED', 'DEBIT_COMPLETED', FALSE, 'Trừ tiền tài khoản nguồn 1000188888 thành công', CURRENT_TIMESTAMP - INTERVAL '1 hours'),
('STEP-003', 'SAGA-1001', 'EXECUTE_CREDIT', 'DEBIT_COMPLETED', 'CREDIT_COMPLETED', FALSE, 'Cộng tiền tài khoản đích 1000299999 thành công', CURRENT_TIMESTAMP - INTERVAL '1 hours'),
('STEP-004', 'SAGA-1001', 'RECORD_LEDGER', 'CREDIT_COMPLETED', 'COMPLETED', FALSE, 'Hạch toán bút toán ghi sổ kép thành công', CURRENT_TIMESTAMP - INTERVAL '1 hours')
ON CONFLICT (id) DO NOTHING;
