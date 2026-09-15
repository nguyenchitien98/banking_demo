# Bài 02 — PostgreSQL: SQL Nâng Cao

> **Mục tiêu:** Thành thạo các SQL features nâng cao của PostgreSQL mà MySQL/H2 không có  
> **Quan trọng nhất:** `RETURNING`, CTE, Window Functions, `ON CONFLICT`, JSON operators

---

## 1. RETURNING — Lấy Giá Trị Sau INSERT/UPDATE/DELETE

> ⭐ **Đây là câu hỏi phỏng vấn thực tế!**  
> *"Không dùng JPA, sau INSERT vào bảng có ID tự tăng, BE nhận ID đó thế nào?"*

### 1.1. RETURNING Sau INSERT

```sql
-- === BASIC: Lấy ID sau INSERT ===
INSERT INTO bank_accounts (account_name, balance, customer_id, status)
VALUES ('Tài khoản thanh toán', 0, 'cust-abc-123', 'ACTIVE')
RETURNING id;
-- Trả về: id = 42
-- PostgreSQL tạo ID từ SERIAL/BIGSERIAL rồi TRẢ VỀ NGAY trong cùng câu lệnh

-- === RETURNING nhiều columns ===
INSERT INTO bank_accounts (account_name, balance, customer_id)
VALUES ('Tài khoản tiết kiệm', 5000000, 'cust-abc-123')
RETURNING id, account_number, created_at, status;
-- Trả về: id=43, account_number="088880043", created_at="2026-09-16...", status="ACTIVE"

-- === RETURNING * — Tất cả columns ===
INSERT INTO bank_transfers (from_account_id, to_account_id, amount, description)
VALUES ('acc-001', 'acc-002', 500000, 'Chuyển tiền ăn trưa')
RETURNING *;
-- Trả về toàn bộ row vừa được INSERT
```

### 1.2. RETURNING Sau UPDATE

```sql
-- Cập nhật balance và lấy giá trị MỚI:
UPDATE bank_accounts
SET balance = balance - 500000,
    version = version + 1,    -- Optimistic lock increment
    updated_at = NOW()
WHERE id = 'acc-001'
  AND version = 5             -- Optimistic lock check
RETURNING id, balance, version, updated_at;
-- Trả về balance MỚI sau khi trừ, version MỚI

-- Tại sao hữu ích hơn SELECT sau UPDATE?
-- → Atomic! SELECT riêng sau UPDATE có thể thấy giá trị đã bị thay đổi bởi transaction khác
-- → RETURNING đảm bảo thấy chính xác giá trị mà UPDATE đã set
```

### 1.3. RETURNING Sau DELETE

```sql
-- Xóa và lấy rows đã xóa (để log/audit):
DELETE FROM outbox_events
WHERE id IN (
  SELECT id FROM outbox_events
  WHERE status = 'SENT'
    AND created_at < NOW() - INTERVAL '7 days'
  LIMIT 1000
)
RETURNING id, event_type, created_at;
-- Trả về danh sách events đã xóa — dùng để log
```

### 1.4. RETURNING Với CTE — Rất Mạnh

```sql
-- Pattern: INSERT → RETURNING → Dùng ngay trong query tiếp theo
WITH new_account AS (
  INSERT INTO bank_accounts (account_name, customer_id, balance)
  VALUES ('Tài khoản thanh toán', 'cust-001', 0)
  RETURNING id, account_number
),
initial_ledger AS (
  INSERT INTO ledger_entries (account_id, entry_type, amount, description)
  SELECT id, 'CREDIT', 0, 'Mở tài khoản'
  FROM new_account
  RETURNING id
)
SELECT a.id AS account_id, a.account_number, l.id AS ledger_id
FROM new_account a, initial_ledger l;
-- Tất cả trong 1 transaction, atomic!
```

### 1.5. Trong JDBC Thuần (Không JPA)

