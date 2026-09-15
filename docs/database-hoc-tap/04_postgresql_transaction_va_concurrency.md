# Bài 04 — PostgreSQL: Transaction & Concurrency

> **Mục tiêu:** Hiểu sâu ACID, Isolation Levels, Locking, Deadlock, SELECT FOR UPDATE  
> **Liên quan BankX:** Optimistic Lock, Outbox Polling, Concurrent Transfers

---

## 1. ACID — Nền Tảng Không Thể Thiếu Trong Banking

```
ACID = Atomicity + Consistency + Isolation + Durability

A — Atomicity (Nguyên tử):
  Transaction là "all-or-nothing"
  BankX: DEBIT + CREDIT + Ledger entries → Tất cả thành công hoặc tất cả rollback
  Nếu DEBIT thành công nhưng CREDIT fail → Rollback DEBIT
  → Không bao giờ có "tiền bị trừ nhưng chưa được cộng"

C — Consistency (Nhất quán):
  DB chuyển từ state hợp lệ này sang state hợp lệ khác
  BankX: SUM(DEBIT) = SUM(CREDIT) trong mọi transaction (Double-Entry rule)
  Constraints không bao giờ bị vi phạm

I — Isolation (Cô lập):
  Concurrent transactions không thấy intermediate state của nhau
  BankX: Transaction A đang DEBIT → Transaction B không thấy balance trung gian
  Mức độ isolation khác nhau (xem mục 3)

D — Durability (Bền vững):
  Sau khi COMMIT → Data được lưu kể cả server crash ngay sau đó
  PostgreSQL dùng WAL (Write-Ahead Log): Ghi log trước khi ghi data
  Crash → Replay WAL → Recover data
```

---

## 2. Transaction Control

```sql
-- === Basic Transaction ===
BEGIN;                          -- Bắt đầu transaction
  UPDATE bank_accounts SET balance = balance - 500000 WHERE id = 'acc-001';
  UPDATE bank_accounts SET balance = balance + 500000 WHERE id = 'acc-002';
  INSERT INTO ledger_entries (...) VALUES (...);
COMMIT;                         -- Lưu tất cả thay đổi

BEGIN;
  UPDATE bank_accounts SET balance = balance - 500000 WHERE id = 'acc-001';
  -- Phát hiện lỗi: acc-002 không tồn tại
ROLLBACK;                       -- Hủy tất cả thay đổi, balance acc-001 trở về cũ

-- === SAVEPOINT — Partial Rollback ===
BEGIN;
  INSERT INTO bank_transfers (...) VALUES (...);
  SAVEPOINT before_ledger;      -- Điểm lưu

  INSERT INTO ledger_entries (...) VALUES (...) ON CONFLICT DO NOTHING;
  -- Nếu có vấn đề gì đó:
  ROLLBACK TO SAVEPOINT before_ledger; -- Rollback về điểm lưu, không rollback toàn bộ

  -- Thử lại với logic khác...
  INSERT INTO ledger_entries (...) VALUES (...);
COMMIT;

-- === Transaction Properties ===
BEGIN ISOLATION LEVEL READ COMMITTED;     -- Default
BEGIN ISOLATION LEVEL REPEATABLE READ;   -- Snapshot isolation
BEGIN ISOLATION LEVEL SERIALIZABLE;      -- Strict serialization
BEGIN READ ONLY;                          -- Chỉ đọc, không ghi
BEGIN DEFERRABLE;                         -- Dùng với SERIALIZABLE
```

---

## 3. Isolation Levels — Rất Quan Trọng Cho Banking

```
Vấn đề với Concurrent Transactions:
  Dirty Read:       Đọc uncommitted data của transaction khác
  Non-Repeatable:   Đọc cùng row 2 lần → Kết quả khác (transaction khác update+commit giữa chừng)
  Phantom Read:     SELECT COUNT(*) → Khác nhau 2 lần (transaction khác insert rows mới)
  Serialization:    2 transactions đọc+ghi cùng data gây inconsistency dù mỗi cái đúng riêng lẻ
```

