# 🖼️ BankX Architecture Diagrams

> Bộ sơ đồ kiến trúc hệ thống Titan BankX — Angular Signals, NgRx, Clean Architecture, Transfer Flow

---

## 1. Angular Signals Reactive Flow

![Angular Signals Flow](file:///C:/Users/Admin/.gemini/antigravity-ide/brain/51df9e9b-7078-4be4-bb63-2f0dc9ee1fdc/angular_signals_flow_1789687450907.png)

**Giải thích:**
- `signal()` → WritableSignal, có thể `.set()` / `.update()`
- `computed()` → Lazy, chỉ tính lại khi dependency thay đổi
- `effect()` → Side effects, tự track dependencies, cleanup tự động
- Không cần Zone.js — CD chỉ chạy khi Signal thực sự thay đổi

---

## 2. NgRx Store Data Flow

![NgRx Store Flow](file:///C:/Users/Admin/.gemini/antigravity-ide/brain/51df9e9b-7078-4be4-bb63-2f0dc9ee1fdc/ngrx_store_flow_1789687474362.png)

**Giải thích vòng tròn:**
```
Component → dispatch(Action) → Effects (HTTP) → loadSuccess Action
→ Reducer (pure fn) → Store (immutable state) → Selector (memoized)
→ Component (view update) → [vòng lặp]
```

---

## 3. Titan BankX System Architecture

![BankX System Architecture](file:///C:/Users/Admin/.gemini/antigravity-ide/brain/51df9e9b-7078-4be4-bb63-2f0dc9ee1fdc/bankx_system_architecture_1789687499565.png)

**4 Tầng Kiến Trúc:**
| Tầng | Công Nghệ | Vai Trò |
|---|---|---|
| Frontend | Angular 22 | UI + UX |
| API Gateway | Spring Cloud Gateway | Auth, Rate Limit, Routing |
| Services | Java 21 + Virtual Threads | Business Logic |
| Data | PostgreSQL + Redis + Kafka | Lưu trữ + Cache + Events |

---

## 4. Java Clean Architecture / Hexagonal (Mermaid)

```mermaid
graph TB
    subgraph Infrastructure["🟢 Infrastructure Layer (Adapters)"]
        subgraph DrivingAdapters["Driving Adapters (Input Side)"]
            REST["🌐 REST Controller\n@RestController\nTransferController"]
            KAFKA_IN["📥 Kafka Consumer\nOutboxPollingService"]
        end
        subgraph DrivenAdapters["Driven Adapters (Output Side)"]
            JPA["🗄️ JPA Repository\nBankAccountJpaRepository\nimplements BankAccountRepository"]
            KAFKA_OUT["📤 Kafka Publisher\nKafkaOutboxPublisher\nimplements EventPublisherPort"]
            REDIS["⚡ Redis Adapter\nRedisCacheAdapter\nimplements CachePort"]
        end
    end

    subgraph Ports["🟣 Ports (Interfaces)"]
        IN_PORT["📋 Input Ports\nTransferUseCase\nAccountQueryUseCase"]
        OUT_PORT["📋 Output Ports\nBankAccountRepository\nEventPublisherPort\nCachePort"]
    end

    subgraph Application["🔵 Application Layer"]
        APP_SVC["⚙️ Application Services\nTransferApplicationService\n@Transactional\nOrchestrates domain objects"]
        COMMANDS["📦 Commands & Queries\nInternalTransferCommand\nTransferHistoryQuery"]
    end

    subgraph Domain["🟡 Domain Layer (Pure Java)"]
        ENTITY["🏦 Entities\nBankAccount\nBankTransfer\nLedgerEntry"]
        DOMAIN_SVC["🧠 Domain Services\nTransferDomainService\nLedgerDomainService\nFraudDomainService"]
        VALUE_OBJ["💎 Value Objects\nMoney(amount, currency)\nAccountNumber\nTransactionReference"]
    end

    REST --> IN_PORT
    KAFKA_IN --> IN_PORT
    IN_PORT --> APP_SVC
    APP_SVC --> COMMANDS
    APP_SVC --> DOMAIN_SVC
    DOMAIN_SVC --> ENTITY
    DOMAIN_SVC --> VALUE_OBJ
    APP_SVC --> OUT_PORT
    OUT_PORT --> JPA
    OUT_PORT --> KAFKA_OUT
    OUT_PORT --> REDIS

    style Domain fill:#1a1400,stroke:#ffd600,color:#fff
    style Application fill:#0a1628,stroke:#1565c0,color:#fff
    style Ports fill:#150a28,stroke:#6a1b9a,color:#fff
    style Infrastructure fill:#0a1f0a,stroke:#2e7d32,color:#fff
```

---

## 5. Transfer Flow — Luồng Chuyển Tiền Nội Bộ

```mermaid
sequenceDiagram
    participant U as 👤 User (Angular)
    participant GW as 🔀 API Gateway
    participant CS as ⚙️ Core Service
    participant DB as 🗄️ PostgreSQL
    participant REDIS as ⚡ Redis
    participant KAFKA as 📨 Kafka
    participant NOTIF as 🔔 Notification

    U->>GW: POST /api/v1/transfers/internal\nX-Idempotency-Key: uuid
    GW->>GW: JWT verify + Rate limit check
    GW->>CS: Forward request
    
    CS->>REDIS: SETNX idempotency:lock:uuid (10min TTL)
    REDIS-->>CS: OK (not duplicate)
    
    CS->>DB: SELECT account + version (Optimistic Lock)
    DB-->>CS: account {balance: 5M, version: 7}
    
    CS->>CS: Fraud Engine\nRisk Score calculation
    
    alt Amount >= 5,000,000 VND
        CS-->>U: 202 Accepted {requiresOtp: true}
        U->>CS: POST /transfers/{id}/confirm-otp {otp: "123456"}
        CS->>REDIS: GET otp:TRANSFER:{phone}
        REDIS-->>CS: "123456" ✓
    end
    
    CS->>DB: BEGIN TRANSACTION
    CS->>DB: UPDATE accounts SET balance=balance-500k,\nversion=8 WHERE id=? AND version=7
    CS->>DB: UPDATE accounts SET balance=balance+500k\nWHERE id=?
    CS->>DB: INSERT INTO ledger_entries (DEBIT + CREDIT)
    CS->>DB: INSERT INTO outbox_events (TRANSFER_COMPLETED)
    CS->>DB: COMMIT
    
    CS-->>U: 200 OK {status: COMPLETED}
    
    CS->>REDIS: DEL account_balance:acc-001 (evict cache)
    
    Note over CS,KAFKA: Async - OutboxPollingService
    CS->>DB: SELECT FROM outbox_events WHERE status=PENDING\nFOR UPDATE SKIP LOCKED
    CS->>KAFKA: Publish transfer.completed event
    CS->>DB: UPDATE outbox_events SET status=SENT
    
    KAFKA->>NOTIF: Consume transfer.completed
    NOTIF->>U: Push notification "Chuyển tiền thành công"
```

---

## 6. Saga Orchestration — Interbank Transfer

```mermaid
stateDiagram-v2
    [*] --> INITIATED: User submits interbank transfer

    INITIATED --> PENDING_OTP: Amount >= 5M VND
    INITIATED --> DEBIT_PROCESSING: Amount < 5M VND (auto)
    PENDING_OTP --> DEBIT_PROCESSING: OTP verified ✓
    PENDING_OTP --> CANCELLED: OTP expired / wrong 3x

    DEBIT_PROCESSING --> DEBIT_COMPLETED: Debit source account ✓
    DEBIT_PROCESSING --> FAILED: Insufficient funds

    DEBIT_COMPLETED --> NAPAS_PENDING: Send to NAPAS/external bank
    NAPAS_PENDING --> CREDIT_COMPLETED: External bank confirms ✓
    NAPAS_PENDING --> REVERSE_DEBIT: External bank rejects / timeout

    CREDIT_COMPLETED --> COMPLETED: ✅ Transfer successful

    REVERSE_DEBIT --> REVERSED: Compensating transaction:\nCredit back to source account
    REVERSED --> FAILED: Notified with reason

    FAILED --> [*]
    COMPLETED --> [*]
    CANCELLED --> [*]

    note right of DEBIT_COMPLETED
        saga_instances table:
        status = DEBIT_COMPLETED
        saga_audit_steps: DEBIT logged
    end note

    note right of REVERSE_DEBIT
        Compensating Transaction:
        INSERT ledger_entry (CREDIT + REVERSE_DEBIT)
        Immutable - never UPDATE/DELETE
    end note
```

---

## 7. Redis Caching Strategy

```mermaid
flowchart TD
    REQ[🌐 API Request\nGET /accounts/balance] --> L1{L1: Caffeine Cache\nTTL 5s}
    L1 -- HIT ✓ --> RESP1[⚡ Return instantly\n~0.1ms]
    L1 -- MISS --> L2{L2: Redis Cache\nTTL 30s}
    L2 -- HIT ✓ --> SETL1[Write to L1\nReturn ~1ms]
    SETL1 --> RESP2[Return to client]
    L2 -- MISS --> DB[(🗄️ PostgreSQL\nSELECT balance)]
    DB --> SETL2[Write to L2 TTL 30s]
    SETL2 --> SETL1B[Write to L1 TTL 5s]
    SETL1B --> RESP3[Return to client ~5ms]

    TRANSFER[💸 Transfer Completed] --> EVICT[🗑️ Evict Redis key:\nDEL account_balance:acc-001]
    EVICT --> NOTE[Next request → Cache Miss\n→ Fresh data from DB]

    style L1 fill:#1a237e,color:#fff
    style L2 fill:#880e4f,color:#fff
    style DB fill:#1b5e20,color:#fff
    style EVICT fill:#b71c1c,color:#fff
```

---

## 8. MVCC — PostgreSQL Concurrency

```mermaid
timeline
    title PostgreSQL MVCC — Hai Transaction Đồng Thời

    section Transaction A (TXN 101)
        t=0 : BEGIN
        t=1 : SELECT balance → sees 5,000,000 VND (snapshot at t=0)
        t=4 : SELECT again → STILL 5,000,000 (snapshot unchanged)
        t=5 : COMMIT

    section Transaction B (TXN 102)
        t=2 : BEGIN
        t=3 : UPDATE balance → 5,500,000 VND (new row version created)
        t=3 : COMMIT

    section Database Storage
        t=3 : Old row version xmin=100 xmax=102 ← Dead tuple after B commits
        t=3 : New row version xmin=102 xmax=0 ← Live tuple
        t=6 : VACUUM cleans old tuple
```

---

*Tất cả diagram được tạo dựa trên code thực tế của Titan BankX Platform*
