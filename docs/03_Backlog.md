# BankX Digital Banking Platform — Backlog & Epic Map

Tài liệu này quản lý toàn bộ User Stories, Epics, và Acceptance Criteria cho **BankX Banking Platform**. Cấu trúc theo phong cách Jira thực tế.

---

## 🗺️ Bản Đồ Màn Hình (Screen Map)

Tham chiếu từ ảnh UI_TPBank.png — 22 màn hình chính:

| Màn Hình | Tên | Epic |
|---|---|---|
| Screen 01 | Login / Đăng nhập | EPIC-01 |
| Screen 02 | OTP Verification | EPIC-01 |
| Screen 03 | Dashboard / Trang chủ | EPIC-02 |
| Screen 04 | Danh sách tài khoản | EPIC-03 |
| Screen 05 | Chi tiết tài khoản + Sao kê | EPIC-03 |
| Screen 06 | Chuyển tiền — Nhập thông tin | EPIC-04 |
| Screen 07 | Chuyển tiền — Xác nhận | EPIC-04 |
| Screen 08 | Chuyển tiền — Nhập OTP | EPIC-04 |
| Screen 09 | Chuyển tiền — Kết quả | EPIC-04 |
| Screen 10 | Thanh toán hóa đơn — Danh mục | EPIC-05 |
| Screen 11 | Thanh toán hóa đơn — Xác nhận | EPIC-05 |
| Screen 12 | Quét mã QR | EPIC-05 |
| Screen 13 | Nạp tiền điện thoại | EPIC-05 |
| Screen 14 | Danh sách thẻ | EPIC-06 |
| Screen 15 | Chi tiết thẻ / Quản lý thẻ | EPIC-06 |
| Screen 16 | Ưu đãi / Promotion | EPIC-07 |
| Screen 17 | Tiện ích / Mini Apps | EPIC-08 |
| Screen 18 | Hồ sơ cá nhân | EPIC-09 |
| Screen 19 | Bảo mật tài khoản | EPIC-09 |
| Screen 20 | Admin — Dashboard | EPIC-10 |
| Screen 21 | Admin — Transaction Monitor | EPIC-10 |
| Screen 22 | Engineering Portal — System Health | EPIC-11 |

---

## 📋 Chi Tiết Epics & User Stories

### 🛡️ EPIC-01: Authentication & Security Foundation
**Mục tiêu:** Xây dựng hệ thống xác thực banking-grade, chống brute force, hỗ trợ Biometric.

#### US-01.1: Đăng nhập bằng Username/Password (Screen 01)
* **Là một:** Khách hàng cá nhân
* **Tôi muốn:** Đăng nhập bằng số điện thoại và mật khẩu
* **Để:** Truy cập vào tài khoản ngân hàng của mình
* **Acceptance Criteria:**
  * Validate phone number format (+84 hoặc 0xxx)
  * Mật khẩu ít nhất 8 ký tự, có chữ hoa, số, ký tự đặc biệt
  * Sau 5 lần sai mật khẩu: Tài khoản bị khóa 30 phút (Rate Limit + Account Lock)
  * JWT Access Token (15 phút) + Refresh Token (7 ngày) được trả về
  * Ghi Audit Log: user_id, IP, device, timestamp, action=LOGIN_SUCCESS/LOGIN_FAILED

#### US-01.2: OTP Verification (Screen 02)
* **Là một:** Khách hàng vừa đăng nhập
* **Tôi muốn:** Xác thực danh tính qua OTP gửi đến SĐT đã đăng ký
* **Để:** Đảm bảo an toàn cho phiên đăng nhập
* **Acceptance Criteria:**
  * OTP 6 chữ số, hết hạn sau 120 giây (TTL trong Redis)
  * Hiện countdown timer trên UI
  * Sau 3 lần nhập sai OTP: Block 5 phút
  * Nút "Gửi lại OTP" sau 60 giây
  * OTP KHÔNG BAO GIỜ được log ra file hoặc response

#### US-01.3: Biometric Login (Mobile only)
* **Là một:** Khách hàng dùng app mobile
* **Tôi muốn:** Đăng nhập nhanh bằng Face ID hoặc Touch ID
* **Để:** Không cần nhập mật khẩu mỗi lần mở app
* **Acceptance Criteria:**
  * Sử dụng Capacitor Biometrics Plugin
  * Lần đầu: Setup biometric + lưu mã xác thực (encrypted token) trong Secure Storage
  * Các lần sau: Biometric verify → lấy token → Call refresh API
  * Nếu biometric fail 3 lần: Fallback về PIN/Password