| Isolation Level | Dirty Read | Non-Repeatable | Phantom | Serialization |
|---|---|---|---|---|
| **READ UNCOMMITTED** | ❌ Có thể | ❌ Có thể | ❌ Có thể | ❌ Có thể |
| **READ COMMITTED** | ✅ Không | ❌ Có thể | ❌ Có thể | ❌ Có thể |
| **REPEATABLE READ** | ✅ Không | ✅ Không | ✅ Không* | ❌ Có thể |
| **SERIALIZABLE** | ✅ Không | ✅ Không | ✅ Không | ✅ Không |

*PostgreSQL REPEATABLE READ dùng snapshot isolation — không có Phantom Read

```sql
-- PostgreSQL KHÔNG có READ UNCOMMITTED (treat như READ COMMITTED)
-- Mặc định: READ COMMITTED

-- Demo Dirty Read (không xảy ra trong PostgreSQL):
-- Session A: BEGIN; UPDATE balance = 0; (chưa COMMIT)
-- Session B: SELECT balance; -- Vẫn thấy giá trị cũ, không thấy 0 (PostgreSQL an toàn)

-- Demo Non-Repeatable Read trong READ COMMITTED:
-- Session A: BEGIN READ COMMITTED;
--   SELECT balance; -- Thấy 5,000,000
-- Session B: UPDATE balance = 6,000,000; COMMIT;
-- Session A: SELECT balance; -- Thấy 6,000,000! (khác lần đầu → Non-Repeatable)

-- REPEATABLE READ — Snapshot Isolation:
-- Session A: BEGIN REPEATABLE READ;
--   SELECT balance; -- Snapshot tại BEGIN: 5,000,000
-- Session B: UPDATE balance = 6,000,000; COMMIT;
-- Session A: SELECT balance; -- VẪN thấy 5,000,000 (snapshot không thay đổi)
-- Session A: UPDATE balance = balance - 100;
--   → Đọc snapshot (5,000,000 - 100) nhưng ghi vào actual row (6,000,000 - 100)?
--   → PostgreSQL detect conflict → ERROR: "could not serialize access due to concurrent update"
--   → Application cần retry

-- SERIALIZABLE — Strict:
-- Phát hiện và abort transaction khi có potential serialization anomaly
-- Banking critical path: Dùng SERIALIZABLE + Retry logic
-- Trade-off: More rollbacks, more retries
```

---

## 4. Locking — Pessimistic Concurrency Control

### 4.1. Row-Level Locks

```sql
-- === SELECT FOR UPDATE — Lock rows để update ===
BEGIN;
  SELECT id, balance, version
  FROM bank_accounts
  WHERE id = 'acc-001'
  FOR UPDATE;                    -- Lock row, các transaction khác phải chờ
  -- Bây giờ có thể UPDATE an toàn:
  UPDATE bank_accounts SET balance = balance - 500000 WHERE id = 'acc-001';
COMMIT;
-- Sau COMMIT: Lock được release, transaction khác tiếp tục

-- === FOR UPDATE SKIP LOCKED — Quan trọng cho Queue/Polling ===
-- BankX OutboxPollingService dùng pattern này!
SELECT id, event_type, payload
FROM outbox_events
WHERE status = 'PENDING'
ORDER BY created_at ASC
LIMIT 100
FOR UPDATE SKIP LOCKED;        -- Bỏ qua rows đang bị lock bởi instance khác
-- → Multiple instances của OutboxPollingService có thể chạy song song
-- → Mỗi instance lấy một batch KHÁC NHAU (không trùng)
-- → SKIP LOCKED: Nếu row đang bị lock → Skip, lấy row tiếp theo

-- === FOR SHARE — Lock "share" (nhiều transactions cùng share lock) ===
SELECT * FROM bank_accounts WHERE id = 'acc-001'
FOR SHARE;
-- FOR SHARE: Nhiều transactions có thể cùng hold SHARE lock
-- Nhưng SHARE lock block UPDATE/DELETE (phải chờ)
-- Dùng khi: Đọc data mà không muốn ai UPDATE trong lúc đó

-- === FOR NO KEY UPDATE ===
-- Giống FOR UPDATE nhưng chỉ lock row, không lock referenced foreign keys
-- Cho phép concurrent INSERT vào child tables
SELECT * FROM bank_accounts WHERE id = 'acc-001'
FOR NO KEY UPDATE;
```

### 4.2. Table-Level Locks

