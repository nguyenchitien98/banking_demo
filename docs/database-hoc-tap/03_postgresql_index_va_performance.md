# Bài 03 — PostgreSQL: Index & Performance

> **Mục tiêu:** Thiết kế index đúng, đọc EXPLAIN ANALYZE, tối ưu query chậm  
> **Liên quan BankX:** Tối ưu ledger_entries, bank_transfers với hàng triệu records

---

## 1. Các Loại Index Trong PostgreSQL

### 1.1. B-Tree — Index Mặc Định (Dùng 90% trường hợp)

```sql
-- B-Tree phù hợp cho: =, <, >, <=, >=, BETWEEN, IN, LIKE 'prefix%'
-- Tự động tạo khi có PRIMARY KEY hoặc UNIQUE constraint

-- Tạo B-Tree index:
CREATE INDEX idx_transfers_from_account
  ON bank_transfers (from_account_id);

-- Composite B-Tree (thứ tự columns quan trọng!):
CREATE INDEX idx_transfers_account_status_date
  ON bank_transfers (from_account_id, status, created_at DESC);
-- Query: WHERE from_account_id = ? AND status = ? → Dùng được index
-- Query: WHERE from_account_id = ? → Dùng được (prefix của composite)
-- Query: WHERE status = ? → KHÔNG dùng được (không phải prefix)
-- → Rule: Các columns trong WHERE phải là PREFIX của composite index

-- DESC trong index:
CREATE INDEX idx_transfers_created_desc
  ON bank_transfers (created_at DESC);
-- ORDER BY created_at DESC → Sử dụng index thay vì sort
```

### 1.2. Partial Index — Index Có Điều Kiện (PostgreSQL Đặc Biệt)

```sql
-- Chỉ index rows thỏa mãn WHERE → Index nhỏ hơn, nhanh hơn

-- Chỉ index transfers ĐANG XỬ LÝ (không index completed/failed):
CREATE INDEX idx_transfers_pending
  ON bank_transfers (created_at, from_account_id)
  WHERE status IN ('PENDING', 'PROCESSING', 'PENDING_OTP');
-- Thay vì index 10 triệu rows → Chỉ index ~10.000 rows pending
-- Query: WHERE status = 'PENDING' ORDER BY created_at → Cực nhanh

-- Chỉ index outbox events chưa xử lý:
CREATE INDEX idx_outbox_pending
  ON outbox_events (created_at ASC)
  WHERE status = 'PENDING';
-- OutboxPollingService query: SELECT * FROM outbox_events WHERE status='PENDING' LIMIT 100
-- → Instant với partial index

-- Chỉ index customers có email (một số có thể null):
CREATE INDEX idx_customers_email
  ON customers (email)
  WHERE email IS NOT NULL;
```

### 1.3. Covering Index — Index Chứa Đủ Data Để Tránh Heap Fetch

```sql
-- "Index-only scan": PostgreSQL đọc index mà KHÔNG cần đọc actual table rows

CREATE INDEX idx_transfers_covering
  ON bank_transfers (from_account_id, created_at DESC)
  INCLUDE (amount, status, to_account_id);
--          ↑ INCLUDE: Thêm columns vào index nhưng KHÔNG dùng để lookup/sort
--            Chỉ dùng để đọc value từ index mà không fetch heap

-- Query này sẽ dùng Index-Only Scan (không đọc table):
SELECT from_account_id, created_at, amount, status
FROM bank_transfers
WHERE from_account_id = 'acc-001'
ORDER BY created_at DESC
LIMIT 20;
-- EXPLAIN: "Index Only Scan using idx_transfers_covering"
```

### 1.4. GIN Index — Cho Array, JSONB, Full-Text Search

```sql
-- GIN (Generalized Inverted Index): Tốt cho multi-value data

-- Index cho JSONB:
CREATE INDEX idx_transfers_device_info
  ON bank_transfers USING GIN (device_info);
-- Query: WHERE device_info @> '{"os": "iOS"}' → Sử dụng GIN index
-- @>: "contains" operator

-- Index cho Array column:
CREATE INDEX idx_customers_roles
  ON customers USING GIN (roles);
-- Query: WHERE 'PREMIUM' = ANY(roles) → Sử dụng GIN index

-- Index cho Full-Text Search:
CREATE INDEX idx_transfers_description_fts
  ON bank_transfers USING GIN (to_tsvector('simple', description));
-- Query: WHERE to_tsvector('simple', description) @@ to_tsquery('simple', 'chuyển')
```