```java
// Cách 1: Dùng RETURNING trong SQL (PostgreSQL-specific)
String sql = """
    INSERT INTO bank_accounts (account_name, balance, customer_id, status)
    VALUES (?, ?, ?, 'ACTIVE')
    RETURNING id, account_number, created_at
    """;

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "Tài khoản thanh toán");
    ps.setBigDecimal(2, BigDecimal.ZERO);
    ps.setObject(3, customerId, Types.OTHER); // UUID type

    // Dùng executeQuery() (không phải executeUpdate!) vì RETURNING trả về ResultSet
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            long id              = rs.getLong("id");
            String accountNumber = rs.getString("account_number");
            OffsetDateTime created = rs.getObject("created_at", OffsetDateTime.class);
            // Đây rồi! Có ID và các giá trị generated ngay lập tức
        }
    }
}

// Cách 2: getGeneratedKeys() — Standard JDBC (không đặc trưng PostgreSQL)
String sql2 = "INSERT INTO bank_accounts (account_name, balance) VALUES (?, ?)";
try (PreparedStatement ps = conn.prepareStatement(sql2, Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, "Tên");
    ps.setBigDecimal(2, BigDecimal.ZERO);
    ps.executeUpdate();

    try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
            long id = rs.getLong(1); // Chỉ lấy được ID, không lấy được account_number
        }
    }
}
// Cách 1 (RETURNING) linh hoạt hơn nhiều — lấy được bất kỳ column nào
```

---

## 2. Common Table Expressions (CTE) — WITH Clause

### 2.1. CTE Cơ Bản — Đặt Tên Cho Subquery

```sql
-- ❌ Không dùng CTE — Khó đọc:
SELECT t.id, t.amount,
       (SELECT account_name FROM bank_accounts WHERE id = t.from_account_id) AS from_name,
       (SELECT account_name FROM bank_accounts WHERE id = t.to_account_id) AS to_name
FROM bank_transfers t
WHERE t.status = 'COMPLETED';

-- ✅ Dùng CTE — Rõ ràng, dễ debug:
WITH completed_transfers AS (
  SELECT id, from_account_id, to_account_id, amount, created_at
  FROM bank_transfers
  WHERE status = 'COMPLETED'
    AND created_at >= NOW() - INTERVAL '30 days'
),
account_names AS (
  SELECT id, account_name FROM bank_accounts
)
SELECT
  ct.id,
  ct.amount,
  src.account_name AS from_name,
  dst.account_name AS to_name,
  ct.created_at
FROM completed_transfers ct
JOIN account_names src ON src.id = ct.from_account_id
JOIN account_names dst ON dst.id = ct.to_account_id;
```

### 2.2. Recursive CTE — Cây Phân Cấp

```sql
-- Ví dụ: Tìm tất cả accounts trong cùng nhóm khách hàng (corporate tree)
WITH RECURSIVE corporate_hierarchy AS (
  -- Base case: Bắt đầu từ công ty gốc
  SELECT id, name, parent_id, 0 AS level
  FROM corporate_customers
  WHERE parent_id IS NULL

  UNION ALL

  -- Recursive: Thêm từng cấp con
  SELECT c.id, c.name, c.parent_id, h.level + 1
  FROM corporate_customers c
  JOIN corporate_hierarchy h ON c.parent_id = h.id
)
SELECT * FROM corporate_hierarchy
ORDER BY level, name;
-- Kết quả: Toàn bộ cây phân cấp với level hierarchy
```

### 2.3. Writable CTE — CTE Có Thể Ghi Data

```sql
-- Chuyển tiền nội bộ trong 1 statement phức tạp:
WITH
-- Bước 1: Trừ tiền từ account nguồn
debit_update AS (
  UPDATE bank_accounts
  SET balance = balance - 500000,
      updated_at = NOW()
  WHERE id = 'acc-from-001'
    AND balance >= 500000  -- Kiểm tra đủ tiền
  RETURNING id, balance AS new_balance
),
-- Bước 2: Chỉ chạy credit nếu debit thành công
credit_update AS (
  UPDATE bank_accounts
  SET balance = balance + 500000,
      updated_at = NOW()
  WHERE id = 'acc-to-002'
    AND EXISTS (SELECT 1 FROM debit_update)  -- Phụ thuộc debit thành công
  RETURNING id, balance AS new_balance
),
-- Bước 3: Tạo ledger entries
debit_entry AS (
  INSERT INTO ledger_entries (account_id, entry_type, amount, transfer_id)
  SELECT 'acc-from-001', 'DEBIT', 500000, 'transfer-abc'
  WHERE EXISTS (SELECT 1 FROM debit_update)
  RETURNING id
),
credit_entry AS (
  INSERT INTO ledger_entries (account_id, entry_type, amount, transfer_id)
  SELECT 'acc-to-002', 'CREDIT', 500000, 'transfer-abc'
  WHERE EXISTS (SELECT 1 FROM credit_update)
  RETURNING id
)
-- Kết quả cuối: Báo cáo trạng thái
SELECT
  (SELECT new_balance FROM debit_update)  AS from_new_balance,
  (SELECT new_balance FROM credit_update) AS to_new_balance,
  (SELECT id FROM debit_entry)            AS debit_ledger_id,
  (SELECT id FROM credit_entry)           AS credit_ledger_id;
```

