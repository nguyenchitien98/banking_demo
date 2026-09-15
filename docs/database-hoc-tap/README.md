# 📚 Database Mastery — PostgreSQL & Redis

> Học chuyên sâu PostgreSQL và Redis từ nền tảng đến Senior level  
> Lấy ví dụ từ project **Titan BankX** thực tế  
> Format: Giải thích rõ ràng + Code thực tế + Câu hỏi phỏng vấn

---

## 📁 Danh Sách File

| # | File | Nội Dung |
|---|---|---|
| **01** | [PostgreSQL — Đặc Tính & So Sánh](./01_postgresql_dac_tinh_va_so_sanh.md) | ACID, MVCC, Types, Extensions — PostgreSQL khác gì MySQL, MongoDB |
| **02** | [PostgreSQL — SQL Nâng Cao](./02_postgresql_sql_nang_cao.md) | `RETURNING`, CTE, Window Functions, JSON, `ON CONFLICT` |
| **03** | [PostgreSQL — Index & Performance](./03_postgresql_index_va_performance.md) | B-Tree, Partial, Composite, GIN, `EXPLAIN ANALYZE`, Partitioning |
| **04** | [PostgreSQL — Transaction & Concurrency](./04_postgresql_transaction_va_concurrency.md) | ACID, Isolation Levels, MVCC, Locking, Deadlock |
| **05** | [Redis — Đặc Tính & Data Structures](./05_redis_dac_tinh_va_data_structures.md) | String, Hash, List, Set, Sorted Set, Pub/Sub, Streams |
| **06** | [Redis — Patterns & Use Cases](./06_redis_patterns_va_use_cases.md) | Caching, Session, Rate Limiting, Distributed Lock, Queue |
| **07** | [Phỏng Vấn Database — Q&A](./07_phong_van_database_qa.md) | 40+ câu hỏi phỏng vấn thực tế PostgreSQL + Redis + SQL |

---

## 🎯 Vấn Đề Thực Tế Dẫn Đến Bộ Tài Liệu Này

> **Câu hỏi phỏng vấn thực tế:**  
> *"Nếu không dùng JPA/Hibernate ở BE, khi INSERT record vào bảng có ID tự tăng (SERIAL/BIGSERIAL), làm sao BE nhận được ID đó?"*

**Đáp án:** PostgreSQL có mệnh đề `RETURNING` — Trả về giá trị của bất kỳ column nào ngay sau INSERT/UPDATE/DELETE:

```sql
-- Không dùng JPA — Raw JDBC/SQL:
INSERT INTO bank_accounts (account_name, balance, customer_id)
VALUES ('Tài khoản thanh toán', 0, 'cust-001')
RETURNING id, account_number, created_at;
--        ↑ PostgreSQL trả về ngay các cột này sau khi INSERT thành công

-- Kết quả trả về ngay:
-- id: 42, account_number: "088880042", created_at: "2026-09-16 06:30:00"
```

**Trong JDBC thuần (không JPA):**
```java
String sql = "INSERT INTO bank_accounts(account_name, balance) VALUES(?,?) RETURNING id";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, "Tài khoản thanh toán");
ps.setBigDecimal(2, BigDecimal.ZERO);
ResultSet rs = ps.executeQuery(); // executeQuery không phải executeUpdate!
if (rs.next()) {
  long generatedId = rs.getLong("id"); // Nhận ID ngay lập tức
}
```

**Đây là PostgreSQL-specific feature** — MySQL, SQL Server có cách riêng (kém linh hoạt hơn).

---

## 🗺️ Lộ Trình Học

```
Tuần 1: PostgreSQL Foundations
  Bài 01 → Bài 02 → Bài 03
  
Tuần 2: PostgreSQL Advanced + Redis Basics
  Bài 04 → Bài 05 → Bài 06
  
Tuần 3: Ôn tập + Phỏng vấn
  Bài 07 → Thực hành với BankX codebase
```

---

## 🔗 Liên Kết Với BankX Project

| Feature BankX | Database Liên Quan |
|---|---|
| `bank_accounts.version` — Optimistic Lock | PostgreSQL `@Version` + Bài 04 |
| `ledger_entries` Immutable | PostgreSQL INSERT-only + Bài 01 |
| `outbox_events` Polling | PostgreSQL `SELECT FOR UPDATE SKIP LOCKED` + Bài 04 |
| `idempotency:lock:{uuid}` | Redis `SET NX EX` + Bài 06 |
| `account_balance:{accNum}` Cache | Redis String + TTL + Bài 05 |
| `otp_attempts:{phone}` counter | Redis Hash + INCR + Bài 06 |
| Fraud alerts rate limit | Redis Sorted Set sliding window + Bài 06 |
| Transaction History Pagination | PostgreSQL Cursor Pagination + Bài 02 |
| `EXPLAIN ANALYZE` optimization | PostgreSQL Index + Bài 03 |
