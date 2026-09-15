# Bài 07 — Phỏng Vấn Database: PostgreSQL & Redis Q&A

> **Format:** Câu hỏi thực tế → Tại sao KHÔNG → Tại sao NÊN  
> **Bao gồm:** 45+ câu hỏi từ cơ bản đến Senior level

---

## 📋 Mục Lục

**PostgreSQL:**
1. [RETURNING clause](#1-returning-clause)
2. [SERIAL vs BIGSERIAL vs UUID](#2-serial-vs-bigserial-vs-uuid-làm-primary-key)
3. [Index design](#3-index-design-không-đúng)
4. [EXPLAIN ANALYZE](#4-explain-analyze)
5. [ACID trong banking](#5-acid-trong-banking)
6. [Isolation Levels](#6-isolation-levels)
7. [Optimistic vs Pessimistic Lock](#7-optimistic-vs-pessimistic-lock)
8. [Deadlock](#8-deadlock)
9. [MVCC](#9-mvcc)
10. [N+1 Problem](#10-n1-query-problem)
11. [PostgreSQL vs MySQL vs MongoDB](#11-postgresql-vs-mysql-vs-mongodb)
12. [Partitioning](#12-table-partitioning)
13. [Flyway Migration](#13-flyway-migration)

**Redis:**
14. [Redis vs Memcached](#14-redis-vs-memcached)
15. [Redis Data Structures](#15-chọn-data-structure-redis-nào)
16. [Cache Stampede](#16-cache-stampede)
17. [Cache Invalidation](#17-cache-invalidation)
18. [Distributed Lock](#18-distributed-lock-với-redis)
19. [OTP Anti-Brute-Force](#19-otp-anti-brute-force)
20. [Rate Limiting Algorithms](#20-rate-limiting-algorithms)
21. [Redis Persistence](#21-redis-persistence)
22. [Redis Memory Full](#22-redis-memory-đầy)

**SQL Nâng Cao:**
23. [Window Functions](#23-window-functions)
24. [CTE vs Subquery](#24-cte-vs-subquery)
25. [JOIN Types](#25-join-types)
26. [Transactions Raw JDBC](#26-transaction-trong-raw-jdbc-không-dùng-jpa)

---

## 1. RETURNING Clause

### ❓ Không dùng JPA/Hibernate, sau INSERT vào bảng có BIGSERIAL id, làm sao lấy được id đó?

**Tại sao KHÔNG dùng `LAST_INSERT_ID()` như MySQL:**
- PostgreSQL không có `LAST_INSERT_ID()`. Đây là MySQL-specific.
- Gọi sequence riêng (`currval('table_id_seq')`) sau INSERT là nguy hiểm trong concurrent environment — sequence có thể đã tăng bởi transaction khác.

**Tại sao NÊN dùng `RETURNING`:**
```sql
-- PostgreSQL RETURNING — Trả về values ngay sau INSERT trong cùng statement:
INSERT INTO bank_accounts (account_name, balance, customer_id)
VALUES ('Tài khoản thanh toán', 0, 'cust-001')
RETURNING id, account_number, created_at;
-- → id = 42, account_number = "088880042", created_at = "2026-09-16..."
-- Atomic! Không có race condition, không cần query thêm
```

```java
// JDBC thuần:
String sql = "INSERT INTO bank_accounts(account_name) VALUES(?) RETURNING id, account_number";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, "Tài khoản thanh toán");
ResultSet rs = ps.executeQuery(); // executeQuery vì RETURNING trả về ResultSet
if (rs.next()) {
  long id = rs.getLong("id");
  String accNum = rs.getString("account_number");
}
```

**RETURNING cũng dùng được với UPDATE và DELETE:**
```sql
UPDATE bank_accounts SET balance = balance - 500000 WHERE id = 'acc-001'
RETURNING id, balance, version;
-- Lấy balance MỚI sau UPDATE (atomic, không sợ concurrent change)

DELETE FROM outbox_events WHERE status = 'SENT' AND created_at < NOW() - INTERVAL '7 days'
RETURNING id, event_type; -- Lấy danh sách đã xóa để log
```

---

## 2. SERIAL vs BIGSERIAL vs UUID làm Primary Key

### ❓ Dùng SERIAL, BIGSERIAL, hay UUID cho Primary Key trong banking?

**SERIAL / BIGSERIAL:**
```sql
id SERIAL       -- 4 bytes, 1 đến 2,147,483,647 (~2 tỷ)
id BIGSERIAL    -- 8 bytes, 1 đến 9,223,372,036,854,775,807 (~9 tỷ tỷ)
-- Sequential: 1, 2, 3, ... → Predictable, tốt cho B-Tree index
```

**UUID:**
```sql
id UUID DEFAULT gen_random_uuid()
-- ví dụ: '550e8400-e29b-41d4-a716-446655440000'
-- Random → Không predictable (security tốt hơn)
-- Không sequential → Fragmented B-Tree index (kém hơn BIGSERIAL cho write-heavy)
```

**Khi nào dùng gì:**
- `BIGSERIAL`: ID nội bộ, không expose ra ngoài — `ledger_entries.id`, `outbox_events.id` — Sequential insert performance tốt
- `UUID`: ID expose qua API, cần unpredictable — `bank_transfers.id`, `customer_id`, `account_id` — User không thể đoán ID của người khác → Security

**BankX sử dụng cả hai:**
```sql
-- UUID cho entities expose qua API:
id UUID DEFAULT gen_random_uuid() PRIMARY KEY  -- bank_transfers, bank_accounts, customers

-- BIGSERIAL cho internal append-only tables:
id BIGSERIAL PRIMARY KEY  -- ledger_entries, outbox_events, audit_logs
```

---

## 3. Index Design Không Đúng

### ❓ Đã có index nhưng query vẫn chậm — các nguyên nhân phổ biến?

**Nguyên nhân 1 — Function wrap:**
```sql
-- ❌ Index vô hiệu vì có EXTRACT():
WHERE EXTRACT(YEAR FROM created_at) = 2026
-- ✅ Rewrite để dùng index:
WHERE created_at >= '2026-01-01' AND created_at < '2027-01-01'
```

**Nguyên nhân 2 — Leading column không match:**
```sql
-- Index: (from_account_id, status, created_at)
-- ❌ Query không prefix: WHERE status = 'PENDING' → Seq Scan
-- ✅ Query dùng prefix: WHERE from_account_id = ? AND status = ?
```

**Nguyên nhân 3 — Statistics cũ:**
```sql
ANALYZE bank_transfers; -- Cập nhật statistics sau khi load data lớn
```

**Nguyên nhân 4 — Cardinality quá thấp:**
```sql
-- WHERE status = 'COMPLETED' với 90% rows là COMPLETED → Seq Scan tốt hơn Index Scan
-- Giải pháp: Partial Index chỉ cho minority (PENDING)
CREATE INDEX idx_pending ONLY ON bank_transfers(created_at) WHERE status = 'PENDING';
```

**Nguyên nhân 5 — Type mismatch:**
```sql
-- Column: account_id VARCHAR
-- Query: WHERE account_id = 12345 (số nguyên, không phải string)
-- → PostgreSQL implicit cast → Index không được dùng
-- ✅ WHERE account_id = '12345' (string đúng type)
```

---

## 4. EXPLAIN ANALYZE

### ❓ Đọc output EXPLAIN ANALYZE như thế nào? Seq Scan hay Index Scan tốt hơn?

```
EXPLAIN ANALYZE SELECT ... → Thực sự chạy query + trả về execution plan

Nodes quan trọng:
  "Seq Scan"         → Đọc toàn bộ table (thường là vấn đề)
  "Index Scan"       → Dùng index, fetch heap page random I/O
  "Index Only Scan"  → Chỉ đọc index (không cần fetch table) → Tốt nhất!
  "Bitmap Heap Scan" → Dùng index, batch fetch heap pages
  "Hash Join"        → Join bằng hash table
  "Nested Loop"      → Loop outer × inner (tốt khi inner nhỏ)

actual time=X..Y → X: startup time, Y: total time (milliseconds)
rows=N → Số rows thực tế trả về (compare với estimate để detect plan issues)
loops=N → Node này chạy N lần (nested loops)

"Seq Scan" không phải luôn là tệ:
  - Bảng nhỏ (<1000 rows) → Seq Scan nhanh hơn Index Scan (index overhead)
  - Lấy >30% rows → Seq Scan thường tốt hơn (random I/O overhead của Index Scan)
  
"Index Scan" tốt khi:
  - Bảng lớn, lấy <5% rows
  - Có selective WHERE condition
```

---

## 5. ACID Trong Banking

### ❓ Giải thích ACID và tại sao banking không thể thiếu nó?

**A — Atomicity:**
- Chuyển tiền từ A sang B: `DEBIT A` + `CREDIT B` là 1 unit.
- Nếu DEBIT thành công, CREDIT fail → Rollback DEBIT → Không bao giờ có "tiền bay hơi".

**C — Consistency:**
- BankX business rule: `SUM(DEBIT) == SUM(CREDIT)` trong 1 transaction.
- Database constraints: Balance không thể âm, account_number phải unique.
- Nếu constraint bị vi phạm → Transaction rollback tự động.

**I — Isolation:**
- Transaction A đang chuyển tiền, Transaction B check balance → B không thấy "in-progress" balance.
- Phòng tránh Dirty Read, Non-Repeatable Read, Phantom Read (tùy isolation level).

**D — Durability:**
- Sau COMMIT → Data ghi vào WAL (Write-Ahead Log) trước khi confirm.
- Server crash ngay sau COMMIT → Restart → PostgreSQL replay WAL → Data recover hoàn toàn.
- Không có "tiền biến mất do server crash sau khi giao dịch thành công".

---

## 6. Isolation Levels

### ❓ Khi nào dùng SERIALIZABLE thay vì READ COMMITTED?

**READ COMMITTED (default):**
- Mỗi statement thấy committed data tại thời điểm statement chạy.
- Nhanh nhất, phù hợp hầu hết queries.
- Không phòng được: Non-Repeatable Read, Phantom Read.

**REPEATABLE READ:**
- Snapshot cố định từ đầu transaction đến cuối.
- Không có Non-Repeatable Read, Phantom Read (PostgreSQL).
- Conflict → `ObjectOptimisticLockingFailureException` → Application retry.

**SERIALIZABLE:**
- Chặt chẽ nhất, như chạy transactions tuần tự.
- Phát hiện và abort khi có serialization anomaly.
- Dùng cho: Balance sheet reconciliation, complex financial calculations.

**BankX sử dụng:**
- Transfer thông thường → READ COMMITTED + Optimistic Lock (`@Version`).
- Daily reconciliation → SERIALIZABLE (đảm bảo tính toán nhất quán 100%).

---

## 7. Optimistic vs Pessimistic Lock

### ❓ Hai người cùng chuyển tiền từ một tài khoản đồng thời — chọn Optimistic hay Pessimistic?

**Pessimistic Lock (`SELECT FOR UPDATE`):**
```sql
SELECT balance FROM bank_accounts WHERE id = 'acc-001' FOR UPDATE;
-- Lock row đến khi COMMIT/ROLLBACK → Các transaction khác chờ
```
- Vấn đề: Với 100 concurrent requests → 99 chờ → Thread pool cạn → Timeout → System unresponsive.
- Dùng khi: Collision rate rất cao, retry quá tốn kém (ví dụ bulk operations).

**Optimistic Lock (`@Version`):**
```sql
UPDATE bank_accounts SET balance = balance - 500000, version = version + 1
WHERE id = 'acc-001' AND version = 7;
-- Affected rows = 0 → Conflict → Application retry
```
- Không block: Nhiều transactions đọc cùng lúc, chỉ 1 ghi thành công, còn lại retry.
- Phù hợp khi collision rate thấp (hầu hết banking operations).
- BankX: `@Retryable(maxAttempts=3)` tự động retry.

---

## 8. Deadlock

### ❓ Deadlock xảy ra thế nào? Phòng tránh như thế nào?

**Deadlock scenario:**
```
Transaction A: Lock acc-001 → Muốn lock acc-002
Transaction B: Lock acc-002 → Muốn lock acc-001
→ Cả 2 chờ nhau mãi mãi
→ PostgreSQL detect sau timeout → Abort 1 transaction
```

**Phòng tránh — Lock theo thứ tự cố định:**
```java
// Luôn lock account ID nhỏ hơn trước (theo UUID sort):
List<String> accountIds = Arrays.asList(fromAccountId, toAccountId);
Collections.sort(accountIds); // Sort để thứ tự nhất quán
// → Mọi transactions đều lock theo cùng thứ tự → Không deadlock
```

**Detect Deadlock trong PostgreSQL:**
```sql
SELECT *
FROM pg_stat_activity
WHERE wait_event_type = 'Lock'
  AND query_start < NOW() - INTERVAL '30 seconds';
-- Xem queries đang chờ lock quá 30 giây
```

---

## 9. MVCC

### ❓ MVCC là gì? Tại sao PostgreSQL không cần lock khi đọc?

**MVCC = Multi-Version Concurrency Control:**
- Mỗi row có nhiều versions với Transaction ID.
- Reader thấy snapshot tại thời điểm transaction bắt đầu.
- Writer tạo version MỚI thay vì overwrite.
- → Reader và Writer không block nhau!

**Hệ quả:**
- Long-running transactions giữ old versions → Bảng phình to → Cần VACUUM dọn dẹp.
- VACUUM thường xuyên → Performance ổn định.

**Dead tuples (dead rows):**
```sql
-- Sau UPDATE: Row cũ trở thành "dead tuple"
-- MVCC giữ chúng cho transactions cũ đang xem
-- VACUUM dọn khi không còn transaction nào cần xem chúng
VACUUM ANALYZE bank_transfers;
```

---

## 10. N+1 Query Problem

### ❓ N+1 Problem là gì? Gặp trong JPA thế nào? Fix thế nào?

**Vấn đề:**
```java
List<BankAccount> accounts = accountRepo.findAll(); // Query 1
for (BankAccount acc : accounts) {
  Customer c = acc.getCustomer(); // Lazy load → 1 query per account
  // 100 accounts = 101 queries!
}
```

**Fix:**
```java
// JOIN FETCH:
@Query("SELECT a FROM BankAccount a JOIN FETCH a.customer")
List<BankAccount> findAllWithCustomer();

// @EntityGraph:
@EntityGraph(attributePaths = {"customer"})
List<BankAccount> findAll();

// @BatchSize: Batch load thay vì N queries
@BatchSize(size = 50)
@OneToMany(fetch = FetchType.LAZY)
private List<LedgerEntry> entries;
```

**Detect:** Bật `spring.jpa.show-sql=true` và đếm queries. Tool: `datasource-proxy`.

---

## 11. PostgreSQL vs MySQL vs MongoDB

### ❓ Tại sao BankX chọn PostgreSQL mà không phải MySQL hay MongoDB?

**vs MySQL:**
- `RETURNING`: PostgreSQL có, MySQL không — Quan trọng khi không dùng JPA.
- Partial Index: PostgreSQL có, MySQL không — Tối ưu index cho banking queries.
- JSONB indexable: PostgreSQL có, MySQL JSON kém hơn.
- License: PostgreSQL hoàn toàn free, MySQL enterprise cần license Oracle.
- Window Functions: Cả hai đều có (MySQL 8.0+), nhưng PostgreSQL mature hơn.

**vs MongoDB:**
- Banking cần ACID đầy đủ → PostgreSQL wins.
- Multi-table transactions: PostgreSQL native, MongoDB chỉ từ 4.0 và kém mature hơn.
- JOIN: PostgreSQL native SQL, MongoDB `$lookup` phức tạp và kém hiệu quả.
- Double-entry bookkeeping: Relational model phù hợp tự nhiên.

---

## 12. Table Partitioning

### ❓ Khi nào dùng Table Partitioning? Cách implement?

**Khi nào cần:**
- Bảng > 100 triệu rows.
- Query thường chỉ truy cập range thời gian gần đây.
- Cần drop data cũ nhanh (DROP PARTITION nhanh hơn DELETE hàng triệu rows rất nhiều).

**Range Partitioning theo tháng:**
```sql
CREATE TABLE bank_transfers (...) PARTITION BY RANGE (created_at);
CREATE TABLE bank_transfers_2026_09
  PARTITION OF bank_transfers
  FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
```

**Partition Pruning:**
```sql
WHERE created_at >= '2026-09-01' -- Chỉ scan partition tháng 9, không scan tháng khác
```

**Xóa data cũ:**
```sql
DROP TABLE bank_transfers_2024_01; -- Instant! (không cần DELETE + VACUUM)
```

---

## 13. Flyway Migration

### ❓ Tại sao dùng Flyway thay vì `hibernate.ddl-auto=update`?

**`ddl-auto=update` nguy hiểm:**
- Hibernate tự ALTER TABLE → Không kiểm soát, không rollback.
- Có thể mất data nếu rename column.
- Không reproducible: Môi trường dev và prod có thể khác nhau.

**Flyway advantages:**
```sql
-- V1__init_schema.sql → V2__add_version_column.sql → V3__create_indexes.sql
-- Mỗi thay đổi là 1 file, version control, reproducible
-- flyway_schema_history track lịch sử đã apply migration nào
-- validate mode: Chỉ validate schema, không sửa
```

**Zero-Downtime Migration — Expand-Contract:**
1. Deploy 1: ADD COLUMN mới (nullable/default) → Backward compatible.
2. Deploy 2: Code viết vào column mới + backfill data cũ.
3. Deploy 3: DROP column cũ (sau khi ổn định).

---

## 14. Redis vs Memcached

### ❓ Khi nào chọn Redis, khi nào chọn Memcached?

**Luôn chọn Redis nếu cần:**
- Data structures phong phú (Hash, List, Set, ZSet).
- Persistence (RDB, AOF) → Restart không mất data.
- Pub/Sub → Real-time messaging.
- Lua scripting → Atomic complex operations.
- Distributed locks → Atomic SETNX.
- Transactions (MULTI/EXEC).

**Memcached chỉ khi:**
- Chỉ cần simple key-value caching.
- Đã dùng Memcached, không muốn migrate.
- Team quen Memcached hơn.

**Kết luận:** Dùng Redis mặc định. Memcached ít lý do để chọn trong dự án mới.

---

## 15. Chọn Data Structure Redis Nào?

### ❓ Có nhiều data structures — khi nào dùng loại nào?

| Dùng Gì | Data Structure | Ví Dụ |
|---|---|---|
| Lưu value đơn giản, counter, flag | **String** | `otp:LOGIN:phone`, `balance:acc`, INCR counter |
| Lưu object nhiều fields | **Hash** | Session data, user profile, account info |
| Queue FIFO hoặc Stack LIFO | **List** | Transfer queue, BLPOP worker |
| Tập hợp unique, check membership | **Set** | Blocked accounts, processed events |
| Ranking, Rate Limiting (sliding window) | **Sorted Set** | Leaderboard, rate limit per minute |
| Event log, audit trail | **Stream** | Fraud events, audit log |
| Real-time notifications | **Pub/Sub** | Alert to Engineering Portal |

---

## 16. Cache Stampede

### ❓ Cache Stampede là gì? Giải quyết thế nào?

**Vấn đề:**
- 1000 requests cùng lúc cache miss → 1000 DB queries → DB overload.

**Giải pháp 1 — Mutex Lock:**
```java
Boolean locked = redis.opsForValue().setIfAbsent("lock:" + key, "1", Duration.ofSeconds(5));
if (locked) {
  // Query DB + cache
} else {
  Thread.sleep(100); // Chờ lock holder populate cache
  return getFromCache(key); // Retry
}
```

**Giải pháp 2 — Probabilistic Early Expiration:**
- Tính xác suất recompute TRƯỚC khi cache thực sự hết hạn.
- Ít requests hơn nhưng đảm bảo cache không expired bất ngờ.

**Giải pháp 3 — L1 + L2 Cache:**
- L1: Caffeine in-process, TTL 5 giây → Absorb burst.
- L2: Redis, TTL 30 giây.

---

## 17. Cache Invalidation

### ❓ Cache Invalidation khó như thế nào? BankX giải quyết ra sao?

> *"There are only two hard things in Computer Science: cache invalidation and naming things."* — Phil Karlton

**Strategies:**

**TTL-based (BankX chủ yếu dùng):**
- Balance cache: TTL 30 giây → Automatically stale sau 30 giây.
- Sau transfer: `DEL account_balance:{accNum}` → Invalidate ngay.

**Event-driven invalidation:**
- Kafka event `TRANSFER_COMPLETED` → Consumer xóa cache.
- Đảm bảo consistency dù có nhiều app instances.

**Write-through:**
- Update DB và cache cùng lúc → Luôn fresh.
- Vấn đề: DB write thành công, Redis write fail → Inconsistency.
- Fix: Transactions (Redis không support cross-store transactions).

**BankX pattern: TTL + Evict on write:**
```java
// Sau transfer thành công:
redis.delete("account_balance:" + fromAccount); // Evict ngay
redis.delete("account_balance:" + toAccount);   // Evict ngay
// Lần đọc tiếp theo → Cache miss → Query DB fresh
```

---

## 18. Distributed Lock Với Redis

### ❓ Tại sao phải dùng Lua script khi release distributed lock?

**Vấn đề nếu chỉ dùng DEL:**
```
1. Instance A: Acquire lock (SET lock:transfer NX EX 30)
2. Instance A: Xử lý... quá lâu → TTL hết → Lock tự release
3. Instance B: Acquire lock thành công
4. Instance A: DEL lock:transfer → Xóa nhầm lock của B!
5. Instance C: Acquire lock → 2 instances cùng xử lý!
```

**Giải pháp — Lua script atomic check-and-delete:**
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then  -- Kiểm tra value là của mình
  return redis.call('del', KEYS[1])              -- Xóa mới xóa
else
  return 0                                       -- Không xóa nếu không phải của mình
end
```
- Instance A lưu lockValue = UUID riêng.
- Khi release: Chỉ xóa nếu lockValue khớp → An toàn.

---

## 19. OTP Anti-Brute-Force

### ❓ OTP 6 số có 1 triệu tổ hợp — làm sao tránh brute force?

**Giải pháp multi-layer:**
1. **TTL ngắn:** OTP hết hạn sau 120 giây → Attacker phải đoán 1.000.000 trong 120 giây.
2. **Max attempts:** Sau 3 lần sai → Block 5 phút → Tối đa 3 lần/120 giây per phone.
3. **Rate limit:** Tối đa 3 OTP requests/5 phút per phone → Không thể request OTP liên tục.
4. **Redis atomic counter:** `INCR otp_attempts:{phone}` → Atomic, không race condition.
5. **IP-based rate limit:** 10 OTP requests/giờ per IP → Bảo vệ ngay cả khi attacker có nhiều SIMs.

```
Tính toán: 3 attempts / 120 giây → 1.5 attempts/phút per phone
           Attacker cần: 1.000.000 / 3 = 333.333 lần bị block
           Thời gian cần: 333.333 × 5 phút = 1.666.665 phút ≈ 1.157 ngày
           → Brute force thực tế bất khả thi
```

---

## 20. Rate Limiting Algorithms

### ❓ So sánh Fixed Window, Sliding Window, Token Bucket?

**Fixed Window:**
- Đơn giản. Vấn đề: 100 req cuối phút + 100 req đầu phút kế = 200 req/2s.

**Sliding Window Log (ZSet):**
- Chính xác nhất. Tốn memory (lưu mọi timestamp).
- Phù hợp: Critical endpoints, strict rate limiting.

**Sliding Window Counter:**
- Lai giữa Fixed và Log. Ít memory hơn Log, chính xác hơn Fixed.

**Token Bucket:**
- Cho phép burst (dùng tokens tích lũy). Rate trung bình bị giới hạn.
- Phù hợp: API consumers được burst thỉnh thoảng.

**Leaky Bucket:**
- Fixed output rate bất kể input. Không cho burst.
- Phù hợp: Smooth traffic, real-time systems.

**BankX sử dụng:**
- Transfer endpoints: Sliding Window ZSet (chính xác).
- Auth endpoints: Fixed Window Counter (đơn giản, đủ dùng).
- External bank API calls: Token Bucket (Resilience4j — burst khi cần).

---

## 21. Redis Persistence

### ❓ RDB vs AOF — Chọn gì cho banking cache?

**RDB (Snapshot):**
- Snapshot định kỳ → Mất data từ lần snapshot cuối → Không phù hợp cho critical data.
- Restart nhanh (load 1 file).

**AOF (Append-Only File):**
- Ghi mọi command → Ít mất data hơn.
- `appendfsync everysec` → Mất tối đa 1 giây.
- Restart chậm hơn (replay tất cả commands).

**BankX:**
- AOF + everysec cho session, OTP, rate limit.
- Lý do: Mất tối đa 1 giây là chấp nhận được. OTP hết hạn thì resend.
- Quan trọng: **Không lưu data critical chỉ trong Redis.** PostgreSQL là source of truth.

---

## 22. Redis Memory Đầy

### ❓ Redis hết memory thì sao? Cách phòng tránh?

**maxmemory-policy quan trọng nhất:**
- `noeviction`: Từ chối mọi write → App lỗi → Tệ nhất.
- `volatile-lru`: Xóa LRU keys có TTL → BankX dùng cái này.
- `allkeys-lru`: Xóa bất kỳ LRU key nào.

**BankX config:**
```
maxmemory 4gb
maxmemory-policy volatile-lru
```
- Sessions (không có TTL) → Không bị evict.
- Balance cache, OTP (có TTL) → Evict theo LRU.

**Monitoring:**
```bash
redis-cli info memory | grep used_memory_human
redis-cli info stats | grep evicted_keys  # Số keys đã bị evict
```

**Nếu eviction rate cao:** Tăng memory, add Redis nodes (Redis Cluster), hoặc giảm TTL cache.

---

## 23. Window Functions

### ❓ Window Functions dùng khi nào? Khác GROUP BY thế nào?

**GROUP BY:** Reduce nhiều rows thành 1 row per group.

**Window Functions:** Tính toán trên "cửa sổ" rows liên quan NHƯNG không reduce.

```sql
-- GROUP BY (reduce to 1 row per account):
SELECT account_id, SUM(amount) as total
FROM ledger_entries GROUP BY account_id;

-- Window Function (giữ tất cả rows, thêm running total):
SELECT account_id, amount, entry_type,
  SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE -amount END)
    OVER (PARTITION BY account_id ORDER BY created_at) AS running_balance
FROM ledger_entries;
-- Mỗi row có running_balance riêng → Giống sổ ngân hàng
```

**Các window functions quan trọng:**
- `ROW_NUMBER()` → Đánh số thứ tự.
- `RANK()` → Xếp hạng (có gap nếu tie).
- `LAG(col, n)` / `LEAD(col, n)` → Giá trị row trước/sau.
- `SUM() OVER (...)` → Cumulative sum.
- `FIRST_VALUE()` / `LAST_VALUE()` → Giá trị đầu/cuối trong window.

---

## 24. CTE vs Subquery

### ❓ Khi nào dùng CTE (`WITH`), khi nào dùng Subquery?

**Dùng CTE khi:**
- Logic phức tạp, cần đặt tên để dễ đọc.
- Cùng subquery dùng nhiều lần trong 1 query.
- Recursive query (cây phân cấp).

**Dùng Subquery khi:**
- Logic đơn giản, không cần đặt tên.
- Correlated subquery (tham chiếu outer query).

**Performance:** Trong PostgreSQL modern, CTE và Subquery thường có cùng performance sau query optimizer. Ưu tiên CTE vì readable hơn.

---

## 25. JOIN Types

### ❓ INNER JOIN vs LEFT JOIN vs CROSS JOIN — Khi nào dùng?

```sql
-- INNER JOIN: Chỉ rows có match ở CẢ HAI bảng
SELECT a.account_name, c.full_name
FROM bank_accounts a
INNER JOIN customers c ON c.id = a.customer_id;
-- Bỏ accounts không có customer record

-- LEFT JOIN: Tất cả rows bảng trái + match (hoặc NULL) từ bảng phải
SELECT a.account_name, t.amount
FROM bank_accounts a
LEFT JOIN bank_transfers t ON t.from_account_id = a.id;
-- Giữ accounts dù chưa có transfer nào (t.amount = NULL)

-- RIGHT JOIN: Ngược LEFT JOIN (ít dùng, thay bằng swap bảng + LEFT JOIN)

-- FULL OUTER JOIN: Tất cả rows từ cả 2 bảng
-- Dùng cho reconciliation

-- CROSS JOIN: Cartesian product (mọi row × mọi row)
-- Dùng cho generate combinations hoặc với LATERAL
```

---

## 26. Transaction Trong Raw JDBC (Không Dùng JPA)

### ❓ Manage transaction thủ công trong JDBC thế nào?

```java
Connection conn = dataSource.getConnection();
try {
  conn.setAutoCommit(false); // Bắt đầu transaction thủ công

  // Debit:
  PreparedStatement debit = conn.prepareStatement(
      "UPDATE bank_accounts SET balance = balance - ? WHERE id = ? AND balance >= ?"
  );
  debit.setBigDecimal(1, amount);
  debit.setObject(2, fromAccountId);
  debit.setBigDecimal(3, amount);
  int rows = debit.executeUpdate();

  if (rows == 0) {
    conn.rollback();
    throw new InsufficientFundsException("Số dư không đủ");
  }

  // Credit với RETURNING:
  PreparedStatement credit = conn.prepareStatement(
      "UPDATE bank_accounts SET balance = balance + ? WHERE id = ? RETURNING balance"
  );
  credit.setBigDecimal(1, amount);
  credit.setObject(2, toAccountId);
  ResultSet rs = credit.executeQuery();

  BigDecimal newToBalance = rs.next() ? rs.getBigDecimal("balance") : null;

  // Insert ledger entries:
  // ...

  conn.commit(); // Tất cả thành công → Commit

} catch (Exception e) {
  conn.rollback(); // Bất kỳ lỗi nào → Rollback tất cả
  throw e;
} finally {
  conn.setAutoCommit(true);
  conn.close(); // Trả về pool
}
```

---

*Tài liệu Database Q&A — Titan BankX Platform. Cập nhật tháng 9/2026.*
