# BankX Banking Platform — Hỏi & Đáp Phỏng Vấn Banking (Interview Q&A)

Tài liệu này tổng hợp các câu hỏi phỏng vấn banking thực tế và câu trả lời mẫu dựa trên những gì đã implement trong BankX.

> 💡 **Cách dùng:** Sau mỗi Sprint, đọc lại Q&A liên quan. Tập trả lời được trên whiteboard bằng sơ đồ.

---

## 1. Câu Hỏi Về Concurrency (Thi Thường Nhất)

### Q1: "User bấm chuyển tiền 2 lần do lag mạng → hệ thống xử lý thế nào để không trừ tiền 2 lần?"

**Trả lời mẫu (30 giây):**
> "Chúng tôi dùng **Idempotency Key** — một UUID do frontend sinh ra trước khi submit. Backend dùng `Redis SETNX` (Set if Not Exists) với TTL 10 phút. Request đầu tiên: Redis set key thành công → xử lý transfer. Request thứ hai với cùng key: Redis trả về false → backend return 409 Conflict ngay, không tạo thêm transaction. Sau khi transfer hoàn thành, response được cache vào Redis cùng key → lần gọi sau trả về kết quả giống nhau."

**Code reference:** `banking/backend/.../transfer/application/service/TransferApplicationService.java`

---

### Q2: "Hai người cùng truy cập 1 tài khoản và cùng rút tiền vượt số dư → xử lý thế nào?"

**Trả lời mẫu:**
> "Đây là bài toán **Race Condition**. Chúng tôi dùng **Optimistic Locking** với cột `version` trong bảng `bank_accounts`. Mỗi khi update balance: `UPDATE bank_accounts SET balance=?, version=version+1 WHERE id=? AND version=?`. Nếu 2 request đồng thời đọc version=5, request A update trước → version thành 6 → request B update WHERE version=5 → 0 rows affected → `OptimisticLockException` → Retry 3 lần với jitter. Nếu sau 3 lần vẫn fail → Transfer FAILED."

**Phải biết giải thích được:**
- Tại sao Optimistic tốt hơn Pessimistic ở đây? → Optimistic không block DB row, tốt cho read-heavy workload. Pessimistic dùng `SELECT FOR UPDATE` → Block row → giảm throughput.
- Khi nào nên dùng Pessimistic? → Khi conflict rate cao (nhiều concurrent writes trên cùng row).

---

### Q3: "Nếu không dùng @Version mà để balance -= amount trực tiếp thì sao?"

**Trả lời:**
> "Race condition: Thread A đọc balance=10M, Thread B đọc balance=10M. A trừ 8M → cập nhật balance=2M. B trừ 8M → cập nhật balance=2M. Thực tế đã chuyển 16M nhưng balance chỉ giảm 8M → tổng tài sản hệ thống âm → ngân hàng mất tiền."

---

## 2. Câu Hỏi Về Distributed Systems

### Q4: "DB commit thành công nhưng Kafka publish failed → dữ liệu xử lý thế nào?"

**Trả lời mẫu:**
> "Đây là bài toán **Dual Write Problem**. Giải pháp là **Transactional Outbox Pattern**. Thay vì publish Kafka trực tiếp trong @Transactional block, chúng tôi ghi event vào bảng `outbox_events` trong cùng DB transaction với transfer. Sau đó, một **Outbox Poller** (scheduled job chạy mỗi giây) đọc các event PENDING, publish lên Kafka, rồi mark là SENT. Tách bạch: DB commit và Kafka publish là 2 bước riêng biệt. DB đảm bảo event không bị mất; Kafka publication có thể retry."

**Nếu bị hỏi thêm:** "Có thể dùng Debezium CDC thay thế Scheduled Poller không?" → Có, Debezium đọc PostgreSQL Write-Ahead Log (WAL) để publish event thay vì polling, latency thấp hơn nhưng cần thêm infrastructure.

---

### Q5: "Kafka Consumer đang xử lý message thì chết → message có bị xử lý 2 lần không?"

**Trả lời:**
> "Kafka đảm bảo **At-Least-Once delivery**. Nghĩa là consumer chết trước khi commit offset → Kafka sẽ redeliver message → Consumer khác (hoặc restart) sẽ nhận lại. Để tránh side effect (gửi 2 notification, charge 2 lần...), Consumer phải **idempotent**: Trước khi xử lý message, check `eventId` trong Redis. Nếu đã processed → skip. Nếu chưa → process và mark processed trong Redis với TTL 1 giờ."

---

### Q6: "Transfer từ Account Service sang Notification Service fail giữa chừng → rollback thế nào?"

