# Angular 22 — Tất Cả Tính Năng Mới (Tháng 5/2026)

> **Angular 22 = "Signal-First, Zone-Free" release**  
> Angular phát triển theo hướng: Signals → Zoneless → Simpler APIs  
> So sánh: Angular 16 → 17 → 18 → 19 → 20 → 21 → 22

---

## 📋 Lộ Trình Angular 16 → 22 — Tổng Quan

```
Angular 16 (May 2023):    🟡 Signals (Developer Preview) — Bước đầu tiên
Angular 17 (Nov 2023):    🟢 Signals stable, @if/@for/@switch, defer{}, New project structure
Angular 18 (May 2024):    🟡 Zoneless (Experimental), Material Design 3
Angular 19 (Nov 2024):    🟢 linkedSignal, resource(), Incremental Hydration
Angular 20 (May 2025):    🟢 @let syntax, Signal Forms (preview), Zoneless (opt-in stable)
Angular 21 (Nov 2025):    🟢 Signal Forms stable, effect() cleanup, SSR improvements
Angular 22 (May 2026):    🚀 Full Zoneless production-ready, Signal-first complete architecture
```

---

## 1. ⭐⭐⭐ Zoneless Change Detection — Production Ready

> **Đây là thay đổi lớn nhất trong lịch sử Angular — Loại bỏ Zone.js hoàn toàn**

### Zone.js là gì và Vấn Đề Của Nó?

```
Zone.js: Thư viện "monkey-patch" các async APIs của browser
  - setTimeout, setInterval, Promise, fetch, addEventListener...
  - Mỗi khi async operation xong → Zone.js notify Angular → Angular run change detection
  - Angular biết "cần check UI nào cần update"

Vấn đề Zone.js:
  1. Bundle size: +100KB (13% bundle size của app nhỏ)
  2. Performance: Run change detection cho MỌI async event (ngay cả không liên quan đến UI)
  3. Khó debug: Lỗi trong Zone.js rất cryptic
  4. Không tương thích tốt với Web Workers, Service Workers
  5. Khó integrate với 3rd party libraries không zone-aware
  6. Server-side rendering: Cần workarounds phức tạp
```

### Zoneless Angular — Cách Hoạt Động

```typescript
// Với Zoneless: Change detection CHỈ chạy khi:
//   1. Signal thay đổi giá trị
//   2. markForCheck() được gọi manual
//   3. async pipe emit value mới

// Không còn Zone.js "overhead" chạy CD cho mọi click, setTimeout...

// angular.json / main.ts — Enable Zoneless:
import { bootstrapApplication } from '@angular/platform-browser';
import { provideExperimentalZonelessChangeDetection } from '@angular/core';
// Angular 22: Không còn "Experimental"!
import { provideZonelessChangeDetection } from '@angular/core'; // ← Stable!

bootstrapApplication(AppComponent, {
  providers: [
    provideZonelessChangeDetection(), // ← Angular 22: Zoneless production ready
    provideRouter(routes),
    provideHttpClient(),
  ]
});

// package.json: Không cần zone.js nữa!
// ❌ "zone.js": "~0.14.0"  // Xóa dòng này
// ❌ "zone.js/testing"     // Xóa dòng này

// polyfills.ts:
// ❌ import 'zone.js';  // Không cần nữa!
```

### Components Với Zoneless — Signal-driven

```typescript
// Với Zoneless, components phải dùng Signals (hoặc async pipe) để trigger CD

@Component({
  selector: 'app-account-balance',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush, // ← Vẫn cần OnPush
  template: `
    <div class="balance-card">
      <!-- Signal → Change detection tự động khi balance thay đổi -->
      <h2>{{ balance() | currency:'VND' }}</h2>
      <p>Tài khoản: {{ accountNumber() }}</p>

      @if (isLoading()) {
        <app-skeleton-loader />
      }

      <!-- async pipe vẫn hoạt động và trigger CD -->
      @for (tx of recentTransactions | async; track tx.id) {
        <app-transaction-item [transaction]="tx" />
      }
    </div>
  `
})
export class AccountBalanceComponent {
  private accountService = inject(AccountService);

  accountId = input.required<string>(); // Signal Input

  // Signals → Zoneless reactive UI:
  balance      = this.accountService.getBalance(this.accountId());
  accountNumber = this.accountService.getAccountNumber(this.accountId());
  isLoading    = signal(true);

  // resource() → Auto-fetch và reactive với Signal inputs
  accountData  = resource({
    request: this.accountId, // Reactive request
    loader: ({ request: id }) => this.accountService.loadAccount(id)
  });
}
```

---

