# Bài 19 — Web Workers (Heavy Computation Off Main Thread)

> **Cấp độ:** Senior  
> **Mục tiêu:** Chạy tính toán nặng không block UI, hiểu Event Loop, off-main-thread patterns  
> **Liên quan BankX:** Phân tích fraud patterns, tính toán risk score, xử lý bulk CSV export

---

## 1. Tại Sao Cần Web Worker?

```
JavaScript chạy trên 1 thread (Main Thread):
┌────────────────────────────────────────────────┐
│              MAIN THREAD                        │
│                                                │
│  ┌─────────┐  ┌─────────┐  ┌────────────────┐ │
│  │ UI Render│  │ Events  │  │ Heavy Task?    │ │
│  │ (Angular)│  │ (click) │  │ → BLOCKS UI!   │ │
│  └─────────┘  └─────────┘  └────────────────┘ │
└────────────────────────────────────────────────┘

Vấn đề: Nếu JavaScript tính toán nặng 5 giây
→ UI bị ĐÓNG BĂNG 5 giây → Không click, không scroll, không gì cả

Web Worker giải quyết:
┌────────────────────────────────────────────────┐
│              MAIN THREAD                        │
│  UI Render + Events → Mượt mà, không bị block  │
│                 ↕ postMessage                  │
│              WEB WORKER THREAD                  │
│  Heavy computation chạy song song ở đây        │
└────────────────────────────────────────────────┘
```

**Tình huống BankX cần Web Worker:**
- Phân tích 10.000 giao dịch để tìm pattern fraud
- Tính toán risk score phức tạp cho bulk transactions
- Parse và validate file CSV 50MB do user upload
- Nén/giải nén data lớn

---

## 2. Tạo Web Worker Trong Angular

```bash
# Angular CLI tạo Web Worker tự động:
ng generate web-worker fraud-analysis
# Tạo: src/app/workers/fraud-analysis.worker.ts
# Cập nhật: tsconfig.worker.json
```

---

## 3. Worker File — Logic Tính Toán Nặng

