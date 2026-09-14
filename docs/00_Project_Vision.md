# BankX Digital Banking Platform — Tầm Nhìn Dự Án (Project Vision)

Tài liệu này xác định tầm nhìn, phạm vi nghiệp vụ, mục tiêu kỹ thuật và các ràng buộc cốt lõi của **BankX Digital Banking Platform** — dự án ngân hàng số mô phỏng TPBank, được xây dựng để phục vụ mục tiêu học tập, luyện tập phỏng vấn Java Senior và khám phá kiến trúc enterprise banking.

Đây là kim chỉ nam để lập trình viên và AI Agents (Gemini, Claude, Cursor) đưa ra quyết định thiết kế nhất quán trong suốt các Sprint.

---

## 1. Tuyên Bố Tầm Nhìn (Vision Statement)

> "Xây dựng một **Digital Banking Platform** thu nhỏ nhưng đầy đủ tư duy, mô phỏng TPBank — đủ để giải thích được mọi câu hỏi phỏng vấn ngân hàng cấp Senior Java."

**BankX** không phải là một CRUD app. Đây là một hệ thống ngân hàng có:

- **Banking Core:** Ledger kép (Double-Entry), Balance concurrency control, Idempotency
- **Distributed Systems:** Event-driven qua Kafka, Outbox Pattern, Saga, Distributed Lock
- **Security:** JWT, Refresh Token Rotation, OTP, RBAC, Rate Limiting, Fraud Detection
- **Observability:** Distributed Tracing, Structured Logging, Metrics Dashboard
- **Mobile-Ready:** Angular + Ionic/Capacitor cho iOS/Android
- **Clean Architecture:** DDD, Hexagonal Architecture, CQRS cho Transaction History

---

## 2. Mục Tiêu Kỹ Thuật (Engineering Objectives)

### Các bài toán cốt lõi cần giải quyết:

| Bài Toán | Giải Pháp |
|---|---|
| User bấm chuyển tiền 2 lần | Idempotency Key + Redis SETNX |
| Hai request rút tiền đồng thời | Optimistic Lock (`@Version`) / SELECT FOR UPDATE |
| Kafka publish fail sau DB commit | Transactional Outbox Pattern |
| Consumer Kafka chết giữa chừng | At-Least-Once + Idempotent Consumer |
| Transfer sang service khác thất bại | Saga Orchestration + Compensating Transaction |
| Token bị đánh cắp | JWT Refresh Token Rotation + Token Revocation |
| Tài khoản bị tấn công brute force | Rate Limit + Account Lock + OTP |
| Giao dịch đáng ngờ số tiền lớn | Fraud Detection Rule Engine |
| Transaction thất bại tại service nào? | Distributed Tracing (OpenTelemetry + Jaeger) |

### Chỉ số hiệu năng mục tiêu:

- **Transfer API:** P99 < 300ms (bao gồm OTP flow)
- **Balance Query (cached):** P99 < 20ms (Redis Cache Aside)
- **Transaction History (CQRS Read):** P99 < 50ms
- **Throughput:** Hệ thống xử lý được 1.000+ giao dịch/giây trên single node
- **Error Rate:** < 0.1% dưới tải bình thường

---

## 3. Phạm Vi Nghiệp Vụ (Scope)

### ✅ Nằm trong phạm vi (In-Scope)

#### Khách hàng (Customer Portal — Angular Web + Ionic Mobile):
1. **Authentication:** Đăng nhập username/password, OTP, Face ID (Capacitor Plugin), Biometric
2. **Dashboard:** Số dư, giao dịch gần đây, shortcuts nhanh
3. **Tài khoản (Account):** Danh sách tài khoản, sao kê, mở tài khoản mới
4. **Chuyển tiền (Transfer):** Nội bộ ngân hàng, liên ngân hàng, xác nhận OTP
5. **Thanh toán hóa đơn (Bill Payment):** Điện, nước, internet, di động (Strategy Pattern)
6. **QR Payment:** Quét mã QR thanh toán, tạo QR nhận tiền
7. **Thẻ (Card):** Danh sách thẻ, khoá/mở thẻ, virtual card
8. **Thụ hưởng (Beneficiary):** Quản lý danh sách người nhận hay dùng
9. **Thông báo (Notification):** Push notification, in-app, email
10. **Ưu đãi (Promotion):** Flash Sale, cashback, voucher ngân hàng
11. **Cá nhân (Profile):** Thông tin KYC, bảo mật, cài đặt
12. **Bảo mật (Security):** Đổi mật khẩu, 2FA, quản lý thiết bị đăng nhập

