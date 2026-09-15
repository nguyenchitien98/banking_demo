# Bài 01 — PostgreSQL: Đặc Tính Nổi Bật & So Sánh Với Các Database Khác

> **Mục tiêu:** Hiểu tại sao BankX chọn PostgreSQL, PostgreSQL có gì đặc biệt, khác gì MySQL/MongoDB/SQL Server

---

## 1. PostgreSQL Là Gì?

```
PostgreSQL = "The World's Most Advanced Open Source Relational Database"
(Tuyên bố của chính họ — và không phải không có lý do)

Lịch sử:
  1986: Bắt đầu tại UC Berkeley (POSTGRES project)
  1996: Đổi tên thành PostgreSQL, thêm SQL support
  Hiện tại: Version 16 (2023), Version 17 (2024)

Dùng bởi: Apple, Instagram, Reddit, Twitch, Spotify, BankX 😄

License: PostgreSQL License (MIT-like) — Hoàn toàn miễn phí, kể cả production
```

---

## 2. PostgreSQL vs MySQL — Bảng So Sánh Đầy Đủ

| Tính Năng | PostgreSQL | MySQL |
|---|---|---|
| **ACID** | ✅ Đầy đủ, mọi storage engine | ✅ Chỉ InnoDB |
| **MVCC** | ✅ Native (không dùng locks cho reads) | ⚠️ InnoDB có, nhưng khác implementation |
| **JSON Support** | ✅ `JSON`, `JSONB` (binary, indexable) | ⚠️ JSON column (không binary, ít feature hơn) |
| **Window Functions** | ✅ Đầy đủ từ rất lâu | ✅ Từ MySQL 8.0 |
| **CTEs (WITH clause)** | ✅ Kể cả recursive, writable | ✅ MySQL 8.0+ |
| **`RETURNING`** | ✅ **Native** | ❌ Không có — phải dùng `LAST_INSERT_ID()` |
| **Partial Index** | ✅ `WHERE` clause trong index | ❌ Không có |
| **Full-Text Search** | ✅ Built-in, `tsvector`, `tsquery`, GIN index | ⚠️ Có nhưng kém hơn |
| **Inheritance** | ✅ Table inheritance | ❌ |
| **Custom Types** | ✅ `CREATE TYPE`, Composite, Enum, Domain | ⚠️ Giới hạn |
| **Extensions** | ✅ PostGIS, pg_trgm, TimescaleDB, pgcrypto | ❌ Ít hơn nhiều |
| **Array Column** | ✅ `INTEGER[]`, `TEXT[]` native | ❌ Không có |
| **Triggers** | ✅ Row-level và Statement-level | ✅ |
| **Replication** | ✅ Streaming, Logical | ✅ |
| **Licensing** | ✅ PostgreSQL License (free) | ⚠️ GPL (commercial use cần license Oracle) |
| **Performance (Write-heavy)** | ✅ Tốt | ✅ Tốt hơn đôi chút với simple queries |
| **Performance (Read-heavy complex query)** | ✅ Tốt hơn với complex queries | ⚠️ |

---

## 3. PostgreSQL vs MongoDB

| Tính Năng | PostgreSQL | MongoDB |
|---|---|---|
| **Model** | Relational (tables, rows) | Document (collections, documents/JSON) |
| **Schema** | Strict Schema (có thể flexible với JSONB) | Flexible Schema (no schema enforcement mặc định) |
| **ACID** | ✅ Single & multi-document | ✅ Từ MongoDB 4.0 (multi-document) |
| **Joins** | ✅ Native JOIN | ⚠️ `$lookup` (kém hiệu quả hơn) |
| **Transactions** | ✅ Mature, battle-tested | ⚠️ Mới hơn, ít tối ưu hơn |
| **Horizontal Scaling** | ⚠️ Khó hơn (Citus, sharding) | ✅ Built-in sharding |
| **Query Language** | ✅ SQL (universal) | Proprietary query language |
| **Banking Use Case** | ✅ **Phù hợp nhất** | ❌ Không phù hợp (ACID critical) |