```typescript
// File: src/app/workers/fraud-analysis.worker.ts

/// <reference lib="webworker" />
// ↑ Khai báo TypeScript: file này chạy trong Web Worker context
// → Có access đến: self, postMessage, addEventListener
// → KHÔNG có access đến: DOM, window, document, Angular services

// ===== Worker lắng nghe message từ Main Thread =====
addEventListener('message', ({ data }) => {
  const { type, payload } = data;

  switch (type) {
    case 'ANALYZE_TRANSACTIONS':
      const result = analyzeTransactionPatterns(payload.transactions);
      postMessage({ type: 'ANALYSIS_COMPLETE', result });
      break;

    case 'CALCULATE_RISK_SCORE':
      const score = calculateRiskScore(payload.transaction, payload.rules);
      postMessage({ type: 'RISK_SCORE_CALCULATED', score, transactionId: payload.transaction.id });
      break;

    case 'PARSE_CSV':
      const parsed = parseTransactionCSV(payload.csvContent);
      postMessage({ type: 'CSV_PARSED', transactions: parsed });
      break;
  }
});


// ===== Hàm tính toán nặng — chạy trong Worker thread =====

interface Transaction {
  id: string;
  amount: number;
  timestamp: string;
  accountId: string;
  deviceId: string;
  ipAddress: string;
  merchantCategory: string;
}

interface FraudPattern {
  pattern: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  transactionIds: string[];
}

function analyzeTransactionPatterns(transactions: Transaction[]): FraudPattern[] {
  const patterns: FraudPattern[] = [];

  // ===== Pattern 1: Nhiều giao dịch nhỏ liên tiếp (Card Testing) =====
  const smallTransactions = transactions.filter(t => t.amount < 50000);
  const groupedByAccount = groupBy(smallTransactions, t => t.accountId);

  for (const [accountId, txs] of Object.entries(groupedByAccount)) {
    if (txs.length > 10) { // Hơn 10 giao dịch nhỏ trong 1 ngày
      patterns.push({
        pattern: 'CARD_TESTING',
        riskLevel: 'HIGH',
        transactionIds: txs.map(t => t.id)
      });
    }
  }

  // ===== Pattern 2: Giao dịch từ nhiều địa điểm khác nhau trong thời gian ngắn =====
  const groupedByUser = groupBy(transactions, t => t.accountId);
  for (const [accountId, txs] of Object.entries(groupedByUser)) {
    const uniqueIPs = new Set(txs.map(t => t.ipAddress));
    if (uniqueIPs.size > 5) { // Hơn 5 IP khác nhau trong ngày
      patterns.push({
        pattern: 'MULTIPLE_LOCATIONS',
        riskLevel: 'CRITICAL',
        transactionIds: txs.map(t => t.id)
      });
    }
  }

  // ===== Pattern 3: Giao dịch lớn bất thường (> 50 triệu) =====
  const largeTransactions = transactions.filter(t => t.amount > 50_000_000);
  if (largeTransactions.length > 0) {
    patterns.push({
      pattern: 'LARGE_AMOUNT',
      riskLevel: 'HIGH',
      transactionIds: largeTransactions.map(t => t.id)
    });
  }

  return patterns;
}

function calculateRiskScore(transaction: Transaction, rules: any[]): number {
  let score = 0;

  // Tính toán phức tạp — có thể mất 100-500ms
  for (const rule of rules) {
    if (rule.evaluate(transaction)) {
      score += rule.weight;
    }
  }

  // Chạy ML model nhẹ (nếu có)
  score += mlScoreEstimate(transaction);

  return Math.min(100, Math.max(0, score));
}

function parseTransactionCSV(csvContent: string): Transaction[] {
  const lines = csvContent.split('\n');
  const headers = lines[0].split(',');

  return lines.slice(1)
    .filter(line => line.trim())
    .map(line => {
      const values = line.split(',');
      return headers.reduce((obj: any, header, i) => {
        obj[header.trim()] = values[i]?.trim();
        return obj;
      }, {} as Transaction);
    });
}

// Utility
function groupBy<T>(arr: T[], keyFn: (item: T) => string): Record<string, T[]> {
  return arr.reduce((groups: Record<string, T[]>, item) => {
    const key = keyFn(item);
    if (!groups[key]) groups[key] = [];
    groups[key].push(item);
    return groups;
  }, {});
}

function mlScoreEstimate(tx: Transaction): number {
  // Simplified ML estimation (no external model)
  let estimate = 0;
  const hour = new Date(tx.timestamp).getHours();
  if (hour < 6 || hour > 22) estimate += 15; // Giao dịch đêm
  if (tx.amount > 10_000_000) estimate += 10; // Số tiền lớn
  return estimate;
}
```

---

## 4. Service — Giao Tiếp Với Web Worker

```typescript
// File: src/app/core/services/fraud-analysis.service.ts

import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class FraudAnalysisService {

  // Tạo Web Worker
  private worker: Worker | null = null;

  // Subject để broadcast messages từ worker
  private workerMessages$ = new Subject<{ type: string; [key: string]: any }>();

  constructor() {
    // Worker chỉ có ở browser (không có ở SSR)
    if (typeof Worker !== 'undefined') {
      this.worker = new Worker(
        new URL('../../workers/fraud-analysis.worker', import.meta.url),
        { type: 'module' }
        // type: 'module' → Worker hỗ trợ ES modules
      );

      // Lắng nghe messages từ worker
      this.worker.onmessage = ({ data }) => {
        this.workerMessages$.next(data);
      };

      // Xử lý lỗi trong worker
      this.worker.onerror = (error) => {
        console.error('Worker error:', error);
        this.workerMessages$.next({ type: 'ERROR', error: error.message });
      };
    }
  }

  // ===== PUBLIC API =====

  analyzeTransactions(transactions: Transaction[]): Observable<FraudPattern[]> {
    // Gửi message đến worker
    this.worker?.postMessage({
      type: 'ANALYZE_TRANSACTIONS',
      payload: { transactions }
    });

    // Trả về Observable, complete khi nhận được kết quả
    return this.workerMessages$.pipe(
      filter(msg => msg['type'] === 'ANALYSIS_COMPLETE'),
      map(msg => msg['result']),
      // take(1) — Chỉ nhận 1 response rồi complete
    );
  }

  calculateRiskScore(transaction: Transaction, rules: any[]): Observable<number> {
    this.worker?.postMessage({
      type: 'CALCULATE_RISK_SCORE',
      payload: { transaction, rules }
    });

    return this.workerMessages$.pipe(
      filter(msg => msg['type'] === 'RISK_SCORE_CALCULATED'
                 && msg['transactionId'] === transaction.id),
      map(msg => msg['score'])
    );
  }

  parseCSV(csvContent: string): Observable<Transaction[]> {
    this.worker?.postMessage({
      type: 'PARSE_CSV',
      payload: { csvContent }
    });

    return this.workerMessages$.pipe(
      filter(msg => msg['type'] === 'CSV_PARSED'),
      map(msg => msg['transactions'])
    );
  }

  // Dọn dẹp khi không cần nữa
  destroy(): void {
    this.worker?.terminate();
    this.workerMessages$.complete();
  }
}
```

