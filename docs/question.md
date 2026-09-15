# ❓ Titan BankX — Câu Hỏi & Trả Lời Phỏng Vấn Banking Java Senior

> **Dành cho:** Phỏng vấn Java Senior / Banking Engineer / Distributed Systems  
> **Format:** Câu hỏi thách thức → Tại sao KHÔNG → Tại sao NÊN  
> **Xoay quanh:** Toàn bộ kiến trúc và code thực tế của Titan BankX Platform  

---

## 📋 Mục Lục

**Chủ đề cốt lõi:**
1. [Saga & Outbox Pattern](#1-saga--outbox-pattern)
2. [Transactional Outbox Pattern](#2-transactional-outbox-pattern)
3. [Idempotency & Duplicate Transfer](#3-idempotency--duplicate-transfer)
4. [Optimistic vs Pessimistic Locking](#4-optimistic-vs-pessimistic-locking)
5. [Double-Entry Bookkeeping](#5-double-entry-bookkeeping)
6. [JWT & Refresh Token](#6-jwt--refresh-token)
7. [CQRS & Event Sourcing](#7-cqrs--event-sourcing)
8. [Circuit Breaker & Resilience](#8-circuit-breaker--resilience)
9. [Kafka & Event-Driven Architecture](#9-kafka--event-driven-architecture)
10. [Risk-Based OTP & Authentication](#10-risk-based-otp--authentication)
11. [VietQR & Tokenization](#11-vietqr--tokenization)
12. [Fraud Detection & Rule Engine](#12-fraud-detection--rule-engine)
13. [Distributed Tracing](#13-distributed-tracing)
14. [Clean Architecture & DDD](#14-clean-architecture--ddd)
15. [Performance & Concurrency](#15-performance--concurrency)
16. [Database & Flyway](#16-database--flyway)
17. [Redis & Caching Strategy](#17-redis--caching-strategy)
18. [API Design & Gateway](#18-api-design--gateway)

**Chủ đề nâng cao (mới thêm):**

19. [gRPC & Protobuf trong Banking](#19-grpc--protobuf-trong-banking)
20. [Database Optimization — Bảng Tỷ Record](#20-database-optimization--bảng-tỷ-record)
21. [Security & OWASP Top 10](#21-security--owasp-top-10-trong-banking)
22. [Testing Strategy](#22-testing-strategy-trong-banking)
23. [Microservices & Deployment Patterns](#23-microservices--deployment-patterns)
24. [Câu Hỏi Mẹo & Bẫy — @Transactional, N+1, Memory Leak...](#24-câu-hỏi-mẹo--bẫy-tricky-questions)
25. [Scalability & System Design — 100K TPS, Rate Limiting, Kafka HA](#25-scalability--system-design)

---

## 1. Saga & Outbox Pattern

---

### ❓ Saga có nhất thiết phải dùng cùng Outbox Pattern trong ngân hàng không?

**Tại sao KHÔNG (lạm dụng Over-Engineering):**
- Đối với Giao dịch Nội bộ (Internal Transfer cùng DB): Dùng Saga là Over-Engineering hoàn toàn. Một `@Transactional` đơn giản đã đảm bảo ACID 100%. Thêm Saga vào chỉ tăng độ phức tạp vô nghĩa, sinh thêm bảng `saga_instances`, `saga_audit_steps`, tăng latency và khó debug.

**Tại sao NÊN (đúng ngữ cảnh):**
1. **Đối với Giao dịch Nội bộ (Internal Transfer):** Luôn ưu tiên dùng Single DB Transaction chuẩn ACID kết hợp với Transactional Outbox Pattern. Outbox đảm bảo sự kiện được publish sang Kafka an toàn 100% để phục vụ Notification, Analytics và CQRS mà không cần Saga.
2. **Đối với Giao dịch Liên ngân hàng (Interbank — NAPAS/SWIFT) hoặc giữa các Microservices tách DB hoàn toàn:** Khi không thể dùng chung 1 DB Transaction, mới áp dụng Saga Pattern (Orchestration/Choreography) để quản lý State Machine và thực hiện các bút toán hoàn trả (Compensating Transactions — `REVERSE_DEBIT`) khi có sự cố.

---

### ❓ Saga Orchestration vs Saga Choreography — khi nào dùng cái nào?

**Tại sao KHÔNG dùng Choreography cho nghiệp vụ phức tạp:**
- Choreography (Event-Driven giữa các service) dễ tạo ra "Spaghetti Logic" — không rõ luồng xử lý, khó debug khi sự cố, rất khó trace được một giao dịch thất bại đang ở bước nào.
- Khi số lượng steps tăng lên (> 3-4 bước), việc trace event qua nhiều services trở thành ác mộng vận hành.

**Tại sao NÊN dùng Orchestration cho Banking:**
- Saga Orchestrator đóng vai trò "nhạc trưởng" — tập trung toàn bộ State Machine vào một nơi (bảng `saga_instances`), biết chính xác saga đang ở bước nào (`DEBIT_COMPLETED`, `CREDIT_PENDING`...).
- Khi credit thất bại → Orchestrator tự động phát lệnh `REVERSE_DEBIT` (Compensating Transaction) có thể audit rõ ràng qua bảng `saga_audit_steps`.
- Trong BankX: `TransferSagaOrchestrator` điều phối toàn bộ flow Interbank Transfer.

---

### ❓ Compensating Transaction (bút toán hoàn trả) trong Saga được triển khai thế nào?

**Tại sao KHÔNG dùng DELETE/UPDATE để hoàn trả:**
- Trong banking, các bút toán ledger là **bất biến (IMMUTABLE)**. Không bao giờ được xóa hay sửa lịch sử giao dịch. Đây là quy tắc kiểm toán tài chính (Financial Audit Immutability).

**Tại sao NÊN tạo bút toán đảo ngược (Reversal Entry):**
- Compensating Transaction là một giao dịch **mới** với chiều ngược lại: Nếu đã DEBIT tài khoản A thì Compensating = CREDIT lại tài khoản A với dấu hiệu `REVERSE_DEBIT`.
- Toàn bộ lịch sử hành động được ghi vào `saga_audit_steps` để audit, báo cáo và debug.

---

## 2. Transactional Outbox Pattern

---

### ❓ Tại sao không dùng Kafka publish trực tiếp trong @Transactional thay vì Outbox?

**Tại sao KHÔNG (Direct Kafka Publish trong Transaction):**
- Kafka publish KHÔNG tham gia vào DB Transaction. Nếu DB commit thành công nhưng Kafka network bị partition → message bị mất vĩnh viễn. Không có cách nào biết liệu message đã được publish hay chưa.
- Trường hợp ngược lại: Kafka publish xong nhưng DB rollback → Kafka đã có message "rác" → consumer xử lý dữ liệu không nhất quán.

**Tại sao NÊN dùng Transactional Outbox:**
- Ghi sự kiện vào bảng `outbox_events` **trong cùng DB Transaction** với lệnh chuyển tiền. Nếu transfer commit → outbox record chắc chắn tồn tại. Nếu transfer rollback → outbox record cũng rollback.
- `OutboxPollingService` (@Scheduled 2s) đọc PENDING records → publish Kafka → mark SENT. Đây là cơ chế **At-Least-Once Delivery** đảm bảo 100%.
- Nếu Kafka DOWN → records vẫn ở trạng thái PENDING → khi Kafka UP, poller tự động retry. **Không mất dữ liệu.**

---

### ❓ Outbox Pattern có thể gây duplicate message không? Xử lý thế nào?

**Tại sao KHÔNG bỏ qua vấn đề duplicate:**
- At-Least-Once delivery có nghĩa là **đôi khi message được publish 2 lần** (poller restart giữa chừng, network retry...). Nếu consumer không idempotent, sẽ gửi 2 notification cho cùng 1 giao dịch.

**Tại sao NÊN thiết kế Idempotent Consumer:**
- Consumer kiểm tra Redis key `consumed_event:{eventId}` (TTL 1 giờ) trước khi xử lý.
- Nếu key đã tồn tại → message đã xử lý rồi → **bỏ qua (skip)** an toàn.
- Nếu key chưa tồn tại → xử lý → ghi key vào Redis.
- Đây là pattern **Idempotent Consumer** được triển khai trong `NotificationKafkaListener` của BankX.

---

### ❓ Tại sao không dùng Debezium CDC thay vì Scheduled Polling cho Outbox?

**Tại sao KHÔNG bắt buộc phải dùng Debezium:**
- Debezium CDC (Change Data Capture) đọc PostgreSQL WAL log, phức tạp để setup và vận hành, thêm dependency infrastructure.
- Với hệ thống tải vừa phải (< 10K TPS), Scheduled Polling 2s hoàn toàn đủ dùng và đơn giản hơn rất nhiều.

**Tại sao NÊN cân nhắc Debezium khi scale lớn:**
- Khi TPS rất cao (> 50K events/s), Polling tạo áp lực lên DB (constant SELECT queries).
- Debezium đọc WAL log với near-realtime latency (milliseconds) và không impact DB query performance.
- Nhưng đây là quyết định kiến trúc cần cân nhắc kỹ dựa trên load thực tế.

---

## 3. Idempotency & Duplicate Transfer

---

### ❓ User bấm "Chuyển tiền" hai lần (double click) trong 1 giây — xử lý thế nào?

**Tại sao KHÔNG chỉ dùng UI disabled button:**
- Disable button trên frontend là UX tốt nhưng **không đủ** ở góc độ bảo mật/độ tin cậy. User có thể gửi 2 request song song bằng nhiều cách khác (script, Postman, network retry, mobile app).
- Backend phải là lớp bảo vệ cuối cùng, không thể phụ thuộc vào frontend.

**Tại sao NÊN dùng Idempotency Key + Redis SETNX:**
1. Angular tự động generate UUID v4 cho mỗi request (`X-Idempotency-Key: {uuid}`).
2. Backend AOP `IdempotencyAspect` nhận header, thực hiện `Redis SETNX idempotency:lock:{uuid}` với TTL 10 phút.
3. Nếu key chưa tồn tại (lần đầu) → xử lý bình thường → cache kết quả JSON vào Redis.
4. Nếu key đã tồn tại (duplicate) → trả về **cached response** ngay lập tức, **KHÔNG trừ tiền lần 2**.

---

### ❓ Idempotency Key nên được lưu ở đâu? DB hay Redis?

**Tại sao KHÔNG lưu DB:**
- Mỗi request cần 1 SELECT + 1 INSERT vào DB → tăng load DB không cần thiết.
- DB không có cơ chế TTL tự nhiên → phải tự viết cleanup job.

**Tại sao NÊN lưu Redis:**
- Redis SETNX (Set if Not Exists) là atomic operation — đảm bảo chỉ 1 trong nhiều concurrent requests giành được lock.
- TTL tự động hết hạn sau 10 phút — không cần cleanup job.
- Tốc độ kiểm tra sub-millisecond.

---

## 4. Optimistic vs Pessimistic Locking

---

### ❓ Hai người cùng chuyển tiền từ một tài khoản đồng thời — xử lý thế nào?

**Tại sao KHÔNG dùng Pessimistic Lock (`SELECT FOR UPDATE`):**
- `SELECT FOR UPDATE` khóa DB row cho đến khi transaction kết thúc. Nếu có 100 requests đồng thời → 99 requests còn lại phải xếp hàng chờ → HikariCP Connection Pool cạn kiệt → Timeout, hệ thống trở nên không phản hồi được (Connection Pool Exhaustion).
- Đặc biệt nguy hiểm trong banking khi có traffic spike.

**Tại sao NÊN dùng Optimistic Lock (`@Version`) + Spring Retry:**
- `@Version` column trên `bank_accounts` tự động tăng mỗi lần UPDATE. Không lock DB row, nhiều thread có thể đọc đồng thời.
- Khi 2 thread A và B cùng cập nhật: Thread A thành công → version tăng từ 5 lên 6. Thread B kiểm tra `UPDATE WHERE version=5` → không tìm thấy row (đã thành 6) → `ObjectOptimisticLockingFailureException`.
- `@Retryable(maxAttempts=3)` tự động reload phiên bản mới và thử lại → gần như luôn thành công ở lần thử thứ 2.
- **High TPS, No blocking, No deadlock.**

---

### ❓ Optimistic Lock có thể gây Starvation không?

**Tại sao NÊN biết:** Nếu có hàng trăm concurrent requests cùng update 1 row (như tài khoản viral), tỷ lệ collision rất cao → retry nhiều lần → có thể không hoàn thành trong max 3 attempts.

**Giải pháp trong BankX:**
- Exponential Backoff (100ms, 200ms, 400ms) giảm tần suất collision.
- Rate Limiter (Resilience4j, 5 req/s cho interbank) giới hạn số requests đến.
- Idempotency đảm bảo retry an toàn.

---

## 5. Double-Entry Bookkeeping

---

### ❓ Tại sao Banking phải dùng Double-Entry Bookkeeping thay vì chỉ UPDATE số dư?

**Tại sao KHÔNG chỉ UPDATE balance:**
- `UPDATE bank_accounts SET balance = balance - 100000 WHERE id = 1` rất đơn giản nhưng không audit được: Không biết tiền đi đâu, không detect được lỗi kế toán.
- Không đủ tiêu chuẩn kiểm toán tài chính (Financial Audit) theo chuẩn quốc tế.

**Tại sao NÊN dùng Double-Entry:**
- Mọi giao dịch tài chính phải tạo ra ít nhất 2 bút toán: 1 DEBIT + 1 CREDIT.
- Quy tắc bất biến: `SUM(DEBIT entries) == SUM(CREDIT entries)` trong cùng một transaction. Nếu vi phạm → hệ thống từ chối giao dịch.
- Bảng `ledger_entries` là **IMMUTABLE** — không có UPDATE, không có DELETE, không có soft delete. Mọi điều chỉnh là một giao dịch mới.
- Cho phép phát hiện lỗi kế toán, audit trail đầy đủ, reconciliation cuối ngày.

---

### ❓ Tại sao bảng ledger_entries không có cột `updated_at` hay cột `deleted_at`?

**Tại sao KHÔNG cho phép sửa/xóa ledger entries:**
- Trong tài chính, ledger là "sổ sách kế toán gốc" (Book of Original Entry). Một khi đã ghi, không được phép sửa hoặc xóa theo chuẩn kiểm toán quốc tế (GAAP, IFRS).
- Nếu có lỗi, phải tạo **giao dịch đảo ngược (Reversal Transaction)** mới — không phải sửa record cũ.

**Tại sao NÊN thiết kế Immutable Ledger:**
- Đảm bảo tính toàn vẹn dữ liệu kế toán.
- Audit trail hoàn chỉnh, không thể bị giả mạo.
- Trong BankX: `ledger_entries` chỉ có `created_at`, không có `updated_at`. Spring Data JPA được cấu hình không cho phép UPDATE trên bảng này.

---

## 6. JWT & Refresh Token

---

### ❓ Tại sao không dùng Session-based Auth thay vì JWT trong banking?

**Tại sao KHÔNG dùng Session:**
- Session lưu state trên server → khó scale horizontal (multiple instances). Dùng sticky session hoặc session replication phức tạp.
- Với API Gateway + Microservices, session không phù hợp cho stateless architecture.

**Tại sao NÊN dùng JWT + Refresh Token Rotation:**
- Access Token (15 phút TTL) → stateless, validate bằng chữ ký không cần query DB → rất nhanh.
- Refresh Token (7 ngày) lưu DB + Redis → có thể revoke, track, rotate.
- **Refresh Token Rotation:** Mỗi lần dùng Refresh Token để lấy Access Token mới → token cũ bị vô hiệu hóa ngay lập tức. Nếu token bị đánh cắp và attacker dùng → hệ thống phát hiện "token đã được dùng rồi" → revoke toàn bộ session của user đó.

---

### ❓ Nếu Refresh Token bị đánh cắp, xử lý thế nào?

**Tại sao KHÔNG chỉ để token hết hạn:**
- Nếu không có cơ chế revoke, attacker có thể dùng token stolen trong suốt 7 ngày TTL.

**Tại sao NÊN có Refresh Token Rotation + Revocation:**
1. Khi user "Đăng xuất" → mark token là `REVOKED` trong DB.
2. Khi token bị dùng lại sau khi revoke → reject + alert.
3. Hệ thống biết token đã rotate rồi mà bị dùng lại → có thể force logout toàn bộ sessions của user đó (paranoid mode).
4. User có thể xem và revoke từng session từ "Thiết bị đã đăng nhập" (Device Management).

---

### ❓ Tại sao lưu Refresh Token cả trong DB lẫn Redis?

**Tại sao KHÔNG chỉ lưu Redis:**
- Redis là in-memory, restart mất data → user bị logout hàng loạt khi Redis restart.
- Không có audit trail — không biết token nào được tạo từ đâu, khi nào.

**Tại sao KHÔNG chỉ lưu DB:**
- Query DB mỗi lần validate Refresh Token → tăng latency, tăng tải DB.

**Tại sao NÊN lưu cả hai:**
- **DB** là source of truth: Persist qua restart, audit trail, revocation.
- **Redis** là cache: Validate nhanh (sub-ms) mà không query DB mỗi request.

---

## 7. CQRS & Event Sourcing

---

### ❓ CQRS có cần thiết cho Banking Core không?

**Tại sao KHÔNG áp dụng CQRS cho mọi thứ:**
- Áp dụng CQRS cho CRUD đơn giản là Over-Engineering. Tăng code complexity (2 model cho 1 entity), eventual consistency khó debug.
- Với tính năng đơn giản (Customer Profile, Account Info), 1 model đọc/ghi là đủ.

**Tại sao NÊN áp dụng CQRS cho Transaction History:**
- **Write model:** `bank_transfers` được optimize cho write — index trên `(account_id, status)`, foreign keys, transactional.
- **Read model:** `transaction_history_view` được denormalize cho read — tất cả thông tin cần hiển thị gộp sẵn, cursor-based pagination O(log N) thay vì OFFSET O(N).
- Lịch sử giao dịch thường được query nhiều hơn write 10:1 → read model optimize riêng tăng performance đáng kể.
- Event-driven projection: Kafka event `TRANSFER_COMPLETED` → `TransactionHistoryProjection` update read model.

---

### ❓ Cursor-based Pagination vs OFFSET Pagination — khi nào dùng cái nào?

**Tại sao KHÔNG dùng OFFSET cho Transaction History:**
- `SELECT ... OFFSET 10000 LIMIT 20` → Database phải scan 10.020 rows, bỏ 10.000 rows đầu, trả về 20 rows. O(N) performance — càng nhiều trang thì càng chậm.
- Với user có hàng triệu giao dịch, trang 50.000 sẽ timeout.

**Tại sao NÊN dùng Cursor-based Pagination:**
- `SELECT ... WHERE id < {cursor} ORDER BY id DESC LIMIT 20` → Index trên `id` → O(log N) bất kể trang nào.
- Cursor là `id` (hoặc `created_at`) của record cuối cùng đã thấy → trang tiếp theo bắt đầu từ đó.
- Stable results: Nếu có insert mới, không bị lệch trang như OFFSET.

---

## 8. Circuit Breaker & Resilience

---

### ❓ Circuit Breaker hoạt động thế nào và tại sao cần nó?

**Tại sao KHÔNG để request timeout tự nhiên:**
- Không có Circuit Breaker, nếu external bank API chậm (5s/request) và có 100 concurrent requests → 100 threads bị block 5s → Thread pool cạn kiệt → Toàn bộ hệ thống bị treo (Cascading Failure).

**Tại sao NÊN dùng Circuit Breaker (Resilience4j):**
- **CLOSED** (bình thường): 100% request được forward đến external bank.
- **OPEN** (sự cố): Khi failure rate > 50% trong sliding window → Circuit mở → **Fail Fast** ngay lập tức (không chờ timeout), kích hoạt Fallback `PENDING_MANUAL_REVIEW`. Giải phóng thread pool ngay lập tức.
- **HALF_OPEN** (kiểm tra phục hồi): Sau 60s, cho 2 request test qua → Nếu OK → quay về CLOSED; nếu fail → về OPEN.
- Trong BankX: Fallback tự động chuyển giao dịch sang trạng thái `PENDING_MANUAL_REVIEW` để team ops xử lý thủ công — không mất tiền, không crash.

---

### ❓ Rate Limiter và Circuit Breaker khác nhau gì? Khi nào dùng cái nào?

**Tại sao NÊN hiểu rõ sự khác biệt:**
- **Rate Limiter** (Resilience4j RateLimiter): Giới hạn **số request gửi đi** trong một khoảng thời gian. Dùng để bảo vệ external bank API khỏi bị quá tải từ phía mình (`max 5 calls/second`). Chủ động throttle.
- **Circuit Breaker**: Phản ứng với **tỷ lệ lỗi nhận về** từ external service. Tự động ngắt kết nối khi external service đang có vấn đề. Phòng thủ.
- **Retry**: Tự động thử lại khi có lỗi tạm thời. Kết hợp với Exponential Backoff + Jitter.
- **Bulkhead**: Giới hạn concurrent calls (10 calls đồng thời tối đa). Tránh resource exhaustion.

---

## 9. Kafka & Event-Driven Architecture

---

### ❓ Tại sao chọn Kafka thay vì RabbitMQ cho Banking?

**Tại sao KHÔNG chọn RabbitMQ cho trường hợp này:**
- RabbitMQ là message broker tốt cho queuing pattern nhưng message bị xóa sau khi consume → không thể replay lại lịch sử event.
- Không có khái niệm partition/consumer group linh hoạt như Kafka.

**Tại sao NÊN chọn Kafka:**
- **Event Log bền vững (Durable):** Message được lưu theo retention period (7 ngày, hoặc theo size). Có thể replay lại toàn bộ lịch sử event để rebuild CQRS read model.
- **Consumer Groups:** Nhiều microservices có thể consume cùng 1 topic độc lập (Notification Service, Analytics Service, CQRS Projection đều cùng consume `transfer.completed`).
- **High Throughput:** Kafka được thiết kế cho hàng triệu messages/giây.
- **Ordering Guarantee:** Trong một partition, message luôn được consume theo thứ tự ghi.

---

### ❓ Khi Kafka Consumer bị restart giữa chừng, message có bị xử lý 2 lần không?

**Tại sao KHÔNG bỏ qua vấn đề này:**
- Kafka sử dụng At-Least-Once delivery theo mặc định. Khi consumer restart trước khi commit offset → message được deliver lại → consumer xử lý lần 2 → gửi 2 notification, tạo 2 record.

**Tại sao NÊN thiết kế Idempotent Consumer:**
- Trước khi xử lý, kiểm tra Redis key `consumed_event:{kafkaMessageId}` (TTL 1 giờ).
- Đã tồn tại → skip. Chưa tồn tại → xử lý + ghi key.
- **Exactly-Once Semantics** ở application level — không cần Kafka Transactions phức tạp.

---

### ❓ Kafka Topic nên được partition như thế nào cho Transfer Events?

**Tại sao NÊN partition theo accountId:**
- Nếu partition ngẫu nhiên (default round-robin), 2 transfer events của cùng 1 account có thể rơi vào 2 partition khác nhau → được process bởi 2 consumer thread khác nhau → thứ tự xử lý không đảm bảo.
- Partition by `accountId` (key-based partitioning) → tất cả events của 1 account luôn vào cùng 1 partition → ordering guarantee trong cùng partition → CQRS projection đúng thứ tự.

---

## 10. Risk-Based OTP & Authentication

---

### ❓ Tại sao không yêu cầu OTP cho mọi giao dịch chuyển tiền?

**Tại sao KHÔNG OTP mọi giao dịch:**
- UX tệ — user chuyển 10.000 VND cho bạn bè mà phải nhập OTP → frustrating, giảm tỷ lệ hoàn thành giao dịch.
- Overhead không cần thiết cho giao dịch rủi ro thấp.

**Tại sao NÊN áp dụng Risk-Based Authentication:**
- **Giao dịch < 5.000.000 VND (rủi ro thấp):** Hoàn tất ngay lập tức — không cần OTP.
- **Giao dịch ≥ 5.000.000 VND (rủi ro cao):** Yêu cầu OTP 6 chữ số (TTL 120s, max 3 attempts). Trạng thái `PENDING_OTP` → chờ verify → `PROCESSING` → `COMPLETED`.
- **Fraud Engine** có thể nâng mức yêu cầu OTP dựa trên Risk Score (giao dịch đêm khuya, thiết bị mới, địa điểm lạ).

---

### ❓ OTP lưu ở Redis như thế nào để tránh brute-force?

**Tại sao NÊN có cả TTL lẫn attempt counter:**
- `otp:{purpose}:{phone}` → TTL 120s → tự hết hạn sau 2 phút.
- `otp_attempts:{purpose}:{phone}` → counter tăng mỗi lần thử sai, max 3 attempts.
- Sau 3 lần thử sai → block 5 phút (`otp_blocked:{purpose}:{phone}` TTL 300s).
- Ngăn chặn brute force 1.000.000 tổ hợp 6 chữ số trong thời gian ngắn.

---

## 11. VietQR & Tokenization

---

### ❓ VietQR Standard EMVCo hoạt động như thế nào?

**Tại sao NÊN hiểu chuẩn TLV:**
- VietQR là chuẩn QR thanh toán quốc gia Việt Nam, dựa trên EMVCo QR Code Specification (TLV — Tag, Length, Value).
- Chuỗi QR gồm nhiều TLV fields: Tag 00 (Payload Format Indicator), Tag 01 (QR Type: 11=Static/12=Dynamic), Tag 38 (BIN ngân hàng + số tài khoản), Tag 54 (Số tiền), Tag 63 (CRC-16 Checksum).
- Trong BankX: `VietQrParser` decode TLV string, `VietQrGenerator` encode, CRC-16/CCITT-FALSE checksum validate tính toàn vẹn.

---

### ❓ Card Tokenization là gì? Tại sao không lưu số thẻ thật?

**Tại sao KHÔNG lưu PAN (Primary Account Number) thật:**
- Lưu số thẻ thật vi phạm PCI-DSS (Payment Card Industry Data Security Standard) — tiêu chuẩn bảo mật thẻ quốc tế. Nếu bị breach → có thể bị phạt hàng triệu USD và mất license.

**Tại sao NÊN dùng Tokenization:**
- **PAN Token:** Thay thế số thẻ thật bằng một token ngẫu nhiên — không có giá trị nếu bị đánh cắp.
- **Masked PAN:** Chỉ hiển thị 4-6 chữ số cuối (`4000 12** **** 8899`) cho user thấy.
- **Virtual PAN:** Số thẻ ảo 16 chữ số dùng cho online transaction — nếu bị lộ, chỉ cần issue thẻ ảo mới mà không ảnh hưởng tài khoản gốc.

---

## 12. Fraud Detection & Rule Engine

---

### ❓ Tại sao không dùng ML model cho Fraud Detection ngay từ đầu?

**Tại sao KHÔNG bắt đầu với ML:**
- ML model cần dữ liệu training lớn, đội ngũ Data Science, infrastructure phức tạp, thời gian deploy lâu, khó explain quyết định (Black box problem — ngân hàng phải giải thích tại sao từ chối giao dịch).
- MVP banking cần rules đơn giản, dễ hiểu, dễ thay đổi.

**Tại sao NÊN bắt đầu với Rule Engine:**
- Rule Engine dễ implement, dễ audit, dễ explain: "Giao dịch bị từ chối vì tổng Risk Score = 85 > 70, do kết hợp NEW_DEVICE (+50) + HIGH_AMOUNT (+40)."
- Trong BankX: 5 built-in rules với Risk Score 0-100:
  - `HIGH_AMOUNT` (+40): Giao dịch > 20M VND
  - `HIGH_VELOCITY` (+30): > 10 giao dịch trong 1 giờ
  - `NEW_DEVICE` (+50): Thiết bị chưa từng đăng nhập trước đó
  - `NEW_BENEFICIARY` (+20): Người thụ hưởng chưa từng chuyển
  - `NIGHT_TIME` (+15): Giao dịch từ 0h-5h sáng
- **Score < 40** → ALLOW. **Score 40-70** → OTP_REQUIRED. **Score > 70** → BLOCK + Alert.
- Sau khi có data, mới dần tích hợp ML model song song (shadow mode) để so sánh kết quả.

---

### ❓ Fraud alert được duyệt thủ công như thế nào trong Admin Portal?

**Tại sao NÊN có Admin Review workflow:**
- Fraud Detection sẽ có False Positive (giao dịch hợp lệ bị chặn nhầm). Cần admin review để approve hoặc escalate.
- Trong BankX: Admin Portal có màn hình Fraud Dashboard → Xem danh sách alerts → Modal duyệt với action `APPROVED` hoặc `ESCALATED`.
- Toàn bộ action được ghi vào `fraud_alerts` với `reviewedBy`, `reviewedAt`, `reviewNote` để audit.

---

## 13. Distributed Tracing

---

### ❓ Distributed Tracing khác gì với Logging thông thường?

**Tại sao KHÔNG chỉ dùng logging:**
- Khi một request đi qua nhiều services (Gateway → Core → Kafka → Notification), logs nằm rải rác ở nhiều nơi. Rất khó correlate để biết "request này mất bao lâu ở service nào".

**Tại sao NÊN dùng Distributed Tracing (OpenTelemetry + Jaeger):**
- Mỗi request được gán một **Trace ID** duy nhất. Mỗi bước xử lý (DB query, Kafka publish, HTTP call) tạo ra một **Span** với thời gian bắt đầu/kết thúc.
- Toàn bộ chuỗi Spans được gộp thành **Waterfall View** trong Jaeger UI → thấy ngay service nào chậm, bước nào bottleneck.
- Trong BankX: W3C `traceparent` header propagate Trace ID qua tất cả HTTP calls. MDC (Mapped Diagnostic Context) inject `traceId` vào mọi log line.

---

### ❓ W3C traceparent header có format như thế nào?

**Format:** `traceparent: {version}-{traceId}-{spanId}-{traceFlags}`

```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
              ↑  ↑                                 ↑               ↑
         version traceId (128-bit, 32 hex chars)  spanId(64-bit)  flags
```

- `version=00` (hiện tại).
- `traceId` duy nhất cho toàn bộ request journey.
- `spanId` của service hiện tại.
- `flags=01` = sampled (ghi lại trace này).

---

## 14. Clean Architecture & DDD

---

### ❓ Tại sao không để JPA Entity trực tiếp trong Domain Layer?

**Tại sao KHÔNG trộn JPA với Domain:**
- JPA Entity có `@Entity`, `@Column`, `@Id` — gắn chặt với Hibernate/Spring. Domain Object phải độc lập với framework.
- Nếu đổi từ JPA sang Mongo/JDBC, phải sửa cả Domain Model — vi phạm Dependency Rule của Clean Architecture.

**Tại sao NÊN tách Domain và JPA Entity:**
- **Domain Object** (`BankAccount`, `BankTransfer`): Pure Java, business logic, không có framework annotation.
- **JPA Entity** (`BankAccountJpaEntity`): Chỉ là persistence representation — `@Entity`, `@Column`, mapping sang DB.
- **Mapper** (hoặc Factory): Chuyển đổi giữa Domain ↔ JPA Entity trong Infrastructure layer.
- Trong BankX: `BankAccountJpaRepository` implement `BankAccountRepository` interface (Domain) → Domain không biết JPA tồn tại.

---

### ❓ Hexagonal Architecture (Ports & Adapters) là gì?

**Tại sao NÊN hiểu Hexagonal:**
- **Core (Domain + Application):** Business logic hoàn toàn thuần túy. Không import Spring, JPA, Kafka.
- **Ports (Interfaces):** `BankAccountRepository` (output port), `TransferUseCase` (input port) — hợp đồng giữa core và infrastructure.
- **Adapters (Implementation):** `BankAccountJpaRepository`, `KafkaOutboxPublisher`, `RestTransferController` — implement các port.
- **Lợi ích:** Có thể swap database từ PostgreSQL sang MongoDB chỉ bằng cách tạo Adapter mới mà không sửa Domain một dòng nào.

---

## 15. Performance & Concurrency

---

### ❓ Java 21 Virtual Threads (Project Loom) cải thiện gì so với Platform Threads?

**Tại sao Platform Threads cũ có vấn đề:**
- 1 Platform Thread = 1 OS Thread → tốn ~1MB RAM. Nếu thread bị block I/O (DB query, Kafka call) → OS thread ngồi chờ, lãng phí tài nguyên. Với 10.000 concurrent requests → cần 10.000 OS threads → OOM.

**Tại sao NÊN bật Virtual Threads:**
- Virtual Thread là siêu nhẹ (KB, không phải MB), được schedule bởi JVM.
- Khi Virtual Thread bị block I/O → JVM swap nó ra, giao Carrier Thread cho Virtual Thread khác.
- Cùng 10.000 concurrent requests chỉ cần vài chục Carrier Threads (OS threads) thay vì 10.000.
- Trong BankX: `spring.threads.virtual.enabled=true` → Spring tự động dùng Virtual Threads cho mọi request handler.

---

### ❓ HikariCP Connection Pool nên được cấu hình như thế nào?

**Tại sao NÊN hiểu các thông số HikariCP:**
- `maximum-pool-size: 20` — Tối đa 20 connections đến PostgreSQL. Không phải "nhiều connection = tốt hơn". PostgreSQL recommend: `(num_cores * 2) + spindle_count`.
- `minimum-idle: 5` — Giữ sẵn 5 connections warm → không tốn thời gian handshake khi đột nhiên có request.
- `connection-timeout: 20000` — Sau 20s không lấy được connection → throw exception thay vì chờ mãi.
- Với Virtual Threads: Thực tế cần ít connections hơn vì Virtual Threads không block OS thread → HikariCP không bị cạn kiệt nhanh như trước.

---

## 16. Database & Flyway

---

### ❓ Tại sao dùng Flyway thay vì `spring.jpa.hibernate.ddl-auto=update`?

**Tại sao KHÔNG dùng `ddl-auto=update`:**
- `update` mode Hibernate tự động ALTER TABLE khi detect schema change → không kiểm soát được, không rollback được, nguy hiểm trên production. Có thể mất data nếu rename column.
- Không có version history của schema changes.
- Không thể reproduce schema trên môi trường mới một cách đáng tin cậy.

**Tại sao NÊN dùng Flyway:**
- Mỗi thay đổi schema là một migration script (`V1__init_schema.sql`, `V2__add_refresh_tokens.sql`...) được version control.
- Flyway track lịch sử migration trong bảng `flyway_schema_history` → biết chính xác môi trường đang ở version nào.
- `ddl-auto=validate` → chỉ validate schema thực tế với JPA mapping, không tự sửa.
- Reproducible: Chạy Flyway trên DB trống → luôn ra cùng kết quả.

---

### ❓ Tại sao dùng `BigDecimal` cho tiền thay vì `double` hay `float`?

**Tại sao KHÔNG dùng double/float:**
- `double` là floating-point binary — không thể represent chính xác nhiều số thập phân. Ví dụ: `0.1 + 0.2 = 0.30000000000000004` trong Java.
- Với tiền tệ, 1 xu sai cũng không chấp nhận được. Cộng dồn sai số qua hàng triệu giao dịch → sai lệch đáng kể.

**Tại sao NÊN dùng BigDecimal:**
- `BigDecimal` lưu exact decimal representation, không có sai số floating-point.
- Cấu hình: `scale=4` (4 chữ số thập phân), `RoundingMode.HALF_UP` (làm tròn chuẩn tài chính).
- PostgreSQL: Cột `DECIMAL(19, 4)` tương ứng.

---

## 17. Redis & Caching Strategy

---

### ❓ Tại sao cache balance trong Redis chỉ 30 giây TTL?

**Tại sao KHÔNG cache lâu hơn:**
- Balance thay đổi liên tục (mỗi transfer, mỗi payment). Cache quá lâu → user thấy số dư cũ → trải nghiệm xấu và không tin tưởng.

**Tại sao KHÔNG không cache gì cả:**
- Mỗi request xem số dư = 1 DB query → với 10.000 users đồng thời = 10.000 DB queries/giây → quá tải DB.

**Tại sao NÊN cache 30 giây:**
- 30 giây là cân bằng giữa freshness và performance. User reload trang → thấy số dư cũ tối đa 30 giây.
- Khi transfer xong → **Evict cache ngay lập tức** (`account_balance:{accountNumber}` bị xóa) → Lần query tiếp theo lấy trực tiếp từ DB → fresh data.
- Cache hit rate thực tế: ~90% trong giờ bình thường → giảm 90% tải DB.

---

### ❓ Redis SETNX vs SET với NX option khác nhau thế nào?

**Cả hai đều atomic** — đây là điều quan trọng nhất:
- `SETNX key value`: Cũ, không hỗ trợ TTL trong cùng command.
- `SET key value NX EX seconds`: Mới hơn, kết hợp NX (Set if Not Exists) + EX (TTL) trong **1 atomic command** → không có race condition.
- Trong BankX: `redisTemplate.opsForValue().setIfAbsent(key, value, duration)` → Spring Data Redis dùng `SET NX EX` bên dưới.

---

## 18. API Design & Gateway

---

### ❓ Tại sao cần API Gateway thay vì gọi thẳng Backend?

**Tại sao KHÔNG gọi thẳng Backend:**
- Angular phải biết IP/port của từng microservice → coupling chặt → khó scale, khó deploy.
- CORS, Rate Limit, Auth phải xử lý riêng ở từng service.

**Tại sao NÊN có API Gateway:**
- **Single Entry Point:** Angular chỉ cần biết 1 URL (`localhost:8080`). Gateway route sang đúng service.
- **Cross-cutting Concerns:** CORS, Rate Limit, Auth Filter, Request Logging, Correlation ID — xử lý 1 lần ở Gateway, không cần ở từng service.
- **Resilience:** Gateway có thể retry, circuit break, load balance giữa các instances của cùng service.
- **Versioning:** `/api/v1/transfers/**` → Service v1; `/api/v2/transfers/**` → Service v2 (blue/green deployment).

---

### ❓ `X-Idempotency-Key`, `X-Trace-Id` là custom headers — có vi phạm HTTP standard không?

**Tại sao KHÔNG vi phạm:**
- HTTP cho phép custom headers. Convention: Dùng prefix `X-` cho custom headers (mặc dù RFC mới không bắt buộc).
- Nhiều API lớn cũng dùng: `X-Request-Id` (GitHub), `X-Forwarded-For` (Load Balancer), `X-Api-Key` (nhiều services).

**Tại sao NÊN document rõ:**
- Custom headers phải được document trong Swagger/OpenAPI để developer biết cần gửi header gì.
- Trong BankX: Swagger UI hiển thị rõ `X-Idempotency-Key` là required header cho POST /transfers/internal.

---

### ❓ Tại sao Transfer API trả về 202 Accepted thay vì 200 OK khi cần OTP?

**Tại sao NÊN dùng 202 Accepted:**
- `200 OK` có nghĩa là request đã hoàn thành thành công.
- `202 Accepted` có nghĩa là request đã được nhận và **đang xử lý** — kết quả chưa có ngay.
- Khi transfer cần OTP, transfer chưa hoàn thành mà chỉ ở trạng thái `PENDING_OTP` → 202 là semantically chính xác hơn 200.
- Angular nhận 202 + `requiresOtp: true` → hiển thị OTP Modal → user nhập OTP → `POST /transfers/{id}/confirm-otp`.

---

## 19. gRPC & Protobuf trong Banking

---

### ❓ Dự án banking có nên dùng gRPC/Protobuf không?

**Tại sao KHÔNG dùng gRPC cho mọi thứ:**
- gRPC không thể gọi trực tiếp từ browser (Angular) — phải có grpc-web proxy ở giữa, tăng thêm infrastructure layer.
- Tooling debug khó hơn REST (không thể Postman/curl đơn giản, Protobuf binary không readable).
- Team cần học thêm Proto schema language, `protoc` compiler, generated stub code.
- Swagger/OpenAPI tự động không apply được — documentation phức tạp hơn.
- Với BankX (monolith, 1 team, local deploy): REST + JSON là đủ, gRPC là Over-Engineering.

**Tại sao NÊN cân nhắc gRPC trong đúng ngữ cảnh:**
- **Service-to-Service Internal Communication (Microservices):** Khi Account Service gọi sang Notification Service, Fraud Service gọi sang Rule Engine — gRPC mang lại:
  - Binary Protocol (Protobuf): Nhỏ hơn JSON 3-10 lần → Giảm bandwidth đáng kể ở mức high TPS.
  - Strongly typed contracts: `.proto` file là interface contract — cả 2 team phải tuân thủ → ít breaking changes.
  - HTTP/2 multiplexing: Nhiều request trên 1 connection → giảm TCP handshake overhead.
  - Bi-directional streaming: Phù hợp cho real-time feeds (account balance stream, fraud alert stream).
- **Kết luận thực tế:** Banking Core nên dùng REST cho External API (Angular, 3rd party) + gRPC cho Internal Service-to-Service. Đây là pattern của Google Pay, Stripe, Revolut.

---

### ❓ Protobuf schema versioning trong banking — làm thế nào để không breaking change?

**Tại sao KHÔNG xóa hay đổi số thứ tự field trong .proto:**
- Trong Protobuf, field number (không phải tên) là định danh thực sự khi serialize/deserialize.
- Xóa field number 3 → Producer cũ vẫn gửi field 3 → Consumer mới đọc unknown field → Silently ignored (tốt) HOẶC parse error (tệ).
- Đổi type của field (từ int32 sang string) → Wire format không tương thích → Runtime crash.

**Tại sao NÊN áp dụng Backward/Forward Compatibility rules:**
```protobuf
// Phiên bản 1:
message Transfer {
  string id = 1;
  int64 amount = 2;
  string from_account = 3;
}

// Phiên bản 2 — SAFE changes:
message Transfer {
  string id = 1;
  int64 amount = 2;
  string from_account = 3;
  // Thêm field MỚI → Backward compatible
  optional string description = 4;
  // KHÔNG BAO GIỜ tái dụng lại số 3 nếu đã xóa
  reserved 5, 6;         // Đặt reserved để tránh tương lai ai dùng nhầm
  reserved "old_field";  // Reserved cả tên
}
```
- Rule: **Chỉ ADD mới, không DELETE, không RENAME, không đổi type.**

---

### ❓ gRPC vs REST vs Kafka — khi nào dùng cái nào trong hệ thống banking?

| Tình huống | Lựa Chọn | Lý do |
|---|---|---|
| Angular → Backend API | **REST/JSON** | Browser native, dễ debug, OpenAPI |
| Backend → Backend sync call | **gRPC** | Low latency, type-safe, binary |
| Backend → Backend async event | **Kafka** | Decoupling, persistence, replay |
| Backend → Banking Partner (NAPAS) | **SOAP/REST** | Partner dictates protocol |
| Mobile → Backend | **REST** | Firewall-friendly, native HTTP |
| Admin Portal → Metrics | **WebSocket** | Real-time bidirectional stream |
| Internal batch reconciliation | **gRPC streaming** | High throughput, backpressure |

---

## 20. Database Optimization — Bảng Tỷ Record

---

### ❓ Nếu bảng `ledger_entries` có tỷ record, em tối ưu query như thế nào?

**Tại sao KHÔNG chỉ thêm index đơn giản:**
- Bảng tỷ record: Index B-Tree chuẩn vẫn hữu ích nhưng không đủ. VACUUM, ANALYZE, AUTOVACUUM cần được tune riêng. Index rebuild tốn hàng giờ.

**Chiến lược tối ưu theo tầng (Layer-by-Layer):**

**Tầng 1 — Index Design:**
```sql
-- Index kép cho query phổ biến nhất: lọc theo account + sắp xếp theo thời gian
CREATE INDEX CONCURRENTLY idx_ledger_account_created
  ON ledger_entries (account_id, created_at DESC)
  WHERE status = 'POSTED'; -- Partial index: chỉ index POSTED entries

-- CONCURRENTLY: Tạo index không lock table (critical cho production)

-- Index cho cursor pagination
CREATE INDEX CONCURRENTLY idx_ledger_id_desc
  ON ledger_entries (id DESC); -- Covering index cho cursor-based pagination
```

**Tầng 2 — Table Partitioning (quan trọng nhất với tỷ record):**
```sql
-- Range Partitioning theo tháng
CREATE TABLE ledger_entries (
  id          BIGSERIAL,
  account_id  UUID NOT NULL,
  amount      DECIMAL(19,4),
  entry_type  VARCHAR(10),
  created_at  TIMESTAMPTZ NOT NULL,
  -- ...
) PARTITION BY RANGE (created_at);

-- Tạo partition từng tháng (tự động qua script/cron)
CREATE TABLE ledger_entries_2026_09
  PARTITION OF ledger_entries
  FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

CREATE TABLE ledger_entries_2026_10
  PARTITION OF ledger_entries
  FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

-- Lợi ích:
-- Query WHERE created_at >= '2026-09-01' → Chỉ scan 1 partition nhỏ
-- DROP cả partition cũ (quý trước) → nhanh hơn DELETE hàng triệu lần
-- VACUUM chạy per-partition → ít overhead hơn
```

**Tầng 3 — Archiving & Cold Storage:**
```sql
-- Dữ liệu > 1 năm → Move sang bảng archive (S3 + Parquet hoặc read-only replica)
-- ledger_entries_archive: Chỉ dùng cho audit/reporting, không query realtime
-- Giữ ledger_entries chỉ chứa 12 tháng gần nhất → Query nhanh hơn nhiều
```

**Tầng 4 — CQRS Read Model:**
- `ledger_entries` là write-optimized (ACID, normalized).
- Tạo `account_monthly_summary` (denormalized): Tổng DEBIT/CREDIT theo tháng, số dư cuối tháng.
- Dashboard query → Read từ summary table (hàng triệu) thay vì scan ledger (tỷ records).

---

### ❓ `EXPLAIN ANALYZE` cho thấy Seq Scan dù đã có index — tại sao?

**Các nguyên nhân phổ biến:**

**Nguyên nhân 1: Query không dùng được index**
```sql
-- ❌ Function wrap làm index vô hiệu:
WHERE EXTRACT(YEAR FROM created_at) = 2026
-- PostgreSQL không thể dùng index trên created_at vì có EXTRACT()

-- ✅ Viết lại:
WHERE created_at >= '2026-01-01' AND created_at < '2027-01-01'
-- Bây giờ PostgreSQL dùng index range scan
```

**Nguyên nhân 2: Statistics lỗi thời**
```sql
-- Chạy sau khi load lượng lớn data:
ANALYZE ledger_entries;
-- Hoặc:
VACUUM ANALYZE ledger_entries;
-- PostgreSQL Query Planner dùng statistics để quyết định plan — stats cũ → plan sai
```

**Nguyên nhân 3: Cardinality quá thấp**
- Nếu 90% rows có `status = 'POSTED'`, query `WHERE status = 'POSTED'` → PostgreSQL chọn Seq Scan vì rẻ hơn Index Scan + heap fetch ngẫu nhiên.
- Giải pháp: Dùng Partial Index chỉ index `status = 'PENDING'` (minority).

**Nguyên nhân 4: `pg_stats` correlation thấp**
- Nếu data được insert theo thứ tự ngẫu nhiên, Heap fetch sau Index Scan là random I/O.
- Giải pháp: `CLUSTER ledger_entries USING idx_ledger_account_created;` — Sắp xếp lại physical data theo index. Chạy offline.

---

### ❓ Database Sharding là gì? BankX có cần không?

**Tại sao KHÔNG cần Sharding ngay:**
- Sharding (chia data ra nhiều DB nodes dựa trên shard key) giải quyết write scalability khi 1 PostgreSQL không đủ.
- Vertical Scaling (RAM, CPU, NVMe SSD): PostgreSQL với 128GB RAM + NVMe có thể xử lý hàng trăm triệu rows tốt hơn nhiều người nghĩ.
- Partitioning + Read Replica thường đủ cho banking tier đến ~10 tỷ records.
- Sharding tăng complexity: Cross-shard transaction (Distributed Transaction = SAGA), Join giữa shards không thể, shard rebalancing khó.

**Tại sao NÊN biết Sharding khi thực sự cần:**
- **Shard Key Design là critical:** Shard theo `customer_id` → Mọi giao dịch của cùng customer ở cùng shard → Cross-shard transaction ít hơn.
- **Consistent Hashing:** Khi thêm shard mới, chỉ cần migrate ~1/N data thay vì rehash tất cả.
- **Vitess (YouTube's MySQL sharding layer) hoặc Citus (PostgreSQL sharding):** Không cần tự implement sharding logic.
- BankX quy mô: Partitioning đã đủ. Sharding chỉ khi > 100 tỷ records hoặc write TPS > 100K.

---

### ❓ Nếu có query lấy "Top 10 tài khoản có số dư lớn nhất" trên bảng 100 triệu records — tối ưu thế nào?

**Ngây thơ:**
```sql
SELECT account_id, balance FROM bank_accounts
ORDER BY balance DESC LIMIT 10;
-- Full sort 100 triệu rows → Rất chậm nếu không có index
```

**Tối ưu Tầng 1 — Index:**
```sql
CREATE INDEX idx_accounts_balance_desc ON bank_accounts (balance DESC);
-- PostgreSQL dùng Index Scan → Lấy 10 records đầu ngay, không cần sort
-- COST: O(1) thay vì O(N log N)
```

**Tối ưu Tầng 2 — Materialized View (nếu query chạy thường xuyên):**
```sql
-- Tính sẵn, refresh mỗi giờ:
CREATE MATERIALIZED VIEW top_balance_accounts AS
  SELECT account_id, balance, updated_at
  FROM bank_accounts
  ORDER BY balance DESC
  LIMIT 100;

CREATE UNIQUE INDEX ON top_balance_accounts (account_id);

-- Refresh (non-blocking với CONCURRENTLY):
REFRESH MATERIALIZED VIEW CONCURRENTLY top_balance_accounts;

-- Query Materialized View → Instant (100 rows, không cần scan)
SELECT * FROM top_balance_accounts LIMIT 10;
```

**Tối ưu Tầng 3 — Cache Redis:**
- Kết quả top 10 → Cache Redis TTL 1 giờ → Dashboard query không hit DB.

---

### ❓ N+1 Query Problem là gì? Trong JPA/Hibernate xảy ra thế nào?

**Mô tả vấn đề:**
```java
// Lấy 100 tài khoản → 1 query
List<BankAccount> accounts = accountRepo.findAll(); // Query 1

for (BankAccount acc : accounts) {
  // Mỗi tài khoản lại query để lấy owner → 100 queries riêng lẻ!
  Customer owner = acc.getCustomer(); // Lazy loading = Query N (100 lần)
  System.out.println(owner.getName());
}
// Tổng: 1 + 100 = 101 queries thay vì 1 query với JOIN
```

**Giải pháp trong BankX:**
```java
// Solution 1: JPQL JOIN FETCH
@Query("SELECT a FROM BankAccount a JOIN FETCH a.customer WHERE a.status = 'ACTIVE'")
List<BankAccount> findAllWithCustomer();
// 1 query với JOIN thay vì N+1

// Solution 2: @EntityGraph (declarative)
@EntityGraph(attributePaths = {"customer", "customer.kycDocuments"})
List<BankAccount> findByStatus(String status);

// Solution 3: @BatchSize (khi không muốn EAGER load tất cả)
@BatchSize(size = 50)
@OneToMany(fetch = FetchType.LAZY)
private List<LedgerEntry> entries;
// Thay vì N queries → ceil(N/50) queries — Batch loading

// Detection: spring.jpa.show-sql=true + đếm queries trong log
// Tool: Hibernate Statistics, datasource-proxy library
```

---

## 21. Security & OWASP Top 10 trong Banking

---

### ❓ SQL Injection trong banking — tại sao JPA không đủ?

**Tại sao KHÔNG chỉ tin tưởng JPA là safe:**
- JPA/JPQL với parameters an toàn. NHƯNG nhiều developer "raw native query":
```java
// ❌ NGUY HIỂM — String concatenation trong native query:
@Query(value = "SELECT * FROM accounts WHERE account_number = '" + accountNumber + "'",
       nativeQuery = true)
// accountNumber = "'; DROP TABLE accounts; --" → SQL Injection!
```

**Tại sao NÊN áp dụng nhiều lớp:**
```java
// ✅ Parameterized query — luôn dùng ?1 hoặc :param:
@Query(value = "SELECT * FROM accounts WHERE account_number = :accountNum",
       nativeQuery = true)
BankAccount findByNumber(@Param("accountNum") String accountNum);

// Validation layer — Spring Validation trên DTO:
@Pattern(regexp = "^[0-9]{10,14}$", message = "Số tài khoản chỉ chứa 10-14 chữ số")
private String accountNumber;
// Reject bất kỳ input không phải số → SQL injection không có cơ hội

// Principle of Least Privilege — DB user chỉ có SELECT/INSERT/UPDATE
-- Không có DROP, CREATE, TRUNCATE quyền cho application user
```

---

### ❓ Mass Assignment Attack là gì? JPA Entity vs DTO giải quyết thế nào?

**Vấn đề:**
```java
// ❌ Nhận JPA Entity trực tiếp từ request body:
@PostMapping("/accounts")
public ResponseEntity<?> create(@RequestBody BankAccountJpaEntity account) {
  // User có thể gửi: {"accountNumber": "...", "balance": 9999999, "status": "ADMIN"}
  // Hibernate save nguyên object → User tự set balance và role!
}
```

**Giải pháp với DTO:**
```java
// ✅ Chỉ accept DTO với field được whitelist:
public record CreateAccountRequest(
  @NotBlank String accountName      // Chỉ user được set tên
  // balance, status, version, customerId → KHÔNG có trong DTO
  // Backend tự set theo business logic
) {}

@PostMapping("/accounts")
public ResponseEntity<?> create(@RequestBody @Valid CreateAccountRequest req) {
  BankAccount account = new BankAccount();
  account.setAccountName(req.accountName());
  account.setBalance(BigDecimal.ZERO);   // Backend set, không từ user
  account.setStatus("ACTIVE");            // Backend set
}
```

---

### ❓ IDOR (Insecure Direct Object Reference) trong banking — ví dụ và phòng tránh?

**Ví dụ tấn công:**
```
User A (customerId: cust-001) có account: acc-001
URL: GET /api/v1/accounts/acc-002/ledger-entries
→ Nếu chỉ check accountId mà không check ownership → User A thấy ledger của User B!
```

**Phòng tránh trong BankX:**
```java
// AccountService — luôn check ownership:
public List<LedgerEntry> getLedgerEntries(String accountId, String currentUserId) {
  BankAccount account = accountRepo.findById(accountId)
    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

  // QUAN TRỌNG: Verify ownership
  if (!account.getCustomerId().equals(currentUserId)) {
    throw new AccessDeniedException("Bạn không có quyền xem tài khoản này");
    // Trả về 403, không 404 → Không leak thông tin tài khoản tồn tại hay không
  }

  return ledgerRepo.findByAccountId(accountId);
}

// Hoặc dùng Spring Security @PreAuthorize:
@PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication.name)")
public List<LedgerEntry> getLedgerEntries(@PathVariable String accountId) { ... }
```

---

### ❓ XSS (Cross-Site Scripting) Attack trong Angular Banking App — tại sao Angular an toàn hơn?

**Angular có built-in XSS protection:**
```typescript
// Angular AUTO-ESCAPE tất cả interpolation:
// Nếu user gửi: username = "<script>alert('XSS')</script>"

// Template:
<span>{{ username }}</span>
// Angular render ra: <span>&lt;script&gt;alert('XSS')&lt;/script&gt;</span>
// → Text hiển thị, không execute script

// ❌ Cách nguy hiểm (bypass sanitization):
<div [innerHTML]="userContent"></div>
// Nếu userContent chứa script → XSS! Angular sẽ WARN trong console.

// ✅ Khi cần render HTML từ trusted source:
import { DomSanitizer } from '@angular/platform-browser';
const sanitized = this.sanitizer.bypassSecurityTrustHtml(trustedContent);
// Chỉ dùng khi thực sự tin tưởng source
```

---

### ❓ CSRF Attack trong Spring + Angular — tại sao REST API không cần CSRF token?

**Tại sao REST API + JWT không cần CSRF:**
- CSRF exploit the browser's automatic cookie sending.
- Nếu auth dùng **JWT trong Authorization header** (không phải Cookie) → Browser không tự động gửi header → Attacker không thể forge authenticated request.
- `localStorage.getItem('token')` → JavaScript của attacker page không thể đọc từ domain khác (Same-Origin Policy).

**Nhưng nếu dùng HttpOnly Cookie cho JWT:**
```java
// Spring Security config:
http.csrf(csrf -> csrf
  .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
  // Angular đọc XSRF-TOKEN cookie → gửi X-XSRF-TOKEN header
);

// application.yml:
# Nếu vẫn muốn CSRF protection với Cookie-based auth
spring.security.csrf.enabled: true
```

---

## 22. Testing Strategy trong Banking

---

### ❓ Tại sao viết Unit Test cho Service Layer mà không phải cho Controller Layer?

**Tại sao KHÔNG tập trung test Controller:**
- Controller chỉ là thin layer: Nhận HTTP request → gọi Service → serialize response. Logic gần như không có → Testing không mang lại nhiều giá trị.
- Integration test (MockMvc) cho Controller có ý nghĩa hơn Unit Test.

**Tại sao NÊN tập trung Unit Test ở Service + Domain:**
```java
// LedgerService Unit Test — Test business rule "Double-Entry phải cân bằng":
@Test
void recordDoubleEntry_shouldThrow_whenDebitDoesNotEqualCredit() {
  // Arrange
  DoubleEntryCommand cmd = new DoubleEntryCommand(
    "acc-001", "acc-002",
    new BigDecimal("1000.00"),
    new BigDecimal("999.99")  // Cố tình sai số
  );

  // Act & Assert
  assertThrows(LedgerImbalanceException.class,
    () -> ledgerService.recordDoubleEntry(cmd));
}

// Dùng Mockito để mock dependencies:
@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {
  @Mock
  private LedgerEntryRepository ledgerRepo;

  @InjectMocks
  private LedgerService ledgerService;
  // Service được test với mocked repository — không cần DB thật
}
```

---

### ❓ Integration Test với Testcontainers — tại sao không dùng H2 in-memory?

**Tại sao KHÔNG dùng H2:**
- H2 không hoàn toàn tương thích PostgreSQL: Một số PostgreSQL-specific feature (Partial Index, `ON CONFLICT`, native functions) không work trên H2.
- Bugs chỉ xuất hiện trên production (PostgreSQL) mà không bắt được trên H2 test.
- Flyway migration scripts PostgreSQL-specific có thể fail trên H2.

**Tại sao NÊN dùng Testcontainers:**
```java
@SpringBootTest
@Testcontainers
class TransferServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
    .withDatabaseName("bankx_test")
    .withUsername("test")
    .withPassword("test");
  // Chạy PostgreSQL thật trong Docker container — tự động start/stop

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void internalTransfer_shouldDebitAndCreditCorrectly() {
    // Test với PostgreSQL thật → Bắt được bug PostgreSQL-specific
    // Flyway migration cũng chạy thật
  }
}
```

---

### ❓ Contract Testing (Pact) là gì? Khi nào cần trong banking?

**Tại sao KHÔNG chỉ dùng Mock responses:**
- Mock server tự tạo có thể không đồng bộ với API thật của partner (NAPAS, external bank).
- Khi partner thay đổi response format → Mock vẫn pass → Production fail.

**Tại sao NÊN dùng Consumer-Driven Contract Testing (Pact):**
```java
// Consumer (BankX) định nghĩa expectations:
// "Tôi expect khi gọi GET /napas/accounts/{id} → nhận JSON có field 'accountHolder'"

// Pact verify rằng Provider (NAPAS Stub) thỏa mãn contract đó.
// Khi NAPAS thay đổi API → Pact Contract test fail ngay trong CI/CD
// → Phát hiện breaking change trước khi deploy production
```

---

## 23. Microservices & Deployment Patterns

---

### ❓ Blue-Green Deployment khác với Rolling Deployment thế nào trong banking?

**Blue-Green Deployment:**
```
Blue (v1.0) đang chạy: 100% traffic
Deploy Green (v1.1)  → Test kỹ → Switch traffic 0% → 100% sang Green ngay lập tức
                                                        ↑ Zero downtime

Rollback: Switch traffic ngay về Blue trong vài giây → Không cần redeploy
```

**Rolling Deployment:**
```
Instance 1: v1.0 → v1.1 (upgrade dần)
Instance 2: v1.0 → v1.1
Instance 3: v1.0 → v1.1
Rollback: Phải deploy lại v1.0 → Tốn thời gian hơn
```

**Canary Deployment:**
```
95% traffic → v1.0 (stable)
5%  traffic → v1.1 (canary — chỉ nhóm nhỏ user dùng trước)
Monitor errors/latency → Nếu OK → Tăng dần lên 100%
→ Giảm risk nhất, nhưng phức tạp nhất
```

**Banking thường dùng:** Blue-Green vì rollback instant khi có critical bug ảnh hưởng tiền bạc.

---

### ❓ Database Migration trong Zero-Downtime Deployment — làm thế nào?

**Vấn đề:** Deploy mới yêu cầu đổi schema DB → Nhưng không thể down DB khi đang có giao dịch

**Pattern: Expand-Contract (phải làm 3 deploy):**

```sql
-- Deploy 1: EXPAND — Thêm column mới (backward compatible)
ALTER TABLE bank_transfers ADD COLUMN fee_amount DECIMAL(19,4) DEFAULT 0;
-- Column mới, nullable/default → App v1 vẫn chạy bình thường

-- Code Deploy v2: Viết vào cả column cũ lẫn mới
-- Đọc từ column mới nếu có, fallback column cũ

-- Deploy 2: Backfill data
UPDATE bank_transfers SET fee_amount = amount * 0.001 WHERE fee_amount = 0;
-- Chạy background job, không lock table

-- Deploy 3: CONTRACT — Remove column cũ (sau khi v2 stable)
ALTER TABLE bank_transfers DROP COLUMN old_column;
```

**Flyway áp dụng:**
- Migration được Flyway track version → Chạy tự động khi app start.
- `CONCURRENTLY` index creation không lock table.
- Tuyệt đối không `DROP COLUMN` trong cùng deploy với feature code sử dụng column mới.

---

### ❓ Health Check endpoint `/actuator/health` trả về gì? Tại sao quan trọng?

```java
// BankX application.yml:
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized  # Ẩn details với anonymous user

// Response của /actuator/health:
{
  "status": "UP",  // UP, DOWN, OUT_OF_SERVICE, UNKNOWN
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "PostgreSQL", "result": 1 }
    },
    "redis": {
      "status": "UP",
      "details": { "version": "7.2.0" }
    },
    "kafka": {
      "status": "UP",
      "details": { "clusterId": "abc123" }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "free": "50GB" }
    }
  }
}
```

**Tại sao quan trọng:**
- **Kubernetes Liveness Probe:** K8s gọi `/health` mỗi 30s. Status `DOWN` → K8s restart container.
- **Kubernetes Readiness Probe:** `/health/readiness` → K8s chỉ route traffic khi `UP`. App đang warmup/migration → Readiness DOWN → Không nhận request.
- **Load Balancer:** HAProxy/Nginx gọi health check → Remove unhealthy instance khỏi pool.
- **Alert:** PagerDuty/OpsGenie monitor health endpoint → Alert on-call engineer khi `DOWN`.

---

## 24. Câu Hỏi Mẹo & Bẫy (Tricky Questions)

---

### ❓ `@Transactional` có hoạt động khi gọi method trong cùng class không?

**Tại sao KHÔNG (self-invocation problem):**
```java
@Service
public class TransferService {

  // Phương thức A — KHÔNG @Transactional
  public void processTransfer(Transfer t) {
    this.executeWithRetry(t); // ← Gọi method B trong CÙNG class
  }

  @Transactional  // ← Transaction này SẼ KHÔNG ĐƯỢC APPLY!
  public void executeWithRetry(Transfer t) {
    // Spring AOP proxy không intercept self-invocation
    // → @Transactional bị bỏ qua hoàn toàn
  }
}
```

**Tại sao xảy ra:**
- Spring `@Transactional` hoạt động qua AOP Proxy. Khi A gọi B trong cùng class → gọi trực tiếp `this.B()` không qua proxy → Aspect không chạy.

**Giải pháp:**
```java
// Solution 1: Tách ra service riêng
@Service
public class TransferRetryService {
  @Transactional
  public void executeWithRetry(Transfer t) { ... }
}

// Solution 2: Self-inject (không đẹp nhưng hoạt động)
@Service
public class TransferService {
  @Autowired
  private TransferService self; // Inject chính mình qua Spring proxy

  public void processTransfer(Transfer t) {
    self.executeWithRetry(t); // Qua proxy → @Transactional hoạt động
  }
}

// Solution 3: Dùng ApplicationContext.getBean() (tương tự)
```

---

### ❓ `@Transactional(readOnly = true)` làm gì khác biệt?

**Tại sao NÊN dùng `readOnly = true` cho query methods:**
```java
@Transactional(readOnly = true)
public List<BankAccount> getMyAccounts(String customerId) {
  return accountRepo.findByCustomerId(customerId);
}
// Tại sao tốt hơn?
// 1. Hibernate tắt dirty checking → Không track thay đổi entity → Tiết kiệm memory + CPU
// 2. PostgreSQL nhận hint "read-only transaction" → Route sang Read Replica nếu có
// 3. Tránh accidental write: Nếu code trong method có save() → Exception ngay
// 4. Performance improvement ~30% với large object graphs
```

---

### ❓ `Optional.get()` không an toàn — tại sao và cách thay thế?

```java
// ❌ Nguy hiểm:
BankAccount account = accountRepo.findById(id).get(); // NoSuchElementException nếu null

// ✅ Cách đúng trong BankX:
BankAccount account = accountRepo.findById(id)
  .orElseThrow(() -> new ResourceNotFoundException(
    "Tài khoản không tồn tại: " + id  // Message có context
  ));

// Hoặc:
Optional<BankAccount> opt = accountRepo.findById(id);
if (opt.isEmpty()) {
  throw new ResourceNotFoundException("...");
}
BankAccount account = opt.get(); // An toàn vì đã check

// Với default value:
BigDecimal balance = accountRepo.findById(id)
  .map(BankAccount::getBalance)
  .orElse(BigDecimal.ZERO);
```

---

### ❓ Tại sao `LocalDate` tốt hơn `java.util.Date` trong banking?

```java
// java.util.Date — Vấn đề:
// 1. Mutable → Thread-unsafe
// 2. Month bắt đầu từ 0 (January = 0!) → Bug muôn thuở
// 3. Không có timezone concept rõ ràng
// 4. Deprecated từ Java 1.1

// java.time.LocalDate / LocalDateTime / ZonedDateTime — Tốt hơn:
LocalDate transactionDate = LocalDate.of(2026, 9, 16); // Tháng 9 là 9, không phải 8
ZonedDateTime transactionTime = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
// Immutable → Thread-safe

// Trong BankX + PostgreSQL + JPA:
@Column(name = "created_at")
private ZonedDateTime createdAt; // TIMESTAMPTZ trong PostgreSQL
// @CreationTimestamp của Hibernate auto-set khi INSERT
```

---

### ❓ `HashMap` trong multi-threaded banking service có vấn đề gì?

```java
// ❌ HashMap trong concurrent environment:
// HashMap không thread-safe → Race condition → Data corruption, infinite loop trong resize

// BankX dùng:
// ConcurrentHashMap — Thread-safe HashMap:
private final Map<String, CircuitBreakerState> circuitStates
  = new ConcurrentHashMap<>();

// Trong banking rate limiter:
private final Map<String, AtomicInteger> requestCounts
  = new ConcurrentHashMap<>();
// AtomicInteger cho thread-safe increment mà không lock toàn bộ map

// Collections.synchronizedMap() — Cũ, lock toàn bộ map → Chậm hơn ConcurrentHashMap
// → KHÔNG dùng nếu có nhiều concurrent read/write
```

---

### ❓ Memory Leak trong Spring Banking Service — các nguyên nhân phổ biến?

```java
// Nguyên nhân 1: Static collection tích lũy mãi mãi
public class FraudRuleCache {
  private static final Map<String, Rule> RULE_CACHE = new HashMap<>();
  // Static Map sống với class loader → Không bao giờ GC → Memory leak nếu key tăng mãi

  // Fix: Dùng Caffeine/Guava Cache với expiration:
  private static final Cache<String, Rule> RULE_CACHE = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();
}

// Nguyên nhân 2: Thread-local không cleanup
ThreadLocal<String> currentTraceId = new ThreadLocal<>();
// Trong Thread Pool (server environment), thread được reuse
// Nếu không remove() sau request → ThreadLocal tích lũy trong thread pool

// Fix:
try {
  currentTraceId.set(traceId);
  // process...
} finally {
  currentTraceId.remove(); // LUÔN cleanup trong finally
}

// Nguyên nhân 3: RxJS/Subscription chưa unsubscribe (Angular)
// Xem Bài 09 Angular
```

---

### ❓ Tại sao không nên log password, card number hay JWT token?

**Tại sao KHÔNG log sensitive data:**
```java
// ❌ NGUY HIỂM:
log.info("User login: username={}, password={}", username, password);
log.info("Transfer request: {}", requestBody.toString()); // requestBody có card number!
log.info("Auth token: {}", jwtToken);

// Logs thường được:
// - Gửi sang ELK Stack (Elasticsearch) → Index → Searchable
// - Lưu trên nhiều server khác nhau
// - Dễ bị xem bởi team Ops
// → Password/card số trong log = vi phạm PCI-DSS, GDPR
```

**Giải pháp:**
```java
// ✅ Mask sensitive fields:
log.info("User login: username={}", username); // Chỉ log username, không password

// Custom Masking trong Jackson:
@JsonSerialize(using = MaskedSerializer.class)
private String cardNumber; // Serialize ra "4111 11** **** 1111" trong log

// Log DTO thay vì raw object:
log.info("Transfer: amount={}, fromAccount={}, traceId={}",
  request.getAmount(),
  maskAccount(request.getFromAccount()), // "0888****01"
  MDC.get("traceId")); // Safe to log

// Spring Security — Mặc định mask password trong PasswordEncoder logs
```

---

## 25. Scalability & System Design

---

### ❓ Thiết kế hệ thống chịu 100K TPS cho bảng bank_transfers — em làm gì?

**Approach theo tầng (đây là câu hỏi System Design mở — trình bày tư duy):**

**Tầng 1 — Database:**
- Write-Optimized PostgreSQL: Disable FSM autovacuum aggressiveness, tune `checkpoint_completion_target=0.9`, `wal_buffers=64MB`.
- Table Partitioning theo ngày/tuần trên `bank_transfers`.
- UNLOGGED tables cho in-flight saga state (không cần WAL durability với state tạm thời).

**Tầng 2 — Application:**
- Java 21 Virtual Threads: Không block OS thread khi chờ DB.
- Async Kafka producer với batching: `linger.ms=5`, `batch.size=16384` → Kafka batch nhiều messages.
- Connection Pool: HikariCP `maximumPoolSize = (vCPU * 2) + 1`.

**Tầng 3 — Architecture:**
- Write Path: Load Balancer → API Gateway → Multiple Core Service instances → PostgreSQL Primary.
- Read Path: Core Service → Read Replica (hot reads) hoặc Redis Cache (very hot reads).
- Kafka async processing: Heavy operations (Notification, CQRS projection) off critical path.

**Tầng 4 — Infrastructure:**
- NVMe SSD cho PostgreSQL WAL journal.
- Dedicated IOPs volume cho data files.
- `pg_bouncer` connection pooler trước PostgreSQL (transaction mode).

---

### ❓ Rate Limiting được implement thế nào ở API Gateway level?

**Token Bucket Algorithm (Resilience4j dùng):**
```
Bucket có N tokens.
Mỗi request consume 1 token.
Tokens được refill với tốc độ R tokens/giây.
Khi bucket rỗng → Request bị reject (429 Too Many Requests).

Ưu điểm: Cho phép burst (dùng hết tokens cùng lúc) nhưng không vượt tốc độ trung bình.
Dùng cho: API endpoint giới hạn số lần gọi.
```

**Fixed Window Counter:**
```
Đếm requests trong mỗi window 1 phút.
Nếu > 100 requests → Reject.
Nhược điểm: Boundary problem — 100 req cuối phút + 100 req đầu phút kế = 200 req trong 2 giây.
```

**Sliding Window Log:**
```
Lưu timestamp của mỗi request trong Redis Sorted Set.
ZRANGEBYSCORE key (now-60s) +inf → Đếm requests trong 60s gần nhất.
Chính xác nhất nhưng tốn memory nhất.
```

**BankX áp dụng:**
```java
// application.yml:
resilience4j:
  ratelimiter:
    instances:
      interbank-transfer:
        limit-for-period: 5        # 5 requests
        limit-refresh-period: 1s   # Mỗi giây
        timeout-duration: 0s       # Không chờ — reject ngay

// Phân tầng Rate Limit:
// 1. API Gateway: 1000 req/min per IP (DDoS protection)
// 2. Auth endpoint: 5 attempts/min per user (Brute force protection)
// 3. Interbank transfer: 5 req/s (external bank API rate limit)
// 4. OTP send: 3 attempts/5min per phone (SMS spam protection)
```

---

### ❓ Nếu Kafka broker down, BankX có mất message không?

**Tại sao KHÔNG mất message:**
- **Transactional Outbox Pattern** đảm bảo message được ghi vào `outbox_events` trong cùng DB transaction với transfer.
- Nếu Kafka broker down → `OutboxPollingService` vẫn poll mỗi 2s → Thử publish → Kafka down → Retry sau 2s → Kafka up → Publish ngay.
- Message không bị mất vì nằm trong PostgreSQL (durable) chứ không chỉ trong memory.

**Kafka Producer config cho durability:**
```java
// application.yml:
spring.kafka.producer:
  acks: all                # Chờ tất cả in-sync replicas acknowledge
  retries: 3               # Retry 3 lần nếu broker không ack
  enable-idempotence: true # Kafka Producer không duplicate khi retry
  transaction-id-prefix: bankx-producer- # Kafka transactions (nếu dùng)
```

**Kafka Replication:**
- `replication.factor: 3` → 3 broker giữ bản sao → 1 broker down → 2 còn lại vẫn serve.
- `min.insync.replicas: 2` → Phải có ít nhất 2 replicas sync trước khi ack → Data không bị mất kể cả khi 1 broker fail lúc đang write.

---

*Tài liệu được tổng hợp từ toàn bộ kiến trúc và code thực tế của Titan BankX Digital Banking Platform (27 Sprints).  
Cập nhật: Tháng 9/2026 — Thêm: gRPC, Database tỷ record, Security, Testing, Tricky Questions, Scalability.*
