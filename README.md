# BankX Digital Banking Platform

> Dự án ngân hàng số mô phỏng TPBank — Xây dựng để học Java Senior Banking Architecture + Angular Banking Frontend

![BankX UI Preview](./banking/UI_TPBank.png)

---

## 🎯 Mục Tiêu Dự Án

1. **Học Banking Domain:** Ledger kép, Transfer flow, OTP, KYC, Fraud Detection
2. **Java Enterprise Architecture:** Clean Architecture, DDD, Hexagonal, CQRS
3. **Distributed Systems:** Kafka, Outbox Pattern, Saga, Idempotency, Circuit Breaker
4. **Angular Banking Frontend:** Signals, NgRx, Lazy Loading, Ionic Mobile
5. **Chuẩn bị phỏng vấn Banking Java Senior**

---

## 🏗️ Kiến Trúc Tổng Quan

```
Angular Web + Ionic Mobile
         ↓
  Spring Cloud Gateway (Rate Limit, Auth Filter, Routing)
         ↓
  BankX Banking Core (Modular Monolith — Phase 1)
  ├── Auth Module (JWT, OTP, Refresh Token Rotation)
  ├── Customer Module (KYC, Profile)
  ├── Account Module (Balance, Optimistic Lock)
  ├── Transfer Module (Idempotency, Saga, Outbox)
  ├── Ledger Module (Double-Entry Bookkeeping)
  ├── Payment Module (Strategy Pattern, Bill, QR)
  ├── Card Module (Tokenization, Virtual Card)
  ├── Notification Module (Kafka Consumer)
  ├── Fraud Module (Rule Engine, Risk Score)
  └── Audit Module (Immutable Trail)
         ↓
  PostgreSQL + Redis + Kafka
         ↓
  Prometheus + Grafana + Jaeger (Observability)
```

---

## 📚 Tài Liệu (Đọc Theo Thứ Tự Này)

| # | Tài Liệu | Mô Tả |
|---|---|---|
| 1 | [Project Vision](./banking/docs/00_Project_Vision.md) | Tầm nhìn, scope, technology stack |
| 2 | [Architecture Bible](./banking/docs/01_Architecture_Bible.md) | Kiến trúc chi tiết, sequence diagrams, patterns |
| 3 | [Coding Guideline](./banking/docs/02_Coding_Guideline.md) | Coding standards, Javadoc, naming conventions |
| 4 | [Backlog](./banking/docs/03_Backlog.md) | Epics, User Stories, Acceptance Criteria |
| 5 | [Sprint Plan](./banking/docs/04_Sprint_Plan.md) | 30 Sprint roadmap với checklist |
| 6 | [AI Coding Guide](./banking/docs/05_AI_Coding_Guide.md) | Prompt templates, rules cho AI agents |
| 7 | [Database Schema](./banking/docs/06_Database_Schema.md) | Schema tất cả bảng DB + Flyway history |
| 8 | [UI/UX Standard](./banking/docs/07_UI_UX_Standard.md) | TPBank design tokens, component specs |
| 9 | [Interview Q&A](./banking/docs/09_Interview_QA_Banking.md) | Hỏi đáp phỏng vấn banking |
| 10 | [ADR](./banking/docs/adr/) | Architecture Decision Records |

---

## 🚀 Quick Start (Sau Sprint 00)

```bash
# Clone + setup
cd banking

# Start infrastructure
docker-compose -f infrastructure/docker-compose.yml up -d

# Start backend (Phase 1 - Modular Monolith)
cd backend
mvn spring-boot:run -pl bankx-api-gateway -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl bankx-banking-core -Dspring-boot.run.profiles=local

# Start Angular web
cd frontend-web
npm install
ng serve

# URLs:
# 🌐 Web App:      http://localhost:4200
# 🔌 API Gateway:  http://localhost:8090
# 📊 Kafka UI:     http://localhost:8080
# 📈 Grafana:      http://localhost:3000
# 🔍 Jaeger:       http://localhost:16686
```

---

## 📊 Sprint Progress

```
Phase 0 (Foundation):     [ ] Sprint 00
Phase 1 (Core):           [ ] Sprint 01 → 05
Phase 2 (Banking Heart):  [ ] Sprint 06 → 12
Phase 3 (Advanced):       [ ] Sprint 13 → 18
Phase 4 (Distributed):    [ ] Sprint 19 → 24
Phase 5 (Mobile+Prod):    [ ] Sprint 25 → 30

Chi tiết: banking/task.md
```

---

## 🔑 Banking Concepts Learned

Sau khi hoàn thành dự án, sẽ hiểu và implement được:

- ✅ **Double-Entry Bookkeeping** — Ledger entries, không thể âm tổng
- ✅ **Idempotency** — Chống duplicate transfer với Redis SETNX
- ✅ **Optimistic Locking** — Chống race condition khi rút tiền đồng thời
- ✅ **Outbox Pattern** — Đảm bảo DB + Kafka consistency
- ✅ **Saga Orchestration** — Distributed transaction với compensation
- ✅ **JWT + Refresh Token Rotation** — Secure session management
- ✅ **OTP Flow** — Risk-based authentication
- ✅ **Rate Limiting** — Token Bucket in Redis
- ✅ **Circuit Breaker** — Resilience4j, 3 states
- ✅ **CQRS** — Transaction History Read Model
- ✅ **Distributed Tracing** — OpenTelemetry + Jaeger
- ✅ **Clean Architecture** — Domain independence from framework
- ✅ **Strategy Pattern** — Bill Payment providers
- ✅ **Angular Signals** — Modern reactive state management

---

## 👨‍💻 Dành Cho AI Agents

Khi làm việc trong thư mục `banking/`:
1. Đọc `banking/.agents/AGENTS.md` — Rules bắt buộc
2. Đọc `banking/docs/05_AI_Coding_Guide.md` — Workflow và prompt templates
3. Dùng skill `banking-java-backend` khi code Java
4. Dùng skill `banking-angular-frontend` khi code Angular