---

## 5. Component Dùng Worker Service

```typescript
// fraud-monitoring.page.ts

@Component({
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="fraud-dashboard">
      <div class="controls">
        <button (click)="startAnalysis()" [disabled]="analyzing()">
          @if (analyzing()) {
            <span class="spinner"></span> Đang phân tích {{ progress() }}%...
          } @else {
            🔍 Phân tích Fraud Patterns
          }
        </button>

        <input type="file" (change)="onFileUpload($event)" accept=".csv">
      </div>

      @if (patterns().length > 0) {
        <div class="results">
          <h3>Phát hiện {{ patterns().length }} patterns</h3>
          @for (pattern of patterns(); track pattern.pattern) {
            <div class="pattern-card" [class]="pattern.riskLevel.toLowerCase()">
              <span class="pattern-name">{{ pattern.pattern }}</span>
              <span class="risk">{{ pattern.riskLevel }}</span>
              <span class="count">{{ pattern.transactionIds.length }} giao dịch</span>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class FraudMonitoringPage implements OnDestroy {
  private readonly fraudService = inject(FraudAnalysisService);
  private readonly transactionService = inject(TransactionService);

  analyzing = signal(false);
  progress = signal(0);
  patterns = signal<FraudPattern[]>([]);

  startAnalysis(): void {
    this.analyzing.set(true);
    this.progress.set(0);

    // Lấy transactions rồi gửi cho worker phân tích
    this.transactionService.getAllTransactions().subscribe(transactions => {
      this.progress.set(30); // Đã lấy data

      // Worker thực hiện phân tích — UI KHÔNG BỊ BLOCK!
      this.fraudService.analyzeTransactions(transactions.data).subscribe({
        next: (patterns) => {
          this.patterns.set(patterns);
          this.analyzing.set(false);
          this.progress.set(100);
        },
        error: () => {
          this.analyzing.set(false);
        }
      });

      this.progress.set(60); // Worker đang chạy
    });
  }

  onFileUpload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const csvContent = e.target?.result as string;

      // Parse CSV trong Worker — không block UI khi file 50MB
      this.fraudService.parseCSV(csvContent).subscribe(transactions => {
        console.log(`Parsed ${transactions.length} transactions from CSV`);
        this.startAnalysisWithTransactions(transactions);
      });
    };
    reader.readAsText(file);
  }

  private startAnalysisWithTransactions(transactions: Transaction[]): void {
    this.fraudService.analyzeTransactions(transactions).subscribe(
      patterns => this.patterns.set(patterns)
    );
  }

  ngOnDestroy(): void {
    this.fraudService.destroy(); // Dừng worker
  }
}
```

---

## 6. SharedArrayBuffer & Atomics — Chia Sẻ Memory (Nâng Cao)