**Trả lời:**
> "Khi chạy Microservices, không thể dùng `@Transactional` bao phủ nhiều database độc lập. Giải pháp là **Saga Pattern** (Choreography hoặc Orchestration-based). BankX dùng **Orchestration Saga**: Transfer Service là Saga Orchestrator quản lý các bước: 1. Debit Account → 2. Credit Account → 3. Notify. Nếu bước 2 fail: Orchestrator gửi lệnh Compensate Step 1 (reverse debit → credit lại tài khoản nguồn) → Update transfer status = FAILED → Notify failure."

---

### Q7: "Tại sao không dùng 2PC (Two-Phase Commit) thay Saga?"

**Trả lời:**
> "2PC cần tất cả participants lock tài nguyên cho đến khi coordinator quyết định commit/rollback → giảm availability rất mạnh (blocking protocol). Trong banking microservices với hàng nghìn TPS, 2PC trở thành bottleneck. Saga dùng eventual consistency thay vì strong consistency → trade-off chấp nhận được vì banking transaction thường có compensation actions."

---

## 3. Câu Hỏi Về Security

### Q8: "JWT Access Token bị đánh cắp → làm thế nào để logout tất cả session?"

**Trả lời:**
> "Mỗi lần refresh token, chúng tôi dùng **Refresh Token Rotation**: token cũ bị revoke ngay trong Redis, token mới được cấp. Nếu attacker dùng stolen access token (tồn tại max 15 phút), sau 15 phút tự expire. Để revoke ngay: Admin có thể call `POST /api/auth/revoke-all/{userId}` → Xóa tất cả refresh tokens của user khỏi Redis → Tất cả session logout ngay lần refresh tiếp theo. Hoặc dùng blacklist token ID trong Redis với TTL = remaining token lifetime."

---

### Q9: "Rate Limiting implement thế nào? Token Bucket hay Leaky Bucket?"

**Trả lời:**
> "BankX dùng **Token Bucket** stored in Redis. Mỗi user có một bucket với capacity=10 và refill_rate=5 tokens/second. Mỗi API call tốn 1 token. Khi bucket rỗng → 429 Too Many Requests. Redis dùng Lua Script để check và consume token atomically (tránh race condition trong rate limiter chính nó). Login endpoint: Max 5 attempts/minute/IP. Transfer: Max 10 requests/minute/user."

---

### Q10: "Làm thế nào để prevent SQL Injection trong Spring?"

**Trả lời:**
> "Spring Data JPA/JPQL tự động dùng prepared statements — tham số được bind, không concatenate string. Đối với native queries, dùng `@Query(nativeQuery=true)` với `:param` placeholder. KHÔNG BAO GIỜ dùng string concatenation trong query. Thêm vào đó, Spring Security có input validation (@Valid, @NotNull, @Size) ở Controller layer trước khi data vào business logic."

---

## 4. Câu Hỏi Về Performance & Scalability

### Q11: "1 tỷ transaction records → query lịch sử giao dịch của 1 user thế nào?"

**Trả lời:**
> "Dùng **CQRS** với Read Model riêng. Write side: `transactions` và `ledger_entries` tables (normalized). Read side: `transaction_history_view` table được denormalized, index trên `(customer_id, created_at DESC)`, partitioned by month. Khi transaction được tạo, Kafka consumer cập nhật Read Model async. Query history đọc từ Read Model → Fast. Thêm pagination (offset-based hoặc cursor-based với created_at). Nếu cần full-text search: Sync sang Elasticsearch."

---

### Q12: "Balance API phải trả về trong 20ms → caching strategy thế nào?"

**Trả lời:**
> "**Cache Aside Pattern** với Redis TTL 30 giây. Flow: Request balance → Check Redis `account:balance:{accountId}` → Cache hit: Return ngay (<5ms). Cache miss: Query PostgreSQL → Lưu vào Redis TTL 30s → Return (~50ms lần đầu). Cache invalidation: Khi balance thay đổi (debit/credit), xóa cache key ngay trong cùng transaction. Chấp nhận 30s stale data cho dashboard display (không phải critical real-time). Với confirmation screen trước khi transfer: Luôn đọc fresh từ DB."

---

### Q13: "Nếu Redis chết thì sao?"

**Trả lời:**
> "Circuit Breaker (Resilience4j): Khi Redis không phản hồi sau N failures → Mở circuit, bypass cache, đọc thẳng DB. Degraded mode nhưng không crash. Khi Redis recover → Close circuit, dần cache lại data. Quan trọng: OTP và Idempotency key phụ thuộc vào Redis → Cần Redis HA (Sentinel hoặc Cluster). Không để Redis là Single Point of Failure."

---

## 5. Câu Hỏi Về Architecture