---

## 3. Window Functions — Phân Tích Dữ Liệu Không Cần GROUP BY

```sql
-- Window Functions: Tính toán trên "cửa sổ" rows liên quan, KHÔNG reduce rows

-- === ROW_NUMBER — Đánh số thứ tự ===
SELECT
  id,
  account_id,
  amount,
  entry_type,
  created_at,
  ROW_NUMBER() OVER (
    PARTITION BY account_id          -- Đánh số riêng cho từng account
    ORDER BY created_at DESC         -- Giao dịch mới nhất = 1
  ) AS transaction_rank
FROM ledger_entries;
-- Account A: giao dịch mới nhất rank=1, thứ 2 rank=2...
-- Account B: giao dịch mới nhất rank=1, thứ 2 rank=2...

-- === Lấy giao dịch gần nhất của mỗi account ===
WITH ranked AS (
  SELECT *,
    ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY created_at DESC) AS rn
  FROM ledger_entries
)
SELECT * FROM ranked WHERE rn = 1;
-- 1 row per account — giao dịch gần nhất

-- === RANK vs DENSE_RANK ===
SELECT amount,
  RANK()       OVER (ORDER BY amount DESC) AS rank,       -- 1,2,2,4 (skip 3)
  DENSE_RANK() OVER (ORDER BY amount DESC) AS dense_rank  -- 1,2,2,3 (không skip)
FROM ledger_entries;

-- === SUM Running Total (số dư lũy kế) ===
SELECT
  created_at,
  entry_type,
  amount,
  SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE -amount END)
    OVER (
      PARTITION BY account_id
      ORDER BY created_at ASC
      ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW  -- Tất cả rows từ đầu đến row hiện tại
    ) AS running_balance
FROM ledger_entries
WHERE account_id = 'acc-001'
ORDER BY created_at;
-- Kết quả: Số dư tích lũy qua từng giao dịch — giống sổ ngân hàng

-- === LAG/LEAD — So sánh với row trước/sau ===
SELECT
  created_at,
  amount,
  LAG(amount,  1, 0) OVER (PARTITION BY account_id ORDER BY created_at) AS prev_amount,
  LEAD(amount, 1, 0) OVER (PARTITION BY account_id ORDER BY created_at) AS next_amount,
  amount - LAG(amount, 1, 0) OVER (PARTITION BY account_id ORDER BY created_at) AS diff
FROM ledger_entries
WHERE account_id = 'acc-001';

-- === PERCENTILE — Phân tích phân vị ===
SELECT
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY amount) AS median_amount,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY amount) AS p95_amount,
  PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY amount) AS p99_amount
FROM bank_transfers
WHERE created_at >= NOW() - INTERVAL '30 days';
-- Phân tích phân bố số tiền giao dịch trong 30 ngày
```

---

## 4. ON CONFLICT — Upsert (Insert or Update)