## 2. ⭐⭐⭐ Signal Forms (Stable Angular 21+, Enhanced Angular 22)

> **Forms được viết lại hoàn toàn dựa trên Signals — Đơn giản hơn Reactive Forms**

### Vấn Đề Reactive Forms Cũ

```typescript
// Reactive Forms (FormBuilder) — Quá nhiều boilerplate:
@Component({...})
export class TransferFormComponent {
  fb = inject(FormBuilder);

  // Khai báo form:
  transferForm = this.fb.group({
    fromAccount: ['', [Validators.required]],
    toAccount:   ['', [Validators.required, Validators.minLength(10)]],
    amount:      [0,  [Validators.required, Validators.min(1000)]],
    description: [''],
  });

  get fromAccount() { return this.transferForm.get('fromAccount')!; }
  get toAccount()   { return this.transferForm.get('toAccount')!; }

  onSubmit() {
    if (this.transferForm.valid) {
      const value = this.transferForm.value; // Phải TypeCast
    }
  }
}
```

### Signal Forms — Cách Mới (Angular 21+)

```typescript
import { FormField, SignalForm, Validators as V } from '@angular/forms'; // New API

@Component({
  selector: 'app-transfer-form',
  standalone: true,
  template: `
    <form (ngSubmit)="onSubmit()">
      <!-- Signal Form controls bind trực tiếp -->
      <input [formField]="form.fromAccount"
             placeholder="Tài khoản nguồn" />
      @if (form.fromAccount.errors().required) {
        <span class="error">Vui lòng nhập tài khoản nguồn</span>
      }

      <input [formField]="form.amount" type="number"
             placeholder="Số tiền" />
      @if (form.amount.errors().min) {
        <span class="error">Số tiền tối thiểu 1,000 VNĐ</span>
      }

      <!-- Disable button bằng Signal -->
      <button type="submit" [disabled]="!form.valid()">
        Chuyển tiền ({{ form.amount.value() | currency:'VND' }})
        <!-- Real-time preview số tiền! -->
      </button>
    </form>
  `
})
export class TransferFormComponent {
  form = new SignalForm({
    fromAccount: new FormField<string>('', {
      validators: [V.required, V.minLength(10)],
    }),
    toAccount: new FormField<string>('', {
      validators: [V.required, V.minLength(10)],
      asyncValidators: [this.validateAccountExists.bind(this)], // Async validator
    }),
    amount: new FormField<number>(0, {
      validators: [V.required, V.min(1000), V.max(500_000_000)],
    }),
    description: new FormField<string>(''),
  });

  // form.valid() → Signal<boolean> — Tự động reactive!
  // form.fromAccount.value() → Signal<string>
  // form.fromAccount.errors() → Signal<ValidationErrors>

  // Cross-field computed:
  transferSummary = computed(() => ({
    from: this.form.fromAccount.value(),
    to: this.form.toAccount.value(),
    amount: this.form.amount.value(),
    fee: this.form.amount.value() * 0.001,
  }));

  onSubmit() {
    if (this.form.valid()) {
      const values = this.form.value(); // Fully typed! No casting needed
      // values.fromAccount: string
      // values.amount: number
      this.transferService.submit(values);
    }
  }
}
```

---

## 3. ⭐⭐⭐ Resource API (Stable Angular 19, Enhanced 22)

> **Thay thế ngRx Effects và manual HTTP loading state** — Simple reactive data fetching

```typescript
import { resource, ResourceRef } from '@angular/core';

@Component({
  selector: 'app-transactions',
  template: `
    <!-- resource() tự quản lý loading/error/data state -->
    @switch (transactions.status()) {
      @case ('loading') {
        <app-skeleton [rows]="5" />
      }
      @case ('error') {
        <app-error-state [error]="transactions.error()"
                         (retry)="transactions.reload()" />
      }
      @case ('resolved') {
        @for (tx of transactions.value(); track tx.id) {
          <app-transaction-item [transaction]="tx" />
        }
      }
    }
  `
})
export class TransactionsComponent {
  private transactionService = inject(TransactionService);

  // Input signals:
  accountId = input.required<string>();
  pageSize  = input(20);
  page      = signal(1);

  // resource() tự động re-fetch khi inputs thay đổi:
  transactions = resource({
    // request: Signal hoặc computed — Khi thay đổi → re-fetch
    request: computed(() => ({
      accountId: this.accountId(),
      page: this.page(),
      size: this.pageSize(),
    })),
    loader: async ({ request }) => {
      const data = await this.transactionService.getTransactions(
        request.accountId,
        request.page,
        request.size
      );
      return data;
    }
  });

  // transactions.status() → 'idle' | 'loading' | 'error' | 'resolved'
  // transactions.value()  → T | undefined
  // transactions.error()  → unknown
  // transactions.reload() → Trigger manual re-fetch
  // transactions.isLoading() → Signal<boolean>

  nextPage() { this.page.update(p => p + 1); }
  prevPage() { this.page.update(p => p - 1); }
}
```