**Tại sao Banking KHÔNG nên dùng MongoDB:**
- Trong banking, mọi giao dịch đều cần ACID đầy đủ.
- JOIN giữa accounts, transfers, ledger entries là thường xuyên — MongoDB xử lý kém.
- Double-entry bookkeeping cần transactional integrity cực kỳ chặt chẽ.
- Regulatory compliance thường yêu cầu structured, auditable data.

---

## 4. PostgreSQL vs SQL Server (Microsoft)

| Tính Năng | PostgreSQL | SQL Server |
|---|---|---|
| **Cost** | ✅ Free | ❌ Rất đắt ($14,256+/core/năm) |
| **Platform** | ✅ Linux, Windows, macOS | Windows chủ yếu |
| **`RETURNING`** | ✅ `RETURNING` | ⚠️ `OUTPUT INSERTED.*` (tương đương) |
| **Performance** | Ngang ngửa | Ngang ngửa |
| **Banking adoption** | ✅ Nhiều fintech | ✅ Nhiều enterprise bank (legacy) |

---

## 5. Các Đặc Tính Nổi Bật Của PostgreSQL Mà Developer Phải Biết

### 5.1. MVCC — Multi-Version Concurrency Control

```
Đây là điểm cốt lõi nhất của PostgreSQL — hiểu MVCC = hiểu PostgreSQL

Vấn đề truyền thống (Lock-based):
  Reader Thread: Đọc bảng → Phải chờ nếu có Writer đang ghi
  Writer Thread: Đang ghi → Lock table/row → Block tất cả Readers

MVCC của PostgreSQL:
  Readers KHÔNG BAO GIỜ block Writers
  Writers KHÔNG BAO GIỜ block Readers
  → Concurrency cực kỳ cao!

Cơ chế:
  Mỗi row có: xmin (transaction ID tạo row) + xmax (transaction ID xóa row)
  Mỗi transaction có Transaction ID riêng
  Reader thấy snapshot của DB tại thời điểm transaction bắt đầu
  Writer tạo row MỚI thay vì overwrite row cũ (cũ giữ nguyên)
  VACUUM định kỳ dọn dẹp các "dead rows" (old versions)
```

```sql
-- Demo MVCC:
BEGIN;
  SELECT * FROM bank_accounts WHERE id = 1;
  -- Thấy balance = 5,000,000 (snapshot tại BEGIN)

  -- Trong lúc này, transaction khác UPDATE balance = 6,000,000 và COMMIT
  -- PostgreSQL tạo row VERSION MỚI với balance = 6,000,000
  -- Transaction của ta KHÔNG thấy row mới (snapshot cũ)

  SELECT * FROM bank_accounts WHERE id = 1;
  -- VẪN thấy balance = 5,000,000 (consistent snapshot)
COMMIT;
-- Sau commit, SELECT mới thấy 6,000,000
```

---

### 5.2. Data Types Phong Phú — Không Nơi Nào Có