```sql
-- === LOCK TABLE — Lock toàn bộ bảng (ít dùng) ===
LOCK TABLE bank_accounts IN SHARE ROW EXCLUSIVE MODE;
-- Chỉ dùng cho DDL operations, data migration

-- === Advisory Locks — Application-Level Locks ===
-- Lock tự define, PostgreSQL track nhưng không enforce
SELECT pg_advisory_lock(12345);      -- Lock với key 12345 (blocking)
SELECT pg_try_advisory_lock(12345);  -- Thử lock, return FALSE nếu không được (non-blocking)
SELECT pg_advisory_unlock(12345);    -- Unlock

-- BankX use case: Distributed lock cho cron job
-- Đảm bảo chỉ 1 instance chạy daily reconciliation:
BEGIN;
  SELECT pg_try_advisory_xact_lock(hashtext('daily_reconciliation'));
  -- Nếu return TRUE: Instance này giành được lock, chạy reconciliation
  -- Nếu return FALSE: Instance khác đang chạy, bỏ qua
  -- Lock tự động release khi transaction END
```

---

## 5. Deadlock — Và Cách Phòng Tránh

```
Deadlock xảy ra khi:
  Transaction A: Lock row 1 → Muốn lock row 2
  Transaction B: Lock row 2 → Muốn lock row 1
  → Cả 2 đều chờ nhau → Deadlock!

PostgreSQL tự detect và abort 1 transaction (kẻ "xui xẻo"):
  ERROR: deadlock detected
  DETAIL: Process X waits for ShareLock on transaction Y
```

```sql
-- === Deadlock scenario ===
-- Session A:
BEGIN;
UPDATE bank_accounts SET balance = balance - 100 WHERE id = 'acc-001'; -- Lock acc-001
-- Session B (cùng lúc):
BEGIN;
UPDATE bank_accounts SET balance = balance - 100 WHERE id = 'acc-002'; -- Lock acc-002
-- Session A (tiếp):
UPDATE bank_accounts SET balance = balance + 100 WHERE id = 'acc-002'; -- Chờ Session B
-- Session B (tiếp):
UPDATE bank_accounts SET balance = balance + 100 WHERE id = 'acc-001'; -- Chờ Session A
-- → DEADLOCK! PostgreSQL abort 1 transaction

-- === Phòng tránh Deadlock — Luôn lock theo thứ tự cố định ===
-- Rule: Luôn lock account ID nhỏ hơn trước
-- Session A: acc-001 < acc-002 → Lock acc-001 trước, rồi acc-002
-- Session B: acc-001 < acc-002 → Lock acc-001 trước (chờ A xong)
-- → Không deadlock vì thứ tự nhất quán

-- Trong BankX implementation:
String lockOrder[] = Stream.of(fromAccountId, toAccountId)
    .sorted()
    .toArray(String[]::new);
// Lock accounts theo thứ tự alphabet/UUID
```

---

## 6. Optimistic Locking — Pattern BankX Đang Dùng

```sql
-- BankX dùng @Version column cho Optimistic Locking

-- Schema:
ALTER TABLE bank_accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Đọc account với version:
SELECT id, balance, version FROM bank_accounts WHERE id = 'acc-001';
-- Kết quả: balance=5000000, version=7

-- Update với version check:
UPDATE bank_accounts
SET balance = balance - 500000,
    version = version + 1
WHERE id = 'acc-001'
  AND version = 7;              -- ← Optimistic lock check
-- Nếu trả về affected_rows = 1: Success (version vẫn = 7 khi ta update)
-- Nếu trả về affected_rows = 0: Conflict! (transaction khác đã update trước, version ≠ 7)
-- → Application: Reload account → Retry

RETURNING version;
-- version = 8 sau khi update thành công

-- === Spring JPA @Version — Tự động ===
-- @Version field → Spring tự thêm AND version = ? và version+1
-- ObjectOptimisticLockingFailureException khi conflict
-- @Retryable tự động retry
```

---

## 7. MVCC Deep Dive — Hidden System Columns