### rxResource — Cho Observable-based APIs

```typescript
import { rxResource } from '@angular/core/rxjs-interop';

// rxResource: Khi service trả về Observable (HttpClient)
accountDetails = rxResource({
  request: this.accountId,
  loader: ({ request: id }) =>
    this.http.get<AccountDetails>(`/api/v1/accounts/${id}`)
    // Observable → rxResource tự handle subscribe/unsubscribe
});
```

---

## 4. ⭐⭐ linkedSignal — Derived Signal Có Thể Ghi

```typescript
// Signal: Read-only derived (computed)
// WritableSignal: Manually managed
// linkedSignal: DERIVED nhưng CÓ THỂ GHI (reset về derived value)

// Ví dụ BankX: Transfer form với số tiền mặc định theo account
const account = signal<BankAccount | null>(null);

// selectedAmount:
//   - Mặc định = minimum của account (derived từ account signal)
//   - User có thể thay đổi
//   - Khi account thay đổi → Reset về minimum mới
const selectedAmount = linkedSignal({
  source: account,                             // Nguồn
  computation: (acc) => acc?.minimumTransfer ?? 1000  // Tính default
});
// selectedAmount.set(500000) → Ghi giá trị custom
// Khi account signal thay đổi → selectedAmount tự reset về computation()

// Ví dụ khác: Page number reset khi filter thay đổi
const filter = signal({ status: 'ALL', dateRange: 'TODAY' });
const currentPage = linkedSignal({
  source: filter,
  computation: () => 1  // Khi filter thay đổi → Page luôn reset về 1
});
// currentPage.set(3) → Người dùng đi sang trang 3
// filter.set({...}) → currentPage tự reset về 1!
```

---

## 5. ⭐⭐ @let Template Syntax (Angular 20 → Stable 22)

```typescript
// Khai báo biến local trong template (không cần async pipe workaround)

@Component({
  template: `
    <!-- Trước Angular 20: Phải dùng async pipe + as syntax hoặc ngIf workaround -->
    <ng-container *ngIf="account$ | async as account">
      {{ account.name }}
    </ng-container>

    <!-- Angular 20+: @let — Sạch hơn nhiều! -->
    @let account = accountData.value();
    @let isHighBalance = account?.balance > 100_000_000;
    @let displayBalance = account?.balance | currency:'VND';

    @if (account) {
      <div [class.vip]="isHighBalance">
        <h2>{{ account.name }}</h2>
        <p>Số dư: {{ displayBalance }}</p>
        <!-- Dùng lại biến đã khai báo -->
      </div>
    }

    <!-- @let với computed value -->
    @let totalAmount = transactions()
      .reduce((sum, tx) => sum + tx.amount, 0);
    <p>Tổng giao dịch hôm nay: {{ totalAmount | currency:'VND' }}</p>

    <!-- @let scope: Chỉ valid trong phạm vi khai báo -->
    @for (tx of transactions(); track tx.id) {
      @let isDebit = tx.entryType === 'DEBIT';
      @let icon = isDebit ? '↗' : '↙';
      @let colorClass = isDebit ? 'text-red' : 'text-green';
      <div [class]="colorClass">
        {{ icon }} {{ tx.amount | currency:'VND' }}
      </div>
    }
  `
})
class AccountComponent {
  accountData = resource({ ... });
  transactions = signal<Transaction[]>([]);
}
```

---

## 6. ⭐⭐ Incremental Hydration (Angular 19+, Enhanced 22)

> **SSR với lazy hydration — Chỉ hydrate phần nào cần thiết**

```typescript
// Server-Side Rendering với @defer hydration strategy

@Component({
  template: `
    <!-- Phần header: Hydrate ngay khi page load (critical content) -->
    <app-header />
    <app-balance-hero />  <!-- Hydrate ngay -->

    <!-- Transaction list: Chỉ hydrate khi user scroll tới -->
    @defer (hydrate on viewport) {
      <app-transaction-list [accountId]="accountId()" />
    } @placeholder {
      <app-transaction-skeleton />  <!-- SSR renders this initially -->
    }

    <!-- Charts: Chỉ hydrate khi user interact -->
    @defer (hydrate on interaction) {
      <app-analytics-chart />
    } @placeholder {
      <div class="chart-placeholder">📊 Click để xem biểu đồ</div>
    }

    <!-- Fraud alerts: Chỉ hydrate khi cần (idle) -->
    @defer (hydrate on idle) {
      <app-fraud-alerts />
    }
  `
})
class DashboardComponent { ... }

// Hydrate conditions:
// on viewport  → Khi element vào viewport (IntersectionObserver)
// on interaction → Khi user click/focus element
// on idle      → Khi browser idle (requestIdleCallback)
// on timer(5000) → Sau 5 giây
// when condition() → Khi signal condition = true
// never        → Không bao giờ hydrate (pure SSR rendered content)
```