---

### 🏠 EPIC-02: Dashboard & Account Overview
**Mục tiêu:** Màn hình chính hiển thị toàn bộ thông tin tài chính cá nhân.

#### US-02.1: Dashboard Chính (Screen 03)
* **Là một:** Khách hàng đã đăng nhập
* **Tôi muốn:** Thấy ngay tổng quan tài chính khi vào app
* **Để:** Nắm bắt nhanh số dư và giao dịch gần nhất
* **Acceptance Criteria:**
  * Hiển thị: Tên khách hàng, ảnh avatar
  * Card tài khoản chính: Số tài khoản masked (****1234), số dư, loại tài khoản
  * 4 quick shortcuts: Chuyển tiền, Quét QR, Nạp tiền, Rút tiền
  * Dịch vụ yêu thích (personalized shortcuts, lưu trong user preference)
  * Banner ưu đãi (Promotion Service)
  * 5 giao dịch gần nhất (từ Transaction History Service)
  * Load time < 500ms (balance từ Redis cache, transactions từ read model)

---

### 💰 EPIC-03: Account Management
**Mục tiêu:** Quản lý tài khoản thanh toán, tiết kiệm; xem sao kê chi tiết.

#### US-03.1: Danh Sách Tài Khoản (Screen 04)
* **Là một:** Khách hàng
* **Tôi muốn:** Xem tất cả tài khoản của mình trong 1 màn hình
* **Để:** Biết tổng tài sản và chuyển đổi giữa các tài khoản
* **Acceptance Criteria:**
  * Hiển thị: Tài khoản thanh toán, Tiết kiệm, Thẻ tín dụng
  * Mỗi card: Số tài khoản masked, tên tài khoản, số dư, loại tiền (VND)
  * Nút "+ Mở tài khoản mới" (KYC required)
  * Sort: Tài khoản chính lên đầu

#### US-03.2: Chi Tiết Tài Khoản & Sao Kê (Screen 05)
* **Là một:** Khách hàng
* **Tôi muốn:** Xem lịch sử giao dịch chi tiết của từng tài khoản
* **Để:** Kiểm tra và đối soát thu chi
* **Acceptance Criteria:**
  * Filter: Ngày (7 ngày, 30 ngày, tùy chọn), loại (Nhận/Gửi/Tất cả)
  * Search: Tìm theo số tiền, người giao dịch, nội dung
  * Phân trang: Load thêm khi scroll (infinite scroll)
  * CQRS: Transaction History đọc từ Read Model (PostgreSQL với index tối ưu)
  * Export sao kê PDF (Phase 2+)
  * Màu sắc: Xanh (nhận tiền), Đỏ (chuyển tiền)

---

### 💸 EPIC-04: Transfer Module (Trọng Tâm Banking)
**Mục tiêu:** Chuyển tiền an toàn, chống duplicate, hỗ trợ nội bộ và liên ngân hàng.

#### US-04.1: Chuyển Tiền Nội Bộ BankX (Screen 06-09)
* **Là một:** Khách hàng
* **Tôi muốn:** Chuyển tiền cho người nhận trong cùng ngân hàng BankX
* **Để:** Thanh toán, chia sẻ chi phí, hỗ trợ tài chính người thân
* **Acceptance Criteria:**
  * **Screen 06 — Nhập thông tin:**
    * Chọn tài khoản nguồn (từ danh sách tài khoản của user)
    * Nhập STK/SĐT người nhận → Auto look-up tên (call Account Service)
    * Chọn số tiền (quick preset: 100K, 500K, 1M, Khác)
    * Nội dung chuyển tiền (optional, max 200 ký tự)
  * **Screen 07 — Xác nhận:**
    * Review toàn bộ thông tin trước khi xác nhận
    * Hiển thị: Số tiền, phí giao dịch (nếu có), Người nhận, Nội dung
    * Nút "Quay lại" và "Xác nhận"
  * **Screen 08 — Nhập OTP:**
    * OTP 6 chữ số gửi về SĐT
    * Frontend sinh idempotency-key (UUID) trước khi gọi API
    * Countdown 120 giây, nút "Gửi lại OTP"
  * **Screen 09 — Kết quả:**
    * Thành công: Animation ✅, hiển thị mã giao dịch, nút "Chuyển tiếp" / "Về trang chủ"
    * Thất bại: Lý do cụ thể (số dư không đủ, tài khoản khóa...), nút "Thử lại"