```sql
-- PostgreSQL có ON CONFLICT — MySQL có INSERT ... ON DUPLICATE KEY UPDATE

-- === ON CONFLICT DO NOTHING ===
INSERT INTO outbox_events (id, event_type, payload, status)
VALUES ('event-uuid-123', 'TRANSFER_COMPLETED', '{"amount": 500000}', 'PENDING')
ON CONFLICT (id) DO NOTHING;
-- Nếu event-uuid-123 đã tồn tại → Bỏ qua, không báo lỗi
-- Idempotent insert!

-- === ON CONFLICT DO UPDATE (Upsert) ===
INSERT INTO account_daily_summary (account_id, date, total_debit, total_credit, tx_count)
VALUES ('acc-001', CURRENT_DATE, 500000, 0, 1)
ON CONFLICT (account_id, date) DO UPDATE
  SET total_debit  = account_daily_summary.total_debit + EXCLUDED.total_debit,
  --                                                      ↑ EXCLUDED = giá trị TRY TO INSERT
      total_credit = account_daily_summary.total_credit + EXCLUDED.total_credit,
      tx_count     = account_daily_summary.tx_count + EXCLUDED.tx_count,
      updated_at   = NOW();
-- Nếu chưa có: INSERT. Nếu đã có: UPDATE cộng dồn
-- EXCLUDED keyword: tham chiếu đến giá trị đang cố insert

-- === ON CONFLICT (column) vs ON CONFLICT ON CONSTRAINT ===
ON CONFLICT (account_id, date)           -- Conflict trên column
ON CONFLICT ON CONSTRAINT uq_acc_date   -- Conflict trên named constraint
```

---

## 5. Subqueries Nâng Cao

```sql
-- === Correlated Subquery ===
SELECT a.id, a.account_name,
  (SELECT COUNT(*) FROM bank_transfers t
   WHERE t.from_account_id = a.id
     AND t.created_at >= NOW() - INTERVAL '24 hours') AS transfers_today
FROM bank_accounts a
WHERE a.customer_id = 'cust-001';
-- Subquery tham chiếu a.id từ outer query — "correlated"

-- === EXISTS vs IN ===
-- EXISTS: Dừng ngay khi tìm thấy 1 row (efficient)
SELECT * FROM bank_accounts a
WHERE EXISTS (
  SELECT 1 FROM bank_transfers t
  WHERE t.from_account_id = a.id
    AND t.status = 'FAILED'
    AND t.created_at >= NOW() - INTERVAL '1 hour'
);
-- IN: Phải lấy tất cả IDs rồi compare (kém hơn với large set)

-- === LATERAL JOIN — Subquery tham chiếu outer table ===
SELECT a.id, a.account_name, recent.*
FROM bank_accounts a
CROSS JOIN LATERAL (
  SELECT amount, created_at, entry_type
  FROM ledger_entries
  WHERE account_id = a.id      -- ← Tham chiếu outer table
  ORDER BY created_at DESC
  LIMIT 3                       -- 3 giao dịch gần nhất cho mỗi account
) AS recent;
-- LATERAL = subquery được phép reference outer query → Rất mạnh
```

---

## 6. Aggregate Functions Nâng Cao

```sql
-- === FILTER clause — Aggregate có điều kiện ===
SELECT
  COUNT(*) AS total,
  COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
  COUNT(*) FILTER (WHERE status = 'FAILED')    AS failed,
  COUNT(*) FILTER (WHERE status = 'PENDING')   AS pending,
  SUM(amount) FILTER (WHERE status = 'COMPLETED') AS total_completed_amount
FROM bank_transfers
WHERE created_at >= CURRENT_DATE;
-- 1 query thay vì 4 queries separate!

-- === STRING_AGG — Nối strings ===
SELECT
  customer_id,
  STRING_AGG(account_number, ', ' ORDER BY created_at) AS all_accounts
FROM bank_accounts
GROUP BY customer_id;
-- Kết quả: customer_id | all_accounts
--         cust-001    | 088880001, 088880002, 088880003

-- === ARRAY_AGG — Tạo mảng ===
SELECT
  customer_id,
  ARRAY_AGG(account_number ORDER BY created_at) AS account_numbers
FROM bank_accounts
GROUP BY customer_id;
-- Kết quả: customer_id | account_numbers
--         cust-001    | {088880001, 088880002, 088880003}

-- === JSON_AGG — Tạo JSON array ===
SELECT
  customer_id,
  JSON_AGG(
    JSON_BUILD_OBJECT('id', id, 'name', account_name, 'balance', balance)
    ORDER BY created_at
  ) AS accounts_json
FROM bank_accounts
GROUP BY customer_id;
-- Kết quả: JSON array of objects — đẹp để trả về API
```

---

## 7. Date/Time Operations