---

## 7. ⭐⭐ Input/Output Improvements

```typescript
// Angular 17+: input() và output() functions (thay @Input/@Output decorators)
// Angular 22: Stable và enhanced

@Component({
  selector: 'app-transfer-card',
  template: `
    <div class="card">
      <h3>{{ title() }}</h3>
      <p>{{ amount() | currency:'VND' }}</p>
      <p [class.urgent]="isUrgent()">{{ status() }}</p>

      <button (click)="onConfirm()">Xác nhận</button>
      <button (click)="onCancel()">Hủy</button>
    </div>
  `
})
export class TransferCardComponent {
  // input() → Signal Input
  title    = input.required<string>();           // Required input
  amount   = input.required<number>();
  status   = input<string>('PENDING');           // Optional với default
  isUrgent = input<boolean>(false);

  // input với transform:
  formattedAmount = input<string, number>(0, {
    transform: (val) => new Intl.NumberFormat('vi-VN').format(val)
    //  ↑ Input nhận number, transform → string
  });

  // model() → Two-way binding Signal (thay [(ngModel)])
  selectedOption = model<string>('');  // Parent dùng [(selectedOption)]="parentVar"

  // output() → Event emitter
  confirmed = output<TransferConfirmEvent>();
  cancelled = output<void>();

  onConfirm() {
    this.confirmed.emit({
      title: this.title(),
      amount: this.amount(),
      timestamp: new Date()
    });
  }

  onCancel() {
    this.cancelled.emit();
  }
}

// Parent usage:
// <app-transfer-card
//   title="Chuyển tiền ăn trưa"
//   [amount]="500000"
//   (confirmed)="handleConfirm($event)"
//   (cancelled)="handleCancel()" />
```

---

## 8. ⭐ Effect() Cleanup — Angular 21+

```typescript
// Angular 21: effect() có cleanup function
// Angular 22: Enhanced với more use cases

@Component({...})
export class RealtimeMetricsComponent implements OnInit {
  private metricsService = inject(MetricsService);

  connectionStatus = signal<'connected' | 'disconnected'>('disconnected');
  metrics          = signal<SystemMetrics | null>(null);

  constructor() {
    // effect() với cleanup — Khi accountId thay đổi hoặc component destroy:
    effect((onCleanup) => {
      const accountId = this.accountId();
      if (!accountId) return;

      // Setup WebSocket subscription:
      const subscription = this.metricsService
        .subscribeToMetrics(accountId)
        .subscribe(data => this.metrics.set(data));

      this.connectionStatus.set('connected');

      // Cleanup khi accountId thay đổi hoặc component destroyed:
      onCleanup(() => {
        subscription.unsubscribe();
        this.connectionStatus.set('disconnected');
        // Angular tự động gọi cleanup khi:
        // 1. Component bị destroy
        // 2. Effect re-runs (accountId thay đổi → Cleanup cũ trước khi setup mới)
      });
    });
  }
}
```

---

## 9. ⭐ Angular 22 Performance Improvements

```typescript
// === Faster Template Compilation ===
// Angular 22: Template compiler nhanh hơn 30%
// Incremental compilation: Chỉ recompile templates có thay đổi

// === Improved @for Track ===
// Angular 17+: track required
// Angular 22: Smarter default tracking
@for (tx of transactions(); track tx.id) {
  <app-transaction [tx]="tx" />
}
// Angular 22 analyze automatic: Biết khi nào cần re-render

// === Signal-based Queries ===
// Thay @ViewChild với signal queries:
@Component({
  template: `<canvas #chartCanvas></canvas>`
})
class ChartComponent {
  // viewChild() → Signal, không cần ngAfterViewInit
  chartCanvas = viewChild<ElementRef>('chartCanvas');
  // chartCanvas() → ElementRef | undefined (undefined trước view init)

  // viewChildren() → Signal<ElementRef[]>
  transactionItems = viewChildren(TransactionItemComponent);
  // Reactive! Khi items thay đổi → Signal tự update

