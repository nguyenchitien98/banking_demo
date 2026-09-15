# ❓ Titan BankX — Câu Hỏi & Trả Lời Phỏng Vấn Banking Java Senior

> **Dành cho:** Phỏng vấn Java Senior / Banking Engineer / Distributed Systems  
> **Format:** Câu hỏi thách thức → Tại sao KHÔNG → Tại sao NÊN  
> **Xoay quanh:** Toàn bộ kiến trúc và code thực tế của Titan BankX Platform  

---

## 📋 Mục Lục

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

*Tài liệu được tổng hợp từ toàn bộ kiến trúc và code thực tế của Titan BankX Digital Banking Platform (27 Sprints).*