### 1.5. GiST Index — Cho Ranges, Geometry

```sql
-- GiST: Tốt cho range data, spatial data
CREATE INDEX idx_promotions_valid_range
  ON promotions USING GIST (valid_period);  -- valid_period là DATERANGE hoặc TSTZRANGE

-- Query: Tìm promotions đang hiệu lực hôm nay:
SELECT * FROM promotions WHERE valid_period @> CURRENT_DATE;
-- @>: "contains" — ngày hiện tại nằm trong khoảng valid_period
```

### 1.6. Hash Index — Chỉ Cho `=` (Hiếm Dùng)

```sql
-- Hash index chỉ hỗ trợ = equality, không hỗ trợ range (<, >, BETWEEN)
-- Nhanh hơn B-Tree cho = nhưng không WAL-logged đến PostgreSQL 10
-- Hiếm khi dùng — B-Tree thường đủ
CREATE INDEX idx_transfers_ref_hash
  ON bank_transfers USING HASH (transaction_reference);
```

---

## 2. EXPLAIN ANALYZE — Đọc Execution Plan

```sql
-- EXPLAIN: Hiển thị plan mà PostgreSQL dự định chạy
-- EXPLAIN ANALYZE: Thực sự CHẠY query và đo thời gian thực tế
-- EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT): Chi tiết nhất

EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT t.id, t.amount, t.status, a.account_name
FROM bank_transfers t
JOIN bank_accounts a ON a.id = t.from_account_id
WHERE t.from_account_id = 'acc-001'
  AND t.status = 'COMPLETED'
  AND t.created_at >= NOW() - INTERVAL '30 days'
ORDER BY t.created_at DESC
LIMIT 20;
```

### Đọc Output EXPLAIN ANALYZE:

```
Limit  (cost=0.56..8.50 rows=20 width=128) (actual time=0.123..0.456 rows=20 loops=1)
  -> Index Scan using idx_transfers_account_status_date on bank_transfers t
       (cost=0.56..450.23 rows=1893 width=120) (actual time=0.115..0.421 rows=20 loops=1)
       Index Cond: ((from_account_id = 'acc-001') AND (status = 'COMPLETED'))
       Filter: (created_at >= (now() - '30 days'::interval))
       Rows Removed by Filter: 3
     -> Index Scan using bank_accounts_pkey on bank_accounts a
          (cost=0.29..0.31 rows=1 width=36) (actual time=0.008..0.008 rows=1 loops=20)
          Index Cond: (id = t.from_account_id)
Planning Time: 0.432 ms
Execution Time: 0.521 ms
```

**Giải thích từng phần:**

```
cost=0.56..8.50
      ↑      ↑
      |      └── cost khi trả về row cuối cùng
      └── Startup cost (trước khi trả row đầu tiên)
      Đây là ESTIMATED cost (không phải ms) — để compare relative

actual time=0.123..0.456
             ↑       ↑
             |       └── Thực tế đến khi trả row cuối
             └── Thực tế startup time (ms)

rows=20   → Thực tế trả về 20 rows
loops=1   → Node này chạy 1 lần

"Index Scan" → Dùng index ✅ (tốt)
"Seq Scan"   → Full table scan ❌ (thường là vấn đề)
"Bitmap Heap Scan" → Dùng index nhưng fetch heap ngẫu nhiên
"Index Only Scan" → Chỉ đọc index, không fetch heap ⭐ (tốt nhất)

"Rows Removed by Filter: 3" → Index scan lấy thêm 3 rows nhưng bị filter bởi condition
→ Index không cover hoàn toàn điều kiện WHERE
```

---

## 3. Các Execution Node Types

