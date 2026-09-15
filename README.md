# 🏦 Titan BankX — Digital Banking Platform

> Nền tảng Ngân hàng Số mô phỏng TPBank, xây dựng hoàn chỉnh theo tiêu chuẩn Banking-Grade  
> **27 Sprints · Java 21 + Spring Boot 3 · Angular 22 · PostgreSQL · Kafka · Redis**

---

## ✨ Giới Thiệu

**Titan BankX** là dự án ngân hàng số học thuật cấp Senior Engineer, mô phỏng đầy đủ các nghiệp vụ cốt lõi của một ngân hàng thương mại hiện đại (dựa trên TPBank). Dự án được xây dựng qua **27 Sprints** theo lộ trình từ Foundation đến Production Hardening.

**Mục tiêu:** Học và thực hành toàn bộ Banking Architecture patterns ở mức Senior Java Engineer, chuẩn bị cho phỏng vấn Banking Java Senior / Distributed Systems.

---

## 🏗️ Kiến Trúc Tổng Quan

```
┌─────────────────────────────────────────────────────────────┐
│                   Angular 22 Web App (Port 4200)            │
│         (Standalone Components · Signals · Lazy Loading)    │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────▼───────────────────────────────────┐
│           Spring Cloud API Gateway (Port 8080)              │
│        (CORS · Rate Limit · Auth Filter · Correlation ID)   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│              BankX Banking Core (Port 8081)                 │
│                  Modular Monolith — Java 21                  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐ │
│  │Auth Module │ │Customer KYC│ │Account Core│ │ Transfer │ │
│  │JWT·OTP·Lock│ │eKYC·Profile│ │@Version·OL │ │Saga·OTP  │ │
│  └────────────┘ └────────────┘ └────────────┘ └──────────┘ │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐ │
│  │  Ledger   │ │  Payment   │ │    Card    │ │Notification│ │
│  │Double-Entry│ │Strategy/QR │ │Tokenization│ │Kafka·DLQ │ │
│  └────────────┘ └────────────┘ └────────────┘ └──────────┘ │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐ │
│  │   Fraud   │ │Beneficiary │ │ Admin Core │ │   CQRS   │ │
│  │Rule Engine │ │SmartSuggest│ │eKYC·Audit  │ │Read Model│ │
│  └────────────┘ └────────────┘ └────────────┘ └──────────┘ │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐              │
│  │   Saga    │ │Circuit Bkr │ │Engineering │              │
│  │Orchestrator│ │Resilience4j│ │Portal/Chaos│              │
│  └────────────┘ └────────────┘ └────────────┘              │
└────────┬───────────────┬────────────────┬───────────────────┘
         │               │                │
    ┌────▼────┐    ┌──────▼─────┐  ┌──────▼─────┐
    │PostgreSQL│    │   Redis 7  │  │Apache Kafka│
    │   16    │    │Cache·Session│  │Outbox·Event│
    └─────────┘    └────────────┘  └────────────┘
         │
    ┌────▼────────────────────────────────────────┐
    │     Observability Stack                      │
    │  Prometheus · Grafana · Jaeger (OpenTelemetry)│
    └──────────────────────────────────────────────┘
```

---

## 🎯 Kỹ Thuật Đã Học & Triển Khai

### 🔐 Auth & Security
| Kỹ Thuật | Mô Tả | Sprint |
|---|---|---|
| JWT + Refresh Token Rotation | Access Token 15m, Refresh Token 7d, revoke on use | S02 |
| OTP Risk-Based Authentication | 6-digit, Redis TTL 120s, max 3 attempts, block 5m | S03 |
| Account Lock | 5 failed attempts → lock 30m | S02 |
| KYC & Identity Masking | Customer CIF, identity number masking in logs | S04 |

### 💰 Core Banking
| Kỹ Thuật | Mô Tả | Sprint |
|---|---|---|
| Double-Entry Bookkeeping | `SUM(DEBIT) == SUM(CREDIT)`, Immutable Ledger | S06 |
| Idempotency (Redis SETNX) | AOP `@Idempotent`, UUID Key, Response Caching | S08 |
| Optimistic Locking (`@Version`) | Race condition prevention + Spring Retry x3 | S09 |
| Risk-Based Transfer OTP | < 5M → Direct; ≥ 5M → OTP 2-layer | S10 |
| Transactional Outbox Pattern | DB + Kafka atomic consistency, At-Least-Once | S11 |
| Idempotent Kafka Consumer | Redis `consumed_event:{id}` TTL 1h | S12 |

### 💳 Advanced Features
| Kỹ Thuật | Mô Tả | Sprint |
|---|---|---|
| Strategy Pattern (Bill Payment) | EVN/Water/Viettel/Mock providers, OCP compliance | S13 |
| VietQR EMVCo Standard | TLV Parser/Generator, CRC-16/CCITT-FALSE | S14 |
| PCI-DSS Card Tokenization | Virtual PAN, Masked PAN, Card FSM State Machine | S15 |
| Fraud Rule Engine | 5 rules, Risk Score 0-100, ALLOW/OTP/BLOCK | S16 |
| Smart Beneficiary Suggestions | Frequency tracking, auto-save, Top-4 suggestions | S17 |
| Admin Portal & eKYC Review | KYC workflow, emergency freeze, audit trail | S18 |

