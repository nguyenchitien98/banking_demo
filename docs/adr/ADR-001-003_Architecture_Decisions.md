# ADR-001: Tại Sao Dùng Modular Monolith Trước Microservices

**Trạng thái:** ✅ Accepted
**Sprint:** Sprint 00 — Foundation
**Ngày:** 2026-09-14

## Ngữ Cảnh (Context)

Đây là dự án banking platform mới, được xây dựng bởi 1 developer (người học) với mục tiêu:
1. Hiểu sâu banking domain (ledger, transfer, OTP, fraud...)
2. Thực hành kiến trúc enterprise Java
3. Chuẩn bị phỏng vấn banking Java Senior

## Vấn Đề (Problem)

Cần quyết định: Bắt đầu với Microservices hay Modular Monolith?

## Các Phương Án Cân Nhắc

### Phương án 1: Full Microservices ngay từ đầu
**Pros:**
- Giống production hơn
- Học nhiều công nghệ hơn

**Cons:**
- Overhead cực lớn: service discovery, distributed tracing, inter-service communication
- Domain boundaries chưa ổn định → tách sai → tốn công refactor
- Debugging khó khi chưa quen domain
- Test phức tạp hơn nhiều
- Một developer khó manage 8+ services cùng lúc

### Phương án 2: Modular Monolith (ĐƯỢC CHỌN) ✅
**Pros:**
- Tập trung học domain banking trước (Ledger, Transfer, OTP, Fraud...)
- Dễ debug, dễ trace code
- Tất cả modules trong 1 transaction boundary → Dễ implement đúng
- Sau khi domain ổn → Tách service theo ranh giới rõ ràng
- Vẫn học được Kafka, Redis, Outbox trong context của monolith

**Cons:**
- Chưa phải microservices thực sự
- Scale up (không phải scale out)

## Quyết Định (Decision)

**Bắt đầu với Modular Monolith** và **trích xuất microservices có chủ đích** sau:

```
Phase 1-2: Modular Monolith (1 Spring Boot app)
          ↓
Phase 3: Tách Notification Service (ít dependency nhất)
          ↓
Phase 4: Tách Auth Service (security-sensitive, isolate)
          ↓
Phase 5+: Tách Transfer + Account (khi domain đã hoàn toàn ổn định)
```

## Hệ Quả (Consequences)

- ✅ Học được domain banking nhanh hơn
- ✅ Code dễ maintain, dễ debug ở giai đoạn đầu
- ✅ Vẫn áp dụng Clean Architecture (sẵn sàng tách service)
- ✅ Vẫn dùng Kafka, Redis (kiến trúc sẵn sàng distributed)
- ⚠️ Phase 3+ cần refactor để tách service — công sức cao hơn nhưng đúng cách

---

# ADR-002: Tại Sao Dùng Outbox Pattern Cho Kafka Events

**Trạng thái:** ✅ Accepted
**Sprint:** Sprint 11
**Ngày:** 2026-09-14

## Vấn Đề (Problem)

Sau khi Transfer hoàn thành, cần notify các consumers (Notification, Audit, Fraud). Hai cách tiếp cận:

**Cách 1 (Sai):**
```java
@Transactional
public void createTransfer() {
    transferRepository.save(transfer);        // DB commit
    kafkaTemplate.send("transfer.completed"); // Kafka publish
}
```
**Rủi ro:** DB commit OK nhưng Kafka publish fail (network issue) → Transfer thành công nhưng không có notification, audit → Data inconsistency

**Cách 2 (Đúng — Outbox Pattern):**
```java
@Transactional
public void createTransfer() {
    transferRepository.save(transfer);
    outboxRepository.save(outboxEvent); // Cùng DB transaction!
}
// Sau đó: Outbox Poller đọc DB → Publish Kafka
```

## Quyết Định

**Dùng Transactional Outbox Pattern** cho tất cả events từ banking operations.

## Hệ Quả

- ✅ Đảm bảo at-least-once delivery
- ✅ DB và Kafka luôn nhất quán
- ✅ Có thể retry publish mà không ảnh hưởng business
- ⚠️ Cần maintain bảng `outbox_events` và Outbox Poller
- ⚠️ Consumer phải idempotent (vì at-least-once có thể duplicate)

---

# ADR-003: Tại Sao Dùng Optimistic Lock Thay Pessimistic Lock Cho Balance

**Trạng thái:** ✅ Accepted
**Sprint:** Sprint 09
**Ngày:** 2026-09-14

## Vấn Đề

Khi 2 request đồng thời update balance cùng 1 tài khoản → Race condition.

## So Sánh

| | Pessimistic Lock | Optimistic Lock |
|---|---|---|
| Cơ chế | `SELECT FOR UPDATE` → Lock DB row | `@Version` → Check version trong UPDATE |
| Blocking | Có — các request khác phải chờ | Không — request thất bại → Retry |
| Throughput | Thấp (serial) | Cao (parallel, retry khi conflict) |
| Deadlock risk | Có (nếu lock nhiều rows không đúng thứ tự) | Không |
| Phù hợp khi | Conflict rate cao | Conflict rate thấp |

## Quyết Định

**Pha 1:** Optimistic Lock (`@Version`) với Retry 3 lần + exponential backoff
**Lý do:** Banking transfers thường ít concurrent (1 user ít khi transfer 2 lần cùng lúc)

**Tương lai (Phase 3+):** Nếu throughput cao → Redis Distributed Lock (Redisson) cho hot accounts

## Hệ Quả

- ✅ Throughput cao hơn Pessimistic
- ✅ Không deadlock
- ⚠️ Cần implement Retry logic
- ⚠️ Nếu conflict rate cao → retry overhead lớn → Switch sang Pessimistic hoặc Redis Lock