* **Technical Requirements:**
  * Idempotency: Client gửi UUID, server lưu Redis 10 phút
  * Concurrency: Optimistic Lock trên account.version
  * Outbox Pattern: Event → Kafka → Notification + Audit
  * Giới hạn: Max 200M VND/giao dịch, Max 500M VND/ngày

#### US-04.2: Chuyển Tiền Liên Ngân Hàng (Screen 06 tab "Liên ngân hàng")
* **Là một:** Khách hàng
* **Tôi muốn:** Chuyển tiền đến tài khoản ngân hàng khác (Vietcombank, Techcombank...)
* **Để:** Thanh toán linh hoạt không bị giới hạn ngân hàng
* **Acceptance Criteria:**
  * Chọn ngân hàng đích từ dropdown (danh sách từ Napas mock)
  * Nhập STK, họ tên người nhận (không auto-lookup — liên ngân hàng không thể verify tên realtime)
  * Phí giao dịch: Hiển thị rõ trước khi confirm
  * Thời gian: T+0 (nhanh) hoặc T+1 (thường)
  * Mock InterBank adapter: Simulate success/failure từ external bank

---

### 💳 EPIC-05: Payment Module
**Mục tiêu:** Thanh toán hóa đơn đa dạng, QR payment.

#### US-05.1: Thanh Toán Hóa Đơn (Screen 10-11)
* **Là một:** Khách hàng
* **Tôi muốn:** Thanh toán điện, nước, internet, điện thoại trực tiếp từ app
* **Để:** Tiết kiệm thời gian, không cần ra quầy hoặc dùng nhiều app khác
* **Acceptance Criteria:**
  * Danh mục dịch vụ: Điện (EVN), Nước, Internet (FPT/Viettel/VNPT), Di động, Truyền hình, Học phí, Bảo hiểm
  * Pattern: **Strategy Pattern** — mỗi nhà cung cấp là một PaymentProvider implementation
  * Mock providers: Tất cả dùng MockProvider trả về hóa đơn fake
  * Hiển thị hóa đơn chờ thanh toán của user (group by provider)
  * Thanh toán → OTP → Ledger entry

#### US-05.2: QR Payment (Screen 12)
* **Là một:** Khách hàng
* **Tôi muốn:** Quét mã QR để thanh toán hoặc chuyển tiền
* **Để:** Thanh toán nhanh mà không cần nhập số tài khoản
* **Acceptance Criteria:**
  * Camera access qua Capacitor Camera Plugin (Mobile) hoặc Upload ảnh (Web)
  * Parse QR theo chuẩn VietQR (NAPAS format)
  * Auto-điền thông tin người nhận và số tiền từ QR
  * Tạo QR nhận tiền: Tự động generate từ account info + optional amount
  * QR có logo BankX, màu tím thương hiệu

---

### 🃏 EPIC-06: Card Management
**Mục tiêu:** Quản lý thẻ debit/credit, virtual card.

#### US-06.1: Danh Sách Thẻ (Screen 14)
* **Acceptance Criteria:**
  * Hiển thị: Thẻ debit, thẻ credit, virtual card
  * Card số: Masked (****9988), Hạn mức thẻ tín dụng
  * Status badge: Active (xanh), Frozen (xanh lam), Blocked (đỏ)
  * **KHÔNG lưu CVV thật** — chỉ lưu tokenized PAN

#### US-06.2: Quản Lý Thẻ (Screen 15)
* **Acceptance Criteria:**
  * Khóa/Mở thẻ tạm thời (Freeze/Unfreeze) — realtime, không cần OTP
  * Thay đổi hạn mức thanh toán online
  * Cài đặt thông báo giao dịch
  * Tạo Virtual Card (số thẻ ảo, CVV ảo, expiry, cho phép set spending limit)
  * Yêu cầu thay thẻ (Request card replacement)

---

### 🎁 EPIC-07: Promotion & Flash Sale
**Mục tiêu:** Ưu đãi cashback, Flash Sale như TPBank "Flash Sale" banner.

#### US-07.1: Trang Ưu Đãi (Screen 16)
* **Acceptance Criteria:**
  * Tab: Tất cả / Đang diễn ra / Dành riêng cho bạn (personalized)
  * Card ưu đãi: Hình ảnh, tiêu đề, % cashback/giảm giá, thời hạn áp dụng
  * Detail: Điều kiện áp dụng, cách nhận ưu đãi
  * Flash Sale countdown timer (Kafka consumer cập nhật realtime)