### Q14: "Tại sao bắt đầu bằng Modular Monolith thay vì Microservices ngay?"

**Trả lời:**
> "Microservices giải quyết vấn đề của large-scale team và independent deployment. Khi mới bắt đầu: Không có team size lớn, domain boundaries chưa ổn định, distributed system complexity rất cao (service discovery, tracing, eventual consistency). Modular Monolith cho phép tách module rõ ràng nhưng chạy trong 1 process → Dễ debug, deploy, test, và quan trọng nhất: hiểu domain trước khi tách. Sau khi domain stabilize → Extract service một cách có chủ đích (Notification trước, vì ít dependency nhất)."

---

### Q15: "Clean Architecture có lợi gì trong banking?"

**Trả lời:**
> "Domain layer không phụ thuộc vào Spring hay JPA → Business rules có thể test thuần Java, không cần load Spring Context → Test nhanh hơn 100x. Infrastructure layer implement Domain interfaces → Có thể swap DB từ PostgreSQL sang Oracle mà không đổi business logic. Presentation layer chỉ biết Application Use Cases → Có thể add gRPC endpoint mà không đổi logic. Quan trọng trong banking: Domain invariants (balance không được âm, ledger phải cân bằng) được enforce ở tầng domain, không thể bypass dù từ bất kỳ entry point nào."

---

### Q16: "Double-Entry Bookkeeping là gì và tại sao banking phải dùng?"

**Trả lời:**
> "Kế toán kép: Mỗi giao dịch tạo ít nhất 2 entries — một Debit và một Credit. Tổng Debit = Tổng Credit. Lợi ích: Audit trail không thể chối cãi (immutable ledger entries), phát hiện lỗi ngay khi SUM(DEBIT) ≠ SUM(CREDIT), reconstruct balance tại bất kỳ thời điểm nào (sum tất cả entries của account từ T0). Đây là nền tảng của mọi hệ thống kế toán từ thế kỷ 15 và tất cả core banking systems hiện đại."

---

## 6. Câu Hỏi Về Observability

### Q17: "Transfer thất bại ở service nào → tìm thế nào?"

**Trả lời:**
> "**Distributed Tracing** với OpenTelemetry + Jaeger. Mỗi request vào Gateway được gán một `traceId` duy nhất (UUID), truyền qua tất cả service qua HTTP header `X-Trace-Id` và Kafka message header. Mỗi service tạo `span` con khi thực hiện operation. Tất cả spans được export về Jaeger Collector. Khi có lỗi: Search traceId trong Jaeger → Thấy ngay span nào fail, latency của từng service, exception message."

---

## 7. Câu Hỏi Về Angular (Nếu Vào Vị Trí Fullstack)

### Q18: "Tại sao dùng Angular Signals thay vì RxJS BehaviorSubject cho local state?"

**Trả lời:**
> "Signals là reactive primitives mới trong Angular 22. Ưu điểm: Fine-grained reactivity (chỉ re-render component dùng signal đó, không cả cây), dễ đọc hơn RxJS chains, computed signals tự động update khi dependency thay đổi. Dùng Signals cho local state (1 component/feature). Vẫn dùng RxJS Observable cho HTTP calls (Angular HttpClient) và NgRx cho global state phức tạp (auth, cross-feature shared state)."

---

### Q19: "Idempotency Key sinh ra ở đâu trong Angular — Frontend hay Backend?"

**Trả lời:**
> "**Frontend sinh UUID** ngay trước khi user bấm submit transfer. Lý do: Nếu backend sinh key → Client không có key để re-send nếu response bị mất (timeout). Nếu client sinh key → Client có thể retry với cùng key → Backend nhận diện duplicate. Implementation: `crypto.randomUUID()` (browser native API) hoặc `uuidv4()` (npm package). Key được đặt trong HTTP header `Idempotency-Key`."

---

## 8. Checklist Chuẩn Bị Phỏng Vấn

Trước khi phỏng vấn, phải giải thích được (không nhìn tài liệu):

- `[ ]` Vẽ sequence diagram Transfer flow (end-to-end)
- `[ ]` Giải thích Outbox Pattern trên whiteboard
- `[ ]` Demo code Optimistic Locking với @Version
- `[ ]` Giải thích Idempotency với Redis SETNX
- `[ ]` Vẽ sơ đồ Saga với compensation flow
- `[ ]` Giải thích Double-Entry Bookkeeping với example số cụ thể
- `[ ]` Giải thích tại sao Modular Monolith trước Microservices
- `[ ]` Giải thích Clean Architecture dependency rule
- `[ ]` Giải thích Circuit Breaker 3 trạng thái (CLOSED/OPEN/HALF-OPEN)
- `[ ]` Giải thích JWT Refresh Token Rotation
