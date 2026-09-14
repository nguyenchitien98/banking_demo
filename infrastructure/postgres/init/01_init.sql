-- =============================================================================
-- BankX Database Initialization Script
-- Chạy tự động khi PostgreSQL container khởi động lần đầu
-- =============================================================================

-- Tạo extension cho UUID generation (dùng gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tạo extension cho full-text search (Phase 3+ cho tìm kiếm transaction)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- =============================================================================
-- SCHEMA SEPARATION (Phase 2+ sẽ tách ra nhiều DB)
-- Hiện tại dùng chung 1 DB với 1 schema public
-- =============================================================================

-- Đặt timezone mặc định về Asia/Ho_Chi_Minh
ALTER DATABASE bankx_db SET timezone TO 'Asia/Ho_Chi_Minh';

-- Log để xác nhận script chạy thành công
DO $$
BEGIN
    RAISE NOTICE 'BankX database initialization completed successfully';
    RAISE NOTICE 'Extensions: pgcrypto, pg_trgm';
    RAISE NOTICE 'Timezone: Asia/Ho_Chi_Minh';
END $$;