---

### 👤 EPIC-09: Profile & Security
**Mục tiêu:** Quản lý thông tin cá nhân và bảo mật tài khoản.

#### US-09.1: Hồ Sơ Cá Nhân (Screen 18)
* **Acceptance Criteria:**
  * Hiển thị: Avatar, Tên, SĐT, Email, Mã khách hàng
  * Menu điều hướng: Thông tin cá nhân, Bảo mật, Cài đặt, Ngôn ngữ, Hỗ trợ, Đăng xuất
  * KYC Status badge (Chưa xác minh / Đã xác minh)

#### US-09.2: Bảo Mật Tài Khoản (Screen 19)
* **Acceptance Criteria:**
  * Đổi mật khẩu (yêu cầu mật khẩu cũ + OTP)
  * Bật/tắt đăng nhập sinh trắc học (Biometric toggle)
  * Xác thực 2 lớp (2FA - TOTP via Google Authenticator)
  * Quản lý thiết bị đã đăng nhập (xem danh sách, revoke session)
  * Đặt hạn mức giao dịch (Max amount per day)

---

### 🖥️ EPIC-10: Admin Portal
**Mục tiêu:** Công cụ quản trị cho nhân viên ngân hàng.

#### US-10.1: Admin Dashboard (Screen 20)
* **Acceptance Criteria:**
  * KPI: Tổng giao dịch hôm nay, Tổng giá trị GD, Số GD lỗi, Khách hàng mới
  * Biểu đồ: Volume giao dịch theo giờ (Line chart), Phân bổ loại GD (Pie chart)
  * Cảnh báo: Fraud alerts, Failed transfers > 5% trong 1 giờ

#### US-10.2: Transaction Monitoring (Screen 21)
* **Acceptance Criteria:**
  * Bảng danh sách giao dịch realtime (WebSocket push)
  * Filter: Ngày, trạng thái (COMPLETED/FAILED/PENDING), số tiền, ngân hàng
  * Tìm kiếm theo mã GD, STK
  * Chi tiết GD: Ledger entries, audit trail, IP, device
  * Action: Manual resolve, Refund initiation

---

### ⚙️ EPIC-11: Engineering Portal
**Mục tiêu:** Dashboard kỹ thuật cho DevOps và Developer.

#### US-11.1: System Health Dashboard (Screen 22)
* **Acceptance Criteria:**
  * Service Health: Status của tất cả modules (UP/DOWN)
  * Kafka Lag Monitor: Consumer group lag per topic/partition
  * Redis Stats: Hit rate, memory usage, connected clients
  * JVM Metrics: Heap usage, GC pause time
  * Database Pool: Active connections, wait queue, max pool
  * **Verification Guide Panel** (theo AGENTS.md rule): Kịch bản test thủ công
    * Happy path: Gửi transfer → check Kafka consumer lag giảm
    * Chaos: Tắt notification consumer → Lag tăng → Alert
    * Recovery: Bật lại → Lag drain về 0

---

## 🏃 Thứ Tự Ưu Tiên Làm (Sprint Priority Order)

```
Phase 1 — Foundation (Sprint 01-05)
  → Infrastructure setup (Docker, DB, Redis, Kafka)
  → Auth Module (Login, JWT, OTP)
  → Customer Module (Profile, KYC basic)
  → Account Module (CRUD, Balance)
  → Angular: Login → Dashboard → Account screens

Phase 2 — Core Banking (Sprint 06-12)
  → Ledger Module (Double-Entry)
  → Transfer Module (Core flow, Idempotency, Optimistic Lock)
  → Transfer Angular flow (4 screens)
  → Outbox Pattern + Kafka events
  → Notification Module

Phase 3 — Advanced Banking (Sprint 13-18)
  → Payment Module (Bill, QR)
  → Card Module
  → Fraud Detection Rule Engine
  → OTP Saga integration
  → Admin Portal basics

Phase 4 — Distributed & Observability (Sprint 19-25)
  → Saga Orchestration
  → CQRS Transaction History Read Model
  → Prometheus + Grafana
  → Jaeger Distributed Tracing
  → Engineering Portal

Phase 5 — Mobile & Production (Sprint 26-30)
  → Ionic + Capacitor
  → Biometric login
  → CI/CD Pipeline
  → Kubernetes (optional)
  → Load Testing + Performance Tuning
```