### ⚡ Distributed Systems
| Kỹ Thuật | Mô Tả | Sprint |
|---|---|---|
| CQRS + Kafka Projection | Write/Read model separation, Cursor-based pagination | S19 |
| Prometheus + Grafana | Custom business metrics, Micrometer, JVM metrics | S20 |
| OpenTelemetry + Jaeger | Distributed Tracing, W3C traceparent, MDC correlation | S21 |
| Saga Orchestration | State Machine, Compensating Transactions `REVERSE_DEBIT` | S22 |
| Resilience4j | Circuit Breaker 3-state, Rate Limiter 5 req/s, Retry, Bulkhead | S23 |
| Chaos Engineering | DB Delay, Kafka Down, Flood 100 Transfers | S24 |

### 🛠️ Engineering
| Kỹ Thuật | Mô Tả | Sprint |
|---|---|---|
| Swagger / OpenAPI 3 | JWT Bearer Auth, `@Tag`, `@Operation`, Interactive UI | S25 |
| k6 Load Testing | 100 VUs Transfer, Race Condition Test, Auth Rate Limit | S26 |
| Java 21 Virtual Threads | `spring.threads.virtual.enabled=true`, Project Loom | S26 |
| DB Backup/Restore Scripts | PowerShell 1-click backup/restore PostgreSQL & Redis | S27 |

---

## 🚀 Quick Start (5 Phút)

```powershell
# 1. Khởi động Infrastructure
cd c:\Users\Admin\Desktop\banking\infrastructure
docker compose up -d

# 2. Khởi động Backend (Terminal 1)
cd c:\Users\Admin\Desktop\banking\backend\bankx-banking-core
mvn spring-boot:run

# 3. Khởi động Frontend (Terminal 2)
cd c:\Users\Admin\Desktop\banking\frontend-web
npx ng serve --open
```

**Xem hướng dẫn chi tiết:** [guide_run.md](./guide_run.md)

---

## 🌐 URLs & Services

| Dịch Vụ | URL |
|---|---|
| 🌐 Angular Web App | http://localhost:4200 |
| 🔌 Banking Core API | http://localhost:8081 |
| 📖 **Swagger UI** | http://localhost:8081/swagger-ui/index.html |
| 📊 Kafka UI | http://localhost:8090 |
| 📈 Grafana | http://localhost:3000 |
| 🎯 Prometheus | http://localhost:9090 |
| 🔍 Jaeger Tracing | http://localhost:16686 |

---

## 📚 Tài Liệu

| # | Tài Liệu | Mô Tả |
|---|---|---|
| 1 | [guide_run.md](./guide_run.md) | **Hướng dẫn chạy Local chi tiết** |
| 2 | [docs/question.md](./docs/question.md) | **Câu hỏi phỏng vấn Banking Senior** |
| 3 | [docs/04_Sprint_Plan.md](./docs/04_Sprint_Plan.md) | Lộ trình 27 Sprints chi tiết |
| 4 | [docs/01_Architecture_Bible.md](./docs/01_Architecture_Bible.md) | Kiến trúc tổng thể |
| 5 | [docs/06_Database_Schema.md](./docs/06_Database_Schema.md) | Schema CSDL & Flyway |
| 6 | [docs/10_Performance_Report.md](./docs/10_Performance_Report.md) | k6 Load Test Results |
| 7 | [task.md](./task.md) | Tracker tiến độ 27 Sprints |

---

## 📊 Sprint Progress — 100% Hoàn Thành

```
Phase 0 — Infrastructure Foundation:    [██████████] Sprint 00        ✅
Phase 1 — Auth & Account Core:          [██████████] Sprint 01–05     ✅
Phase 2 — Transfer Core (Banking Heart):[██████████] Sprint 06–12     ✅
Phase 3 — Payment & Advanced Features:  [██████████] Sprint 13–18     ✅
Phase 4 — Distributed & Observability:  [██████████] Sprint 19–24     ✅
Phase 5 — Engineering & Local DevOps:   [██████████] Sprint 25–27     ✅

OVERALL: 27 / 27 Sprints  ████████████████████████████  100%
```

---

## 🔑 Tài Khoản Demo

| Username | Password | Vai Trò |
|---|---|---|
| `admin` | `Admin@123456` | Admin Portal, eKYC Review, Audit Trail |
| `user01` | `User@123456` | Khách hàng thông thường |

---

## ⚡ k6 Load Test

```powershell
k6 run k6/test-transfer-load.js        # 100 VUs, TPS > 200
k6 run k6/test-concurrent-transfer.js  # Race Condition Test
k6 run k6/test-auth-rate-limit.js      # Auth Rate Limit Test
```

---

## 🎤 Phỏng Vấn

File [docs/question.md](./docs/question.md) tổng hợp **60+ câu hỏi phỏng vấn chuyên sâu** theo format:
- ❓ **Câu hỏi thực tế** từ phỏng vấn Banking Java Senior
- ❌ **Tại sao KHÔNG** — Phân tích trade-offs, Anti-patterns
- ✅ **Tại sao NÊN** — Giải pháp đúng với lý do cụ thể từ code BankX

Bao gồm: Saga/Outbox, Idempotency, Optimistic Lock, Double-Entry, JWT, CQRS, Circuit Breaker, Kafka, Fraud Detection, Clean Architecture, Performance...

---

*Titan BankX — Built with ❤️ for Banking Engineering Excellence*