```
Hash Join          → Join bằng hash table (tốt cho large tables)
Merge Join         → Join bằng cách merge 2 sorted sets (tốt khi đã có index)
Nested Loop        → Với mỗi row outer → scan inner (tốt khi inner nhỏ)

Sort               → Sort trên disk hoặc memory
  → Nếu "Sort Method: external merge" → Sort trên disk → Tăng work_mem
  → Nếu "Sort Method: quicksort" → Sort trong memory → Tốt

Gather/Gather Merge → Parallel query execution
Aggregate          → GROUP BY, COUNT, SUM...
```

---

## 4. VACUUM — Dọn Dẹp Dead Rows (MVCC Dead Tuples)

```sql
-- MVCC tạo ra "dead rows" (rows cũ sau UPDATE/DELETE)
-- VACUUM dọn dẹp chúng để PostgreSQL có thể tái dùng space

-- Xem dead rows đang tích lũy:
SELECT
  schemaname, tablename,
  n_live_tup,           -- Rows đang sống
  n_dead_tup,           -- Dead rows chưa được VACUUM
  last_vacuum,          -- Lần VACUUM thủ công cuối
  last_autovacuum       -- Lần autovacuum cuối
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;

-- Manual VACUUM:
VACUUM bank_transfers;          -- Dọn dead rows (không lock table)
VACUUM ANALYZE bank_transfers;  -- Dọn + Update statistics
VACUUM FULL bank_transfers;     -- Rewrite table (lock table! Dùng offline)

-- Autovacuum config tùy chỉnh cho bảng write-heavy:
ALTER TABLE ledger_entries SET (
  autovacuum_vacuum_scale_factor = 0.01,  -- VACUUM khi 1% rows là dead (thay vì 20%)
  autovacuum_analyze_scale_factor = 0.005 -- ANALYZE khi 0.5% rows thay đổi
);
```

---

## 5. Table Partitioning

```sql
-- === RANGE PARTITIONING (Phổ biến nhất cho time-series) ===
CREATE TABLE bank_transfers (
  id              BIGSERIAL,
  from_account_id UUID NOT NULL,
  to_account_id   UUID NOT NULL,
  amount          DECIMAL(19,4),
  status          VARCHAR(20),
  created_at      TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (id, created_at)  -- Partition key phải trong PK
) PARTITION BY RANGE (created_at);

-- Tạo partitions:
CREATE TABLE bank_transfers_2026_q1
  PARTITION OF bank_transfers
  FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');

CREATE TABLE bank_transfers_2026_q2
  PARTITION OF bank_transfers
  FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');

CREATE TABLE bank_transfers_2026_q3
  PARTITION OF bank_transfers
  FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');

-- Default partition (bắt các rows không match partition nào):
CREATE TABLE bank_transfers_default
  PARTITION OF bank_transfers DEFAULT;

-- Tự động tạo partition mỗi quý (cần cron job hoặc pg_partman extension)

-- === Partition Pruning — Chỉ scan partitions liên quan ===
SELECT * FROM bank_transfers
WHERE created_at >= '2026-09-01' AND created_at < '2026-10-01';
-- EXPLAIN: "Seq Scan on bank_transfers_2026_q3" (chỉ scan 1 partition!)

-- === Drop old partition — Nhanh hơn DELETE hàng triệu records ===
DROP TABLE bank_transfers_2025_q1;  -- Instant! Không cần DELETE + VACUUM
-- Giữ 24 tháng gần nhất, drop cũ hơn

-- === Hash Partitioning (cho even distribution) ===
CREATE TABLE bank_accounts (id UUID, ...) PARTITION BY HASH (id);
CREATE TABLE bank_accounts_0 PARTITION OF bank_accounts FOR VALUES WITH (modulus 4, remainder 0);
CREATE TABLE bank_accounts_1 PARTITION OF bank_accounts FOR VALUES WITH (modulus 4, remainder 1);
-- Tự động phân phối đều dựa trên HASH của id
```

---

## 6. Connection Pooling với PgBouncer