```typescript
// Khi cần chia sẻ dữ liệu LỚN giữa Main Thread và Worker
// Truyền qua postMessage phải COPY data → Tốn memory với data lớn

// SharedArrayBuffer: Vùng nhớ dùng chung — KHÔNG copy
// Atomics: Đồng bộ hóa truy cập vào SharedArrayBuffer

// Main thread:
const sharedBuffer = new SharedArrayBuffer(1024 * 1024); // 1MB shared memory
const view = new Int32Array(sharedBuffer);

// Ghi data vào shared buffer:
data.forEach((val, i) => view[i] = val);

// Gửi REFERENCE (không copy data):
worker.postMessage({ buffer: sharedBuffer });

// Worker:
addEventListener('message', ({ data }) => {
  const view = new Int32Array(data.buffer);
  // Worker đọc/ghi vào cùng vùng nhớ với main thread
  Atomics.add(view, 0, 1); // Thread-safe increment
});

// ⚠️ Cần headers HTTP để dùng SharedArrayBuffer:
// Cross-Origin-Opener-Policy: same-origin
// Cross-Origin-Embedder-Policy: require-corp
```

---

## 7. Comlink — Làm Việc Với Worker Đơn Giản Hơn

```typescript
// Comlink: Thư viện giúp gọi Worker như gọi hàm thông thường

// npm install comlink

// worker.ts:
import * as Comlink from 'comlink';

const api = {
  analyzeTransactions(transactions: Transaction[]): FraudPattern[] {
    return analyzeTransactionPatterns(transactions);
  },
  calculateRiskScore(tx: Transaction): number {
    return calculateRiskScore(tx, defaultRules);
  }
};

Comlink.expose(api); // Expose API ra ngoài


// main.ts (service):
import * as Comlink from 'comlink';
import { wrap } from 'comlink';

const worker = new Worker(new URL('./fraud.worker', import.meta.url));
const api = Comlink.wrap<typeof workerApi>(worker);

// Gọi worker như gọi hàm async thông thường — không cần postMessage/onmessage!
const patterns = await api.analyzeTransactions(transactions);
const score    = await api.calculateRiskScore(transaction);
```

---

## 8. Performance Comparison — Với Và Không Worker

```typescript
// Benchmark: Phân tích 50.000 giao dịch

// ❌ Không có Worker:
console.time('no-worker');
const patterns = analyzeTransactionPatterns(transactions); // Chạy trên main thread
console.timeEnd('no-worker'); // → ~3500ms — UI ĐÓNG BĂNG 3.5 giây!

// ✅ Với Worker:
// Main thread: gửi data → nhận kết quả = ~5ms
// Worker thread: ~3500ms (chạy song song, UI không bị ảnh hưởng)
// User VẪNN có thể click, scroll, tương tác trong 3.5 giây đó
```

---

## 9. Khi Nào Dùng Web Worker?

```
✅ Dùng Worker khi:
  - Tính toán > 50ms liên tục (gây drop frames)
  - Parse file lớn (CSV, JSON > 1MB)
  - Nén/giải nén data
  - Chạy WebAssembly module
  - Image processing

❌ KHÔNG cần Worker khi:
  - HTTP requests (đã async, không block UI)
  - Database queries (async)
  - Hầu hết operations < 50ms

⚠️ Hạn chế của Worker:
  - Không có DOM access
  - Không có Angular services (phải tự inject)
  - Data transfer overhead (postMessage copy data)
  - Debugging khó hơn
```

---

## Tổng Kết

| Khái Niệm | Mô Tả |
|---|---|
| `new Worker(url)` | Tạo worker thread |
| `postMessage(data)` | Gửi message từ main → worker |
| `addEventListener('message')` | Worker lắng nghe message |
| `self.postMessage(result)` | Worker gửi kết quả về main |
| `worker.terminate()` | Dừng worker |
| `SharedArrayBuffer` | Shared memory giữa threads |
| `Atomics` | Thread-safe operations |
| `Comlink` | Wrapper gọn gàng hơn cho Worker |

---

**← [Bài 18 — SSR & Universal](./18_ssr_angular_universal.md)** | **→ [Bài 20 — Micro-Frontends](./20_micro_frontends.md)**
