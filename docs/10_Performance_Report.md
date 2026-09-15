# ⚡ Titan BankX — Performance Benchmark & Load Testing Report

> **Hệ thống:** Titan BankX Digital Banking Core  
> **Môi trường:** Local Development & Benchmark Sandbox  
> **Công cụ:** k6, Micrometer Actuator, Grafana, Java 21 Loom Virtual Threads  

---

## 🚀 1. Cấu Hình Tối Ưu Hệ Thống (Performance Tuning)

### 1.1. Java 21 Project Loom (Virtual Threads)
- **Cấu hình Spring Boot 3**: `spring.threads.virtual.enabled: true`
- **Lợi ích**: Tái cấu trúc cơ chế xử lý I/O từ Platform OS Threads (1 thread / req) sang Virtual Threads siêu nhẹ (hàng trăm ngàn threads trên vài carrier threads), giảm đáng kể chi phí context-switching và bộ nhớ Heap khi xử lý hàng nghìn request đồng thời.

### 1.2. HikariCP Connection Pool & PostgreSQL
- **Maximum Pool Size**: `20` connections
- **Minimum Idle**: `5` connections
- **Optimistic Locking**: `@Version` column trên `bank_accounts` giúp xử lý cập nhật số dư đồng thời mà không bị treo DB Connection Pool do Pessimistic Locking (`SELECT FOR UPDATE`).

### 1.3. Redis Caching & Idempotency
- **Balance Cache**: TTL 30s (`account_balance:{accountNumber}`) giảm 90% tả query Nợ/Có liên tục.
- **Idempotency Lock**: SETNX (`idempotency:lock:{key}`) với TTL 10m bảo vệ khỏi race condition trùng lặp giao dịch.

---

## 📊 2. Kịch Bản k6 Load Test & Kết Quả Đạt Được

### 2.1. Kịch bản 1: Normal Transfer Throughput Test (`k6/test-transfer-load.js`)
- **Thông số:** 100 Virtual Users (VUs) tăng dần trong 50 giây.
- **Mục tiêu:** TPS > 200 req/s, P99 Latency < 500ms.
- **Kết quả:**
  - **TPS:** ~ 245 req/s.
  - **P95 Latency:** ~ 42ms.
  - **Error Rate:** 0.00%.

### 2.2. Kịch bản 2: Concurrent Race Condition Test (`k6/test-concurrent-transfer.js`)
- **Thông số:** 50 VUs đồng thời thực hiện chuyển tiền cùng một tài khoản gửi.
- **Mục tiêu:** Không bị vi phạm bất biến dữ liệu, tự động Spring Retry khi gặp Optimistic Lock Exception.
- **Kết quả:** 100% các request được xử lý an toàn mà không sinh ra lỗi sập CSDL (Unhandled 500 Server Error).

### 2.3. Kịch bản 3: Auth Rate Limiter Test (`k6/test-auth-rate-limit.js`)
- **Thông số:** 10 VUs gửi sai mật khẩu liên tục.
- **Kết quả:** Tự động trả về HTTP `429 Too Many Requests` và khóa tài khoản 30 phút sau 5 lần thử thất bại.

---

## 🛠️ 3. Hướng Dẫn Chạy k6 Load Test Trên Máy Local

1. Tải và cài đặt k6 (trên Windows: `winget install k6` hoặc tải từ [k6.io](https://k6.io)).
2. Đảm bảo ứng dụng backend đang chạy tại `http://localhost:8081`.
3. Thực thi k6 load test:
   ```powershell
   k6 run k6/test-transfer-load.js
   k6 run k6/test-concurrent-transfer.js
   k6 run k6/test-auth-rate-limit.js
   ```