  // contentChild() / contentChildren() → Projected content queries
  cardHeaders = contentChildren(CardHeaderComponent);
}

// === Lazy Loading Improvements ===
// Angular 22: Lazy loading granularity finer
// Component-level code splitting (not just route-level)

// === Build Speed ===
// Angular 22: esbuild + Vite based build pipeline mature
// Build time: 50% faster than Angular 16 webpack-based
```

---

## 10. So Sánh Angular 16 → 22 — Bảng Tổng Hợp

| Feature | 16 | 17 | 18 | 19 | 20 | 21 | 22 |
|---|---|---|---|---|---|---|---|
| Signals | 🟡 Preview | ✅ Stable | ✅ | ✅ | ✅ | ✅ | ✅ Enhanced |
| @if @for @switch | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| defer{} | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Standalone default | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Zoneless | ❌ | ❌ | 🟡 Exp | 🟡 | ✅ Opt-in | ✅ | ✅ **Production** |
| Signal Forms | ❌ | ❌ | ❌ | ❌ | 🟡 Preview | ✅ Stable | ✅ Enhanced |
| resource() | ❌ | ❌ | ❌ | 🟡 Preview | ✅ | ✅ | ✅ Enhanced |
| linkedSignal | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| @let syntax | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| input() / output() | ❌ | 🟡 | ✅ | ✅ | ✅ | ✅ | ✅ |
| effect() cleanup | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Incremental Hydration | ❌ | ❌ | ❌ | 🟡 | ✅ | ✅ | ✅ Enhanced |
| Signal-based queries | ❌ | 🟡 | ✅ | ✅ | ✅ | ✅ | ✅ |
| viewChild() signal | ❌ | 🟡 | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 11. BankX Frontend — Upgrade Checklist Lên Angular 22

```typescript
// ✅ Đã dùng (Angular 17+ features BankX đã có):
// - Standalone components
// - @if @for @switch
// - signal(), computed(), effect()
// - input() output() functions
// - inject() pattern
// - Lazy loading routes

// 🔄 Nên upgrade trong dự án thực tế:

// 1. Enable Zoneless (Angular 22):
// app.config.ts:
providers: [
  provideZonelessChangeDetection(), // Thay provideZone()
]
// Remove zone.js từ package.json và polyfills.ts

// 2. Migrate forms sang Signal Forms:
// Trước:
this.fb.group({ amount: [0, Validators.required] })
// Sau:
new SignalForm({ amount: new FormField(0, { validators: [V.required] }) })

// 3. Thay HTTP + loading state bằng resource():
// Trước: isLoading = false; data = null; error = null; ngOnInit() { http.get... }
// Sau:
accountData = resource({
  request: this.accountId,
  loader: ({ request: id }) => lastValueFrom(this.http.get(`/api/accounts/${id}`))
});

// 4. Thay ngIf as với @let:
// Trước: *ngIf="account$ | async as account"
// Sau: @let account = accountData.value();

// 5. Thêm effect cleanup:
effect((onCleanup) => {
  const sub = websocket.subscribe(data => this.metrics.set(data));
  onCleanup(() => sub.unsubscribe());
});
```

---

## 12. Angular CLI — Lệnh Quan Trọng

```bash
# Tạo project mới (Angular 22):
ng new bankx-frontend
# → Hỏi: Stylesheet format? → SCSS
# → Hỏi: Enable SSR? → Yes (nếu cần)
# → Standalone components: Default (từ Angular 17)
# → Zoneless: Có thể chọn khi tạo project (Angular 22)

# Serve dev:
ng serve
ng serve --open          # Mở browser tự động
ng serve --host 0.0.0.0  # Expose ra network

# Generate:
ng generate component features/accounts/pages/accounts  # Component
ng generate service core/services/account              # Service
ng generate guard core/guards/auth                     # Guard
ng generate interceptor core/interceptors/api          # Interceptor
ng generate pipe shared/pipes/currency-vn              # Pipe

# Build:
ng build                  # Development build
ng build --configuration production  # Production build (optimized)

# Testing:
ng test              # Unit tests (Karma/Jest)
ng e2e               # E2E tests (Cypress/Playwright)

# Analyze bundle:
ng build --stats-json
npx webpack-bundle-analyzer dist/bankx/stats.json

# Update Angular:
ng update @angular/core @angular/cli  # Update tất cả Angular packages
ng update                              # Xem tất cả packages cần update

# Angular 22 specific:
ng generate @angular/core:zoneless    # Migrate project sang zoneless
ng generate @angular/core:signal-forms # Migrate forms sang Signal Forms
```

---

**← [Java 25 Features](./02_java25_new_features.md)** | **← [README](./README.md)**