```sql
-- PostgreSQL có hidden columns trong mỗi row:
-- xmin: Transaction ID của transaction tạo row này
-- xmax: Transaction ID của transaction xóa/update row này (0 nếu chưa ai)
-- ctid:  Physical location của row trong heap file (page, offset)

SELECT xmin, xmax, ctid, id, balance
FROM bank_accounts
WHERE id = 'acc-001';
-- xmin=12345 xmax=0 ctid=(0,1) → Row do transaction 12345 tạo, chưa bị update

-- Sau UPDATE:
UPDATE bank_accounts SET balance = 5500000 WHERE id = 'acc-001';

-- Old row: xmin=12345 xmax=12346 → Dead row (transaction 12346 đã update)
-- New row: xmin=12346 xmax=0     → Live row với balance mới

SELECT xmin, xmax, ctid, balance FROM bank_accounts WHERE id = 'acc-001';
-- Chỉ thấy: xmin=12346 xmax=0 ctid=(0,5) balance=5500000
-- (Old row vẫn tồn tại vật lý nhưng PostgreSQL filter ra)

-- VACUUM dọn old rows (xmax != 0 và transaction đã committed):
VACUUM bank_accounts;
-- Sau vacuum: Only live rows remain
```

---

## 8. WAL — Write-Ahead Log (Durability)

```
WAL = Cơ chế đảm bảo Durability trong ACID

Cách hoạt động:
  1. Transaction COMMIT
  2. PostgreSQL ghi log (WAL record) vào WAL file TRƯỚC
  3. Confirm COMMIT cho client
  4. Background: Flush actual data pages từ memory → disk

Nếu crash sau COMMIT:
  1. Server restart
  2. PostgreSQL replay WAL records
  3. Data được recover hoàn toàn

PostgreSQL WAL files nằm ở: $PGDATA/pg_wal/
```

```sql
-- WAL-related settings:
-- wal_level = replica      → Đủ cho replication
-- wal_level = logical      → Cho logical replication (Debezium CDC)
-- synchronous_commit = on  → Đảm bảo WAL flush trước khi COMMIT confirm
-- fsync = on               → Đảm bảo WAL ghi thật xuống disk (không chỉ OS buffer)

-- Xem WAL stats:
SELECT * FROM pg_stat_wal;
-- wal_records: Số WAL records được generate
-- wal_fpi: Full Page Images (khi checkpoint)
-- wal_bytes: Tổng bytes WAL đã ghi
```

---

## 9. Read Replicas — Horizontal Read Scaling

```
Primary (Master)   ←→  Streaming Replication  →  Replica 1 (Read-only)
                                               →  Replica 2 (Read-only)
                                               →  Replica 3 (Read-only)

Write path: Primary
Read path: Replica (report queries, analytics, CQRS read model)

Replication Lag:
  Streaming Replication: ~milliseconds (gần như realtime)
  Có thể đọc "slightly stale" data trên replica

BankX Use Case:
  Primary: INSERT/UPDATE bank_transfers, ledger_entries
  Replica: SELECT transaction history, analytics, reporting
```

```yaml
# Spring Boot - Route reads to replica:
spring:
  datasource:
    url: jdbc:postgresql://primary:5432/bankx
  datasource-replica:
    url: jdbc:postgresql://replica:5432/bankx

# Custom: @ReadOnly queries → Replica datasource
# @Transactional(readOnly = true) → Route to replica
```

---

## Tổng Kết

| Concept | PostgreSQL | BankX Use Case |
|---|---|---|
| `BEGIN/COMMIT/ROLLBACK` | Transaction control | Mọi transfer |
| READ COMMITTED | Default isolation | Hầu hết queries |
| REPEATABLE READ | Snapshot isolation | Consistent reads |
| `FOR UPDATE` | Pessimistic lock row | (Ít dùng, prefer Optimistic) |
| `FOR UPDATE SKIP LOCKED` | Non-blocking queue | OutboxPollingService |
| `@Version` + Optimistic Lock | No blocking lock | Transfer balance update |
| MVCC | Readers/Writers không block nhau | High concurrency |
| WAL | Durability after crash | Compliance requirement |
| Read Replica | Scale reads | CQRS read model |
| Deadlock prevention | Lock in consistent order | Multi-account transfers |

---

**← [Bài 03 — Index & Performance](./03_postgresql_index_va_performance.md)** | **→ [Bài 05 — Redis Data Structures](./05_redis_dac_tinh_va_data_structures.md)**