```sql
-- === NUMERIC ===
SMALLINT          -- 2 bytes, -32768 đến 32767
INTEGER           -- 4 bytes
BIGINT            -- 8 bytes (BankX: account ID, transfer ID)
DECIMAL(19, 4)    -- Exact precision (BankX: tiền tệ)
NUMERIC           -- Tương tự DECIMAL
REAL              -- 4-byte float (không dùng cho tiền!)
DOUBLE PRECISION  -- 8-byte float (không dùng cho tiền!)

-- === TEXT ===
VARCHAR(n)        -- Có độ dài tối đa
TEXT              -- Không giới hạn độ dài (BankX: description, notes)
CHAR(n)           -- Độ dài cố định, padding spaces

-- === DATE/TIME ===
DATE                    -- Chỉ ngày: '2026-09-16'
TIME                    -- Chỉ giờ: '06:30:00'
TIMESTAMP               -- Ngày + giờ, không timezone
TIMESTAMPTZ             -- Ngày + giờ + timezone (BankX dùng cái này!)
INTERVAL                -- Khoảng thời gian: '2 hours', '30 days'

-- === BOOLEAN ===
BOOLEAN           -- TRUE / FALSE / NULL (3-valued logic!)

-- === UUID ===
UUID              -- '550e8400-e29b-41d4-a716-446655440000'
                  -- BankX: customer_id, transfer_id, account_id
gen_random_uuid() -- Tạo UUID v4 ngẫu nhiên (cần pgcrypto extension)

-- === JSON (PostgreSQL đặc biệt!) ===
JSON    -- Text, kiểm tra cú pháp JSON, không indexable
JSONB   -- Binary, nén, reorders keys, INDEXABLE với GIN!

-- ví dụ BankX - lưu device info của transfer:
ALTER TABLE bank_transfers ADD COLUMN device_info JSONB;
INSERT INTO bank_transfers (device_info) VALUES ('{"ip": "1.2.3.4", "os": "iOS"}');
SELECT device_info->>'ip' FROM bank_transfers; -- Truy cập JSON field

-- Index trên JSONB field!
CREATE INDEX idx_transfers_ip ON bank_transfers USING GIN(device_info);

-- === ARRAY (PostgreSQL specific!) ===
TEXT[]      -- Mảng text
INTEGER[]   -- Mảng số nguyên

-- Ví dụ:
ALTER TABLE customers ADD COLUMN roles TEXT[];
INSERT INTO customers (roles) VALUES (ARRAY['CUSTOMER', 'PREMIUM']);
SELECT * FROM customers WHERE 'ADMIN' = ANY(roles); -- Tìm users có role ADMIN

-- === HSTORE (Key-Value trong 1 column) ===
-- Ít dùng hơn JSONB, nhưng vẫn có

-- === ENUM ===
CREATE TYPE transfer_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
ALTER TABLE bank_transfers ADD COLUMN status transfer_status;
-- Type-safe, không thể insert giá trị không hợp lệ

-- === RANGE ===
DATERANGE   -- Khoảng ngày
TSTZRANGE   -- Khoảng timestamp với timezone
-- Ví dụ: Kiểm tra overlap giữa 2 khoảng thời gian
WHERE validity_period && '[2026-01-01, 2026-12-31]'::daterange
```

---

### 5.3. Extensions — PostgreSQL "Siêu Năng Lực"

```sql
-- Xem extensions đang cài:
SELECT * FROM pg_extension;

-- Cài thêm extension:
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";     -- Cryptographic functions
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- Trigram similarity (fuzzy search)
CREATE EXTENSION IF NOT EXISTS "btree_gin";    -- GIN index cho standard types
CREATE EXTENSION IF NOT EXISTS "tablefunc";    -- CROSSTAB (pivot tables)

-- PostGIS: Dữ liệu địa lý (địa chỉ, khoảng cách, bản đồ)
CREATE EXTENSION IF NOT EXISTS "postgis";
SELECT ST_Distance(point1, point2) FROM locations; -- Tính khoảng cách

-- TimescaleDB: Time-series data (metrics, logs)
-- Phù hợp cho Engineering Portal metrics

-- pg_trgm — Fuzzy search tên khách hàng:
SELECT * FROM customers
WHERE similarity(full_name, 'Nguyen Van A') > 0.3
ORDER BY similarity(full_name, 'Nguyen Van A') DESC;
-- Tìm "Nguyễn Văn A" kể cả khi gõ sai chính tả
```

---

### 5.4. Full-Text Search — Tìm Kiếm Toàn Văn

```sql
-- Tạo tsvector column cho description:
ALTER TABLE bank_transfers ADD COLUMN description_tsv TSVECTOR;

-- Tạo trigger tự động update tsvector:
UPDATE bank_transfers
SET description_tsv = to_tsvector('simple', COALESCE(description, ''));

-- Index GIN trên tsvector:
CREATE INDEX idx_transfers_fts ON bank_transfers USING GIN(description_tsv);

-- Full-text search:
SELECT * FROM bank_transfers
WHERE description_tsv @@ to_tsquery('simple', 'chuyển & tiền')
ORDER BY ts_rank(description_tsv, to_tsquery('simple', 'chuyển & tiền')) DESC;
-- Nhanh hơn LIKE '%chuyển tiền%' rất nhiều trên bảng lớn
```