```
Vấn đề: PostgreSQL tối đa ~300-500 connections thực sự
         Java app có thể có 1000 threads muốn kết nối cùng lúc

Giải pháp: PgBouncer — Connection Pooler đứng trước PostgreSQL

App Instance 1 (100 connections)  ┐
App Instance 2 (100 connections)  ├──→ PgBouncer → PostgreSQL (20 connections)
App Instance 3 (100 connections)  ┘    (pool 300→20)

PgBouncer Modes:
  Session mode:     1 client = 1 server connection suốt session
  Transaction mode: Server connection chỉ hold khi đang trong transaction
                    → Hiệu quả nhất cho banking (request nhanh)
  Statement mode:   Server connection free sau mỗi statement
```

```ini
; pgbouncer.ini:
[databases]
bankx = host=localhost port=5432 dbname=bankx_db

[pgbouncer]
pool_mode = transaction        ; Transaction pooling
max_client_conn = 1000        ; Tối đa 1000 client connections
default_pool_size = 25        ; 25 connections thực đến PostgreSQL
min_pool_size = 5             ; Giữ sẵn 5 connections warm
server_idle_timeout = 600     ; Đóng idle connection sau 10 phút
```

---

## 7. PostgreSQL Monitoring Queries

```sql
-- === Xem queries đang chạy ===
SELECT pid, now() - query_start AS duration, state, query
FROM pg_stat_activity
WHERE state != 'idle'
  AND query_start < NOW() - INTERVAL '5 seconds'  -- Chạy > 5 giây
ORDER BY duration DESC;

-- === Xem locks đang block nhau ===
SELECT
  blocked.pid AS blocked_pid,
  blocked.query AS blocked_query,
  blocking.pid AS blocking_pid,
  blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_stat_activity blocking
  ON blocking.pid = ANY(pg_blocking_pids(blocked.pid));

-- === Index usage stats ===
SELECT
  schemaname, tablename, indexname,
  idx_scan AS times_used,        -- Số lần index được dùng
  idx_tup_read,                  -- Rows được đọc từ index
  idx_tup_fetch                  -- Rows được fetch từ heap qua index
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
-- Index có idx_scan = 0 sau vài tuần → Không ai dùng → DROP đi

-- === Table size ===
SELECT
  table_name,
  pg_size_pretty(pg_total_relation_size(table_name::text)) AS total_size,
  pg_size_pretty(pg_table_size(table_name::text)) AS table_size,
  pg_size_pretty(pg_indexes_size(table_name::text)) AS index_size
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY pg_total_relation_size(table_name::text) DESC;

-- === Slow queries (cần pg_stat_statements extension) ===
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
-- Top 20 queries chậm nhất theo avg execution time
```

---

## 8. Index Best Practices — Checklist BankX

```sql
-- ✅ Đã có trong BankX:
-- 1. Primary Key tự động tạo B-Tree index
CREATE TABLE bank_transfers (id BIGSERIAL PRIMARY KEY, ...);

-- 2. Index trên foreign keys (Hibernate không tự tạo!)
CREATE INDEX idx_transfers_from_account ON bank_transfers (from_account_id);
CREATE INDEX idx_transfers_to_account   ON bank_transfers (to_account_id);
-- Nếu không có: JOIN với bank_accounts = Seq Scan!

-- 3. Partial index cho outbox polling
CREATE INDEX idx_outbox_pending ON outbox_events (created_at)
WHERE status = 'PENDING';

-- 4. Composite index cho query phổ biến
CREATE INDEX idx_transfers_account_date
ON bank_transfers (from_account_id, created_at DESC);

-- ❌ Tránh:
-- Over-indexing: Mỗi index tốn space và CHẬM DOWN write (INSERT/UPDATE/DELETE phải cập nhật index)
-- Duplicate indexes: (a), (a, b) → Index (a) bị redundant với composite (a, b)
-- Index trên low-cardinality column (vd: gender, boolean) → Ít hiệu quả
-- Không ANALYZE sau khi load data lớn → Planner dùng stats cũ → Plan sai
```

---

**← [Bài 02 — SQL Nâng Cao](./02_postgresql_sql_nang_cao.md)** | **→ [Bài 04 — Transaction & Concurrency](./04_postgresql_transaction_va_concurrency.md)**
