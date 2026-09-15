# 🚀 Titan BankX — Hướng Dẫn Chạy Dự Án Local (Local Run Guide)

> **Dự án:** Titan BankX Digital Banking Platform (TPBank Simulation)  
> **Stack:** Java 21 + Spring Boot 3 + Angular 22 + PostgreSQL + Redis + Kafka  
> **Môi trường:** Local Development Sandbox  

---

## 📋 Mục Lục

1. [Yêu Cầu Hệ Thống (Prerequisites)](#1-yêu-cầu-hệ-thống)
2. [Cấu Trúc Thư Mục](#2-cấu-trúc-thư-mục)
3. [Khởi Chạy Infrastructure (Docker)](#3-khởi-chạy-infrastructure-docker)
4. [Khởi Chạy Backend (Spring Boot)](#4-khởi-chạy-backend-spring-boot)
5. [Khởi Chạy Frontend (Angular)](#5-khởi-chạy-frontend-angular)
6. [Bảng Tổng Hợp URLs & Ports](#6-bảng-tổng-hợp-urls--ports)
7. [Tài Khoản Demo](#7-tài-khoản-demo)
8. [Chạy k6 Load Test](#8-chạy-k6-load-test)
9. [Sao Lưu & Khôi Phục CSDL](#9-sao-lưu--khôi-phục-csdl)
10. [Xử Lý Sự Cố Thường Gặp](#10-xử-lý-sự-cố-thường-gặp)
11. [Hướng Dẫn Sử Dụng Swagger UI](#11-hướng-dẫn-sử-dụng-swagger-ui)

---

## 1. Yêu Cầu Hệ Thống

Đảm bảo máy tính đã cài đặt đầy đủ các công cụ sau trước khi bắt đầu:

| Công Cụ | Phiên Bản | Kiểm Tra |
|---|---|---|
| **JDK (Java)** | 21+ | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Node.js** | 20+ | `node --version` |
| **npm** | 10+ | `npm --version` |
| **Docker Desktop** | 4.x+ | `docker --version` |
| **Docker Compose** | V2+ | `docker compose version` |
| **Angular CLI** | 17+ | `npx ng version` |
| **k6** *(tùy chọn)* | Latest | `k6 version` |

> **Cài đặt k6 trên Windows:** `winget install k6` hoặc tải từ [https://k6.io/docs/get-started/installation/](https://k6.io/docs/get-started/installation/)

---

## 2. Cấu Trúc Thư Mục

```
banking/
├── backend/
│   ├── bankx-api-gateway/          ← Spring Cloud Gateway (Port 8080)
│   ├── bankx-banking-core/         ← Banking Core Service (Port 8081)
│   └── bankx-common/               ← Shared Library (ApiResponse, DTOs)
│
├── frontend-web/                   ← Angular 22 Web App (Port 4200)
│
├── infrastructure/
│   └── docker-compose.yml          ← Tất cả services hạ tầng
│
├── k6/                             ← k6 Load Test Scripts
│   ├── test-transfer-load.js       ← 100 VUs Transfer Load Test
│   ├── test-concurrent-transfer.js ← 50 VUs Race Condition Test
│   └── test-auth-rate-limit.js     ← Auth Rate Limit Test
│
├── scripts/
│   ├── backup_db.ps1               ← 1-Click sao lưu PostgreSQL + Redis
│   └── restore_db.ps1              ← 1-Click khôi phục PostgreSQL
│
├── docs/                           ← Tài liệu kiến trúc & Sprint Plan
├── task.md                         ← Tiến độ 27 Sprints
├── guide_run.md                    ← File này
└── README.md
```

---

## 3. Khởi Chạy Infrastructure (Docker)

### Bước 3.1 — Khởi động toàn bộ hạ tầng

```powershell
# Chuyển vào thư mục infrastructure
cd c:\Users\Admin\Desktop\banking\infrastructure

# Khởi động tất cả services (PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger)
docker compose up -d

# Kiểm tra trạng thái tất cả containers
docker compose ps
```

### Bước 3.2 — Kiểm tra tất cả services đang chạy

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Kết quả kỳ vọng — tất cả containers có trạng thái `Up`:

| Container Name | Service | Port |
|---|---|---|
| `bankx-postgres` | PostgreSQL 16 | 5432 |
| `bankx-redis` | Redis 7 | 6379 |
| `bankx-kafka` | Apache Kafka | 9092 |
| `bankx-kafka-ui` | Kafka UI | 8090 |
| `bankx-prometheus` | Prometheus | 9090 |
| `bankx-grafana` | Grafana | 3000 |
| `bankx-jaeger` | Jaeger All-in-One | 16686 |

### Bước 3.3 — Dừng hạ tầng khi không cần

```powershell
docker compose down

# Nếu muốn xóa cả data volumes (cẩn thận!)
docker compose down -v
```

---

## 4. Khởi Chạy Backend (Spring Boot)

> **Quan trọng:** Phải khởi động Docker Compose trước (Bước 3) rồi mới chạy Backend.

### Bước 4.1 — Build toàn bộ project Backend

```powershell
cd c:\Users\Admin\Desktop\banking\backend

# Build tất cả modules (bỏ qua test để nhanh hơn)
mvn clean install -DskipTests
```

### Bước 4.2 — Khởi chạy Banking Core Service (Port 8081)

Mở một Terminal riêng và chạy:

```powershell
cd c:\Users\Admin\Desktop\banking\backend\bankx-banking-core

# Khởi động Spring Boot
mvn spring-boot:run
```

Khi thấy log sau thì Backend đã sẵn sàng:

```
Started BankxBankingCoreApplication in X.XXX seconds
```

> **Lưu ý:** Flyway sẽ tự động chạy các migration script khi khởi động lần đầu, tạo toàn bộ bảng CSDL và dữ liệu mẫu.

### Bước 4.3 — (Tùy chọn) Khởi chạy API Gateway (Port 8080)

Mở thêm một Terminal và chạy:

```powershell
cd c:\Users\Admin\Desktop\banking\backend\bankx-api-gateway

mvn spring-boot:run
```

---

## 5. Khởi Chạy Frontend (Angular)

### Bước 5.1 — Cài đặt dependencies (chỉ cần làm lần đầu)

```powershell
cd c:\Users\Admin\Desktop\banking\frontend-web

npm install
```

### Bước 5.2 — Khởi động Angular Dev Server

```powershell
# Khởi động Angular với proxy tới Backend
$env:NG_DISABLE_VERSION_CHECK="true"
npx ng serve --open
```

Trình duyệt sẽ tự động mở `http://localhost:4200`.

### Bước 5.3 — Build Production Bundle (Tùy chọn)

```powershell
$env:NG_DISABLE_VERSION_CHECK="true"
npx ng build --configuration development
```

---

## 6. Bảng Tổng Hợp URLs & Ports

| Dịch Vụ | URL | Mô Tả |
|---|---|---|
| 🌐 **Angular Web App** | http://localhost:4200 | Giao diện người dùng chính |
| 🔌 **Banking Core API** | http://localhost:8081 | REST API trực tiếp |
| 🔌 **API Gateway** | http://localhost:8080 | Spring Cloud Gateway (Routing) |
| 📖 **Swagger UI** | http://localhost:8081/swagger-ui/index.html | Tài liệu API tương tác |
| 📊 **Kafka UI** | http://localhost:8090 | Theo dõi Kafka Topics & Consumer Groups |
| 📈 **Grafana** | http://localhost:3000 | Dashboard giám sát Metrics |
| 🎯 **Prometheus** | http://localhost:9090 | Metrics scraping & PromQL |
| 🔍 **Jaeger** | http://localhost:16686 | Distributed Tracing Waterfall |
| 🗄️ **PostgreSQL** | localhost:5432 | CSDL chính (bankx_db / bankx_user / bankx_password) |
| ⚡ **Redis** | localhost:6379 | Cache & Session Store |

### Thông tin kết nối PostgreSQL:

```
Host:     localhost
Port:     5432
Database: bankx_db
Username: bankx_user
Password: bankx_password
```

---

## 7. Tài Khoản Demo

### Tài khoản Đăng nhập:

| Username | Password | Vai Trò |
|---|---|---|
| `admin` | `Admin@123456` | ADMIN — Truy cập Admin Portal, eKYC Review |
| `user01` | `User@123456` | CUSTOMER — Khách hàng thông thường |

### Flow Thử Nghiệm Nhanh:

1. **Đăng nhập** → http://localhost:4200 → Nhập tài khoản ở trên.
2. **Dashboard** → Xem số dư, sao chép số tài khoản.
3. **Chuyển tiền** → Nhập số tài khoản nhận → Nhập số tiền → Xác nhận.
   - Giao dịch **< 5.000.000 VNĐ**: Hoàn thành ngay lập tức.
   - Giao dịch **≥ 5.000.000 VNĐ**: Cần xác nhận OTP 6 chữ số (lấy từ log Backend).
4. **Kiểm tra Thông báo** → Nhấn icon chuông trên Header.
5. **Lịch sử Giao dịch** → Xem trang Accounts.

---

## 8. Chạy k6 Load Test

> **Yêu cầu:** Cài đặt k6 và Backend đang chạy tại `http://localhost:8081`.

### Test 1: Transfer Load Test (100 Virtual Users)

```powershell
cd c:\Users\Admin\Desktop\banking
k6 run k6/test-transfer-load.js
```

**Kết quả kỳ vọng:**
- `http_req_duration p(95) < 500ms`
- `http_req_failed rate < 0.05 (5%)`

### Test 2: Race Condition Test (50 VUs đồng thời)

```powershell
k6 run k6/test-concurrent-transfer.js
```

**Kết quả kỳ vọng:**
- Tất cả requests xử lý an toàn (HTTP 200/202/400/409).
- **Không có lỗi HTTP 500** — Optimistic Lock + Retry hoạt động đúng.

### Test 3: Auth Rate Limit Test

```powershell
k6 run k6/test-auth-rate-limit.js
```

**Kết quả kỳ vọng:**
- Sau 5 lần đăng nhập sai → HTTP `429 Too Many Requests` hoặc `401` với thông báo tài khoản bị khóa.

---

## 9. Sao Lưu & Khôi Phục CSDL

### Sao lưu Database (PostgreSQL + Redis):

```powershell
cd c:\Users\Admin\Desktop\banking

# Chạy script 1-click backup (tạo file SQL trong thư mục ./backups/)
.\scripts\backup_db.ps1

# Hoặc chỉ định thư mục riêng
.\scripts\backup_db.ps1 -BackupDir "D:\backups\bankx"
```

### Khôi phục Database từ bản sao lưu:

```powershell
# Thay YYYYmmdd_HHmmss bằng timestamp thực tế của file backup
.\scripts\restore_db.ps1 -File ".\backups\bankx_db_20260915_231000.sql"
```

---

## 10. Xử Lý Sự Cố Thường Gặp

### ❌ Lỗi: "Connection refused" khi khởi động Backend

**Nguyên nhân:** Docker containers chưa khởi động xong.

```powershell
# Kiểm tra trạng thái containers
docker compose ps

# Xem log PostgreSQL
docker logs bankx-postgres
```

**Giải pháp:** Chờ khoảng 30-60 giây sau khi `docker compose up -d` rồi mới khởi động Backend.

---

### ❌ Lỗi: "Flyway migration failed" khi khởi động

**Nguyên nhân:** CSDL đã tồn tại schema cũ không tương thích.

```powershell
# Xóa và tạo lại database
docker exec -it bankx-postgres psql -U bankx_user -c "DROP DATABASE bankx_db;"
docker exec -it bankx-postgres psql -U bankx_user -c "CREATE DATABASE bankx_db;"
```

Sau đó khởi động lại Backend — Flyway sẽ chạy lại toàn bộ migration từ đầu.

---

### ❌ Lỗi: Angular không kết nối được Backend (CORS Error)

**Nguyên nhân:** Backend chưa khởi động hoặc port sai.

**Kiểm tra:**
```powershell
# Test Backend đang chạy
curl http://localhost:8081/actuator/health
```

**Giải pháp:** Đảm bảo `bankx-banking-core` đang chạy tại port `8081` trước khi `ng serve`.

---

### ❌ Lỗi: Kafka không nhận được message

```powershell
# Kiểm tra Kafka container
docker logs bankx-kafka

# Truy cập Kafka UI để xem topics
# http://localhost:8090
```

---

### ❌ Port bị chiếm (port already in use)

```powershell
# Tìm process đang dùng port (ví dụ port 8081)
netstat -ano | findstr :8081

# Kill process theo PID
taskkill /PID <PID> /F
```

---

## 11. Hướng Dẫn Sử Dụng Swagger UI

### Truy cập Swagger:

1. Đảm bảo Backend đang chạy (`http://localhost:8081`).
2. Mở trình duyệt, truy cập: **http://localhost:8081/swagger-ui/index.html**

### Xác thực JWT để test API bảo mật:

**Bước 1:** Gọi API Login để lấy Access Token:

```
POST /api/v1/auth/login
Body: { "username": "user01", "password": "User@123456" }
```

**Bước 2:** Copy giá trị `accessToken` từ response.

**Bước 3:** Nhấn nút **"Authorize 🔒"** ở góc phải trên cùng Swagger UI.

**Bước 4:** Paste Access Token vào ô **"BearerAuth (http, Bearer)"** (không cần thêm chữ "Bearer" tiền tố).

**Bước 5:** Nhấn **"Authorize"** → Tất cả API bảo mật đã sẵn sàng để test.

### Nhóm API trong Swagger:

| Nhóm | Mô Tả |
|---|---|
| `01. Auth` | Đăng nhập, Refresh Token, OTP |
| `02. Customer` | KYC Profile, Customer Info |
| `03. Account` | Tài khoản, Phong tỏa, Số dư |
| `04. Transfer` | Chuyển tiền nội bộ, Tra cứu tên |
| `05. Ledger` | Sổ nhật ký kép Double-Entry |
| `06. Payment` | Thanh toán hóa đơn, VietQR |
| `07. Card` | Thẻ ảo, Tokenization, Lifecycle |
| `08. Fraud` | Rule Engine, Risk Score, Alerts |
| `09. Monitoring` | Prometheus Metrics, Circuit Breaker |
| `10. Saga` | Orchestration State Machine |
| `11. Engineering Portal & Chaos` | Health Grid, Chaos Injection |

---

## 🛑 Tắt Toàn Bộ Dự Án

```powershell
# Dừng Angular (Ctrl+C trong terminal ng serve)

# Dừng Backend Spring Boot (Ctrl+C trong terminal mvn spring-boot:run)

# Dừng Docker Infrastructure
cd c:\Users\Admin\Desktop\banking\infrastructure
docker compose down
```

---

*Tài liệu cập nhật lần cuối: Sprint 27 — 100% Roadmap Completed.*