#### Admin Portal (Angular Web):
13. **Quản lý khách hàng:** KYC approval, block/unblock account
14. **Quản lý giao dịch:** Monitor realtime, tra cứu, dispute handling
15. **Fraud Monitoring:** Cảnh báo giao dịch đáng ngờ, rule management
16. **Audit Logs:** Nhật ký toàn bộ hành động người dùng và admin
17. **Báo cáo (Reporting):** Doanh thu, số lượng giao dịch, biểu đồ

#### Engineering Portal (Angular Web — Sprint cao):
18. **System Topology:** Sơ đồ kiến trúc live
19. **Kafka Monitor:** Lag, offset, consumer group health
20. **Distributed Tracing:** Jaeger trace viewer
21. **Chaos Engineering:** Tắt Redis/DB để test Circuit Breaker
22. **Metrics Dashboard:** Prometheus + Grafana embedded

### ❌ Nằm ngoài phạm vi (Non-Scope)

- **Core Banking thực tế:** Không kết nối NAPAS/SWIFT thật
- **KYC thực tế:** Chỉ mô phỏng quy trình, không gọi eKYC API thật (VNeID)
- **Thuế / Hóa đơn tài chính:** Bỏ qua
- **Giao dịch quốc tế thực tế:** Chỉ mock data
- **Blockchain / DeFi:** Không thuộc phạm vi

---

## 4. Ràng Buộc Công Nghệ (Technology Constraints)

### Backend (Java)
| Thành phần | Công nghệ | Lý do |
|---|---|---|
| Runtime | Java 21 LTS | Virtual Threads, Records, Pattern Matching |
| Framework | Spring Boot 3.3+ | Mature, interview-relevant, ecosystem phong phú |
| Security | Spring Security 6 | JWT, OAuth2, RBAC standard |
| ORM | Spring Data JPA + Hibernate 6 | Optimistic Lock, Auditing |
| Database | PostgreSQL 16 | ACID, partitioning, performance |
| Cache | Redis 7 (Lettuce/Jedis) | OTP TTL, Idempotency, Rate Limit, Session |
| Messaging | Apache Kafka 3.6+ | Event-Driven, Outbox, Saga |
| Migration | Flyway | Controlled schema evolution |
| Resilience | Resilience4j | Circuit Breaker, Retry, Rate Limiter |
| Gateway | Spring Cloud Gateway | Routing, Auth filter, Rate Limit |
| Discovery | Netflix Eureka | Service Registry (Phase 2+) |
| Build | Maven (multi-module) | Monorepo management |
| Container | Docker + Docker Compose | Local dev, CI/CD |
| Observability | OpenTelemetry + Prometheus + Grafana + Jaeger | Full-stack observability |
| Testing | JUnit 5 + Mockito + Testcontainers + REST Assured | Unit, Integration, Contract |

### Frontend (Angular)
| Thành phần | Công nghệ | Lý do |
|---|---|---|
| Framework | Angular 22 (Standalone, Signals) | Target stack cho vị trí banking |
| Mobile | Ionic + Capacitor | Chạy được iOS/Android từ Angular codebase |
| State | NgRx (global), Angular Signals (local) | Scalable state management |
| HTTP | Angular HttpClient + Interceptors | JWT, error handling, loading |
| Routing | Lazy Loading, Route Guards | Performance, RBAC |
| Styling | SCSS + Angular Material | Banking UI, responsive |
| Charts | Chart.js / ApexCharts | Dashboard, Transaction history |
| Testing | Karma + Jasmine + Playwright (E2E) | Quality assurance |

---

## 5. Quy Định Code & Học Tập

> **Mọi kỹ thuật khi xây dựng đều phải trả lời câu hỏi "Tại sao?" thông qua comment inline bằng tiếng Việt.**

1. **Không viết code giả:** `// TODO`, `return null`, `throw new UnsupportedOperationException()` — đều bị cấm tuyệt đối.
2. **Mỗi tính năng một khi vào Sprint phải chạy được** và tích hợp thành công vào Docker Compose.
3. **Sau mỗi Sprint** hệ thống phải compile thành công, pass toàn bộ Unit Test có liên quan.
4. **Tư duy banking:** Mỗi quyết định kiến trúc phải đặt câu hỏi: "Nếu service này chết giữa chừng thì sao?" → trả lời bằng code, không phải lời nói.
5. **Phỏng vấn-ready:** Luôn có thể giải thích được mọi design decision chỉ dùng sơ đồ sequence và whiteboard.