```sql
-- === Khoảng thời gian ===
SELECT NOW() - INTERVAL '30 days';       -- 30 ngày trước
SELECT NOW() + INTERVAL '1 year';        -- 1 năm sau
SELECT INTERVAL '2 hours 30 minutes';    -- Khoảng thời gian

-- === Trích xuất thành phần ===
SELECT
  EXTRACT(YEAR  FROM created_at) AS year,
  EXTRACT(MONTH FROM created_at) AS month,
  EXTRACT(DAY   FROM created_at) AS day,
  EXTRACT(HOUR  FROM created_at) AS hour,
  EXTRACT(DOW   FROM created_at) AS day_of_week  -- 0=Sunday, 6=Saturday
FROM bank_transfers;

-- === DATE_TRUNC — Làm tròn đến đơn vị ===
SELECT DATE_TRUNC('month', created_at) AS month_start  -- Ngày đầu tháng
FROM bank_transfers;
-- '2026-09-16' → '2026-09-01 00:00:00'

-- === AT TIME ZONE ===
SELECT created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS vietnam_time
FROM bank_transfers;
-- Chuyển TIMESTAMPTZ → giờ Việt Nam để hiển thị

-- === AGE — Tính khoảng cách ===
SELECT AGE(NOW(), created_at) AS account_age
FROM bank_accounts WHERE id = 'acc-001';
-- Kết quả: '1 year 3 months 15 days'

-- === Thống kê theo thời gian ===
SELECT
  DATE_TRUNC('day', created_at) AS day,
  COUNT(*) AS total_transfers,
  SUM(amount) AS total_amount
FROM bank_transfers
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY day;
-- Thống kê theo ngày trong 30 ngày gần nhất
```

---

## 8. GENERATE_SERIES — Tạo Dãy Số/Ngày

```sql
-- Tạo dãy số:
SELECT generate_series(1, 10);           -- 1,2,3,...,10
SELECT generate_series(0, 100, 5);       -- 0,5,10,...,100

-- Tạo dãy ngày — Dùng để fill gap trong báo cáo:
SELECT generate_series(
  '2026-09-01'::DATE,
  '2026-09-30'::DATE,
  INTERVAL '1 day'
) AS day;

-- Báo cáo số giao dịch theo ngày — KỂ CẢ NGÀY KHÔNG CÓ GIAO DỊCH:
WITH date_series AS (
  SELECT generate_series(
    '2026-09-01'::DATE,
    '2026-09-30'::DATE,
    INTERVAL '1 day'
  )::DATE AS day
)
SELECT
  ds.day,
  COALESCE(COUNT(t.id), 0) AS total_transfers,
  COALESCE(SUM(t.amount), 0) AS total_amount
FROM date_series ds
LEFT JOIN bank_transfers t ON DATE_TRUNC('day', t.created_at)::DATE = ds.day
GROUP BY ds.day
ORDER BY ds.day;
-- Ngày không có giao dịch vẫn có row với count=0
-- Nếu không dùng generate_series + LEFT JOIN → Ngày không có giao dịch = bị bỏ qua
```

---

## Tổng Kết — Cheat Sheet

| Feature | Syntax | BankX Use Case |
|---|---|---|
| `RETURNING` | `INSERT ... RETURNING id, account_number` | Lấy ID sau INSERT không dùng JPA |
| `ON CONFLICT DO NOTHING` | `INSERT ... ON CONFLICT (id) DO NOTHING` | Idempotent outbox insert |
| `ON CONFLICT DO UPDATE` | `... DO UPDATE SET col = EXCLUDED.col` | Upsert daily summary |
| CTE | `WITH name AS (...)` | Readable complex queries |
| Writable CTE | `WITH ins AS (INSERT ... RETURNING)` | Multi-step atomic operations |
| Window Function | `OVER (PARTITION BY ... ORDER BY ...)` | Running balance, ranking |
| `FILTER` | `COUNT(*) FILTER (WHERE ...)` | Multi-condition aggregate |
| `LATERAL` | `CROSS JOIN LATERAL (SELECT ... LIMIT 3)` | Top-N per group |
| `GENERATE_SERIES` | `generate_series(start, end, step)` | Gap-fill reports |

---

**← [Bài 01 — Đặc Tính](./01_postgresql_dac_tinh_va_so_sanh.md)** | **→ [Bài 03 — Index & Performance](./03_postgresql_index_va_performance.md)**