---

### 5.5. Table Inheritance — Tính Kế Thừa

```sql
-- Ví dụ: Các loại giao dịch kế thừa từ bảng gốc
CREATE TABLE transactions (
  id          BIGSERIAL PRIMARY KEY,
  amount      DECIMAL(19,4),
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE internal_transfers (
  from_account_id UUID NOT NULL,
  to_account_id   UUID NOT NULL
) INHERITS (transactions); -- Kế thừa tất cả columns từ transactions

CREATE TABLE interbank_transfers (
  napas_ref       VARCHAR(50),
  bank_code       VARCHAR(10)
) INHERITS (transactions);

-- Query transactions bao gồm tất cả loại:
SELECT * FROM transactions; -- Trả về cả internal_transfers lẫn interbank_transfers

-- Query chỉ 1 loại:
SELECT * FROM ONLY transactions; -- Chỉ rows trong bảng gốc, không kế thừa
```

---

### 5.6. Generated Columns (PostgreSQL 12+)

```sql
-- Column tự tính toán từ column khác, lưu vào disk
ALTER TABLE bank_accounts ADD COLUMN
  balance_tier VARCHAR(10) GENERATED ALWAYS AS (
    CASE
      WHEN balance >= 100000000 THEN 'PLATINUM'
      WHEN balance >= 10000000  THEN 'GOLD'
      WHEN balance >= 1000000   THEN 'SILVER'
      ELSE 'STANDARD'
    END
  ) STORED;
-- Tự động tính lại khi balance thay đổi
-- Không cần trigger, không cần application logic
```

---

### 5.7. Row-Level Security (RLS) — Bảo Mật Cấp Hàng

```sql
-- Tự động filter data dựa trên current user — không cần WHERE trong mọi query!
ALTER TABLE bank_accounts ENABLE ROW LEVEL SECURITY;

-- Policy: Customer chỉ thấy account của mình
CREATE POLICY customer_accounts_policy ON bank_accounts
  FOR SELECT
  USING (customer_id = current_setting('app.current_customer_id')::UUID);

-- Bây giờ mọi SELECT đều tự động có WHERE customer_id = {current_customer}
-- Không cần developer nhớ thêm WHERE mỗi lần
```

---

## 6. PostgreSQL vs Các Database Khác — Bảng Tổng Kết

```
Chọn PostgreSQL khi:
  ✅ Cần ACID đầy đủ (banking, fintech, e-commerce)
  ✅ Data có structure phức tạp, nhiều relationships
  ✅ Cần JSONB (flexible data + indexable)
  ✅ Complex queries (window functions, CTEs, JOINs nhiều bảng)
  ✅ Budget hạn chế (open source, free)
  ✅ Cần extensibility (PostGIS, TimescaleDB...)
  ✅ Long-term project (codebase mature, community lớn)

Chọn MySQL khi:
  ✅ Team quen MySQL, không muốn học PostgreSQL
  ✅ Đơn giản, tốc độ read cao cho simple queries
  ✅ Hệ thống có sẵn dùng MySQL

Chọn MongoDB khi:
  ✅ Schema thực sự flexible, thay đổi liên tục
  ✅ Horizontal scaling từ đầu (sharding built-in)
  ✅ Document-oriented data (catalog, CMS, logs)
  ❌ KHÔNG dùng cho banking/fintech

Chọn Redis khi:
  ✅ In-memory speed (microseconds)
  ✅ Cache, session, rate limiting, pub/sub
  ❌ Không phải primary database cho persistent data

Chọn TimescaleDB (PostgreSQL extension) khi:
  ✅ Time-series data (metrics, IoT, logs)
  ✅ Muốn SQL interface cho time-series
```

---

**← [README](./README.md)** | **→ [Bài 02 — SQL Nâng Cao](./02_postgresql_sql_nang_cao.md)**
