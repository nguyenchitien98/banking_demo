# Bài 08 — Signals & State Management (Angular 22)

> **Mục tiêu:** Hiểu Signals, computed(), effect() và tại sao thay thế NgRx cho app nhỏ-vừa  
> **File thực tế:** `auth.service.ts`, `accounts.page.ts`

---

## 1. State Management là Gì?

**State** = Dữ liệu của ứng dụng tại một thời điểm.

```
State của AccountsPage:
├── accounts: BankAccount[]           ← Danh sách tài khoản
├── selectedAccount: BankAccount | null ← Tài khoản đang xem
├── loadingAccounts: boolean          ← Đang tải không?
├── toastMessage: string | null       ← Thông báo hiện tại
└── totalDebitAmount: number          ← Tổng DEBIT
```

**Vấn đề:** Khi state thay đổi → UI phải cập nhật. Làm thế nào?

---

## 2. Signals — Giải Pháp Angular 22

**Signal** = Biến reactive đặc biệt. Khi giá trị thay đổi → Angular biết cần re-render phần HTML nào.

```typescript
// ===== TẠOR SIGNAL =====
import { signal } from '@angular/core';

// signal<T>(giá_trị_ban_đầu)
const count = signal<number>(0);              // Signal số, khởi đầu = 0
const accounts = signal<BankAccount[]>([]);   // Signal mảng, khởi đầu = []
const user = signal<UserSummary | null>(null);// Signal nullable
const isLoading = signal<boolean>(false);     // Signal boolean


// ===== ĐỌC SIGNAL — Gọi như hàm =====
console.log(count());      // 0 (gọi () để đọc giá trị)
console.log(accounts());   // [] (mảng rỗng)

// Trong HTML template cũng gọi bằng ():
// {{ accounts().length }}
// {{ selectedAccount()?.accountNumber }}


// ===== CẬP NHẬT SIGNAL =====

// .set() — Đặt giá trị hoàn toàn mới
count.set(5);           // count() === 5
accounts.set([acc1, acc2]); // Thay toàn bộ mảng

// .update() — Cập nhật dựa trên giá trị cũ
count.update(c => c + 1);  // count() === 6 (cộng thêm 1)
accounts.update(list => [...list, newAccount]); // Thêm phần tử mới

// .mutate() — TRỰC TIẾP sửa object (không tạo bản sao — ít dùng)
// → Không dùng, ưu tiên .set() và .update()
```

---

## 3. Signal trong BankX — Phân Tích accounts.page.ts

```typescript
// accounts.page.ts — toàn bộ signal của trang

export class AccountsPage implements OnInit {

  // ===== SIGNALS — Dữ liệu cần track thay đổi =====
  public accounts        = signal<BankAccount[]>([]);
  public selectedAccount = signal<BankAccount | null>(null);
  public entries         = signal<LedgerEntry[]>([]);
  public totalDebitAmount  = signal<number>(0);
  public totalCreditAmount = signal<number>(0);

  // ===== PROPERTY THƯỜNG — Không cần track (loading state) =====
  public loadingAccounts = true;
  public loadingEntries  = false;
  // Dùng boolean thường vì: loadingAccounts thay đổi ít,
  // và Angular vẫn detect change với properties thường (zone.js)


  // ===== ĐỌC signal trong method .ts =====
  selectAccount(acc: BankAccount): void {
    this.selectedAccount.set(acc);    // CẬP NHẬT signal
    this.loadLedgerEntries(acc.id);
  }

  openDoubleEntryModal(): void {
    const list = this.accounts();     // ĐỌC signal — gọi ()
    if (list.length >= 2) {
      this.debitAccountId  = list[0].id;
      this.creditAccountId = list[1].id;
    }
    this.showDoubleEntryModal = true;
  }

  simulateRaceCondition(): void {
    const acc = this.selectedAccount(); // ĐỌC signal
    if (!acc) return;                   // Kiểm tra null
    // ...
  }

  private calculateProofAmounts(entries: LedgerEntry[]): void {
    let debitSum = 0, creditSum = 0;
    for (const e of entries) {
      if (e.entryType === 'DEBIT')  debitSum  += e.amount;
      if (e.entryType === 'CREDIT') creditSum += e.amount;
    }
    this.totalDebitAmount.set(debitSum);   // Cập nhật signal
    this.totalCreditAmount.set(creditSum); // Cập nhật signal
    // HTML tự động hiển thị số mới
  }
}
```

---

## 4. `computed()` — Signal Phụ Thuộc Signal Khác

```typescript
import { signal, computed } from '@angular/core';

// computed() tạo signal MỚI tự động tính từ signal khác
// Khi signal nguồn thay đổi → computed tự cập nhật

// Ví dụ: Tính tổng tài sản từ danh sách tài khoản
export class DashboardPage {
  accounts = signal<BankAccount[]>([]);

  // computed: Tự động tính lại mỗi khi accounts thay đổi
  totalBalance = computed(() => {
    return this.accounts().reduce((sum, acc) => sum + acc.balance, 0);
    //            ↑ Đọc signal accounts() → computed biết phụ thuộc vào nó
  });

  activeAccountCount = computed(() =>
    this.accounts().filter(a => a.status === 'ACTIVE').length
  );

  hasAccounts = computed(() => this.accounts().length > 0);
}

// Template:
// <span>Tổng tài sản: {{ totalBalance() | number:'1.0-0' }} VND</span>
// <span>{{ activeAccountCount() }} tài khoản đang hoạt động</span>
// @if (hasAccounts()) { ... }
```

**BankX có thể dùng computed() như vậy thay vì tính thủ công:**

```typescript
// THAY VÌ tính thủ công trong calculateProofAmounts():
private calculateProofAmounts(entries: LedgerEntry[]): void {
  let debitSum = 0, creditSum = 0;
  for (const e of entries) { ... }
  this.totalDebitAmount.set(debitSum);
  this.totalCreditAmount.set(creditSum);
}

// CÓ THỂ DÙNG computed():
entries = signal<LedgerEntry[]>([]);

totalDebitAmount = computed(() =>
  this.entries()
    .filter(e => e.entryType === 'DEBIT')
    .reduce((sum, e) => sum + e.amount, 0)
);

totalCreditAmount = computed(() =>
  this.entries()
    .filter(e => e.entryType === 'CREDIT')
    .reduce((sum, e) => sum + e.amount, 0)
);
// Khi entries.set(newList) → totalDebitAmount và totalCreditAmount tự tính lại
```

---

## 5. `effect()` — Side Effect Khi Signal Thay Đổi

```typescript
import { signal, effect } from '@angular/core';

// effect() chạy MỖI KHI bất kỳ signal nào được đọc bên trong nó thay đổi
// Dùng cho: Lưu localStorage, log, trigger animation, sync với external library

export class SettingsPage {
  theme = signal<'dark' | 'light'>('dark');
  language = signal<string>('vi');

  constructor() {
    // effect() phải được gọi trong injection context (constructor)
    effect(() => {
      // Mỗi khi theme thay đổi → tự động áp dụng class cho <body>
      document.body.className = this.theme(); // Đọc signal → effect track
      console.log(`Theme đổi thành: ${this.theme()}`);
    });

    effect(() => {
      // Lưu language vào localStorage mỗi khi thay đổi
      localStorage.setItem('language', this.language());
    });
  }
}
```

---

## 6. Signals vs Trước đây — So Sánh

```typescript
// ===== CÁCH CŨ (Không có Signals — dùng Zone.js) =====

export class OldComponent {
  // Property thường
  accounts: BankAccount[] = [];

  loadAccounts() {
    this.accountService.getMyAccounts().subscribe(res => {
      this.accounts = res.data;
      // Angular dùng Zone.js để detect thay đổi:
      // Patch mọi async operation (setTimeout, fetch, Promise, etc.)
      // Sau mỗi async → Angular check TOÀN BỘ component tree có gì thay đổi không
      // ← Chậm khi app lớn
    });
  }
}

// ===== CÁCH MỚI (Signals — Angular 22) =====

export class NewComponent {
  accounts = signal<BankAccount[]>([]);

  loadAccounts() {
    this.accountService.getMyAccounts().subscribe(res => {
      this.accounts.set(res.data);
      // Angular biết CHÍNH XÁC signal nào thay đổi
      // Chỉ re-render PHẦN HTML đang đọc signal accounts()
      // ← Nhanh hơn đáng kể
    });
  }
}
```

---

## 7. State Chia Sẻ Qua Service Signal

**Pattern hay nhất trong BankX:** Đặt signal trong Service → nhiều component cùng dùng.

```typescript
// auth.service.ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  // Signals được định nghĩa trong Service — SINGLETON
  public currentUser     = signal<UserSummary | null>(null);
  public isAuthenticated = signal<boolean>(false);

  login(credentials: LoginRequest): Observable<...> {
    return this.http.post(...).pipe(
      tap(res => {
        this.currentUser.set(res.data.user);     // Cập nhật signal
        this.isAuthenticated.set(true);           // Cập nhật signal
      })
    );
  }
}

// ===== COMPONENT A: MainLayoutComponent =====
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);

  // Lấy REFERENCE đến signal (không gọi () — không đọc giá trị)
  readonly currentUser = this.authService.currentUser;
  // currentUser là Signal function → trong HTML: {{ currentUser()?.username }}
}

// ===== COMPONENT B: AccountsPage =====
export class AccountsPage {
  private readonly authService = inject(AuthService);

  // Cùng signal, cùng instance AuthService (Singleton)
  readonly isAuthenticated = this.authService.isAuthenticated;
}

// Khi login thành công → authService.currentUser.set(user)
// → MainLayoutComponent tự update (hiện tên user trên header)
// → Tất cả component đọc currentUser đều tự update
```

---

## 8. NgRx — Khi Nào Cần? Khi Nào Không?

**NgRx** là state management library mạnh mẽ hơn Signals (Redux pattern).

```typescript
// NgRx phức tạp hơn nhiều (Action → Reducer → Selector → Store):
// Action:
export const loadAccounts = createAction('[Accounts] Load Accounts');
export const loadAccountsSuccess = createAction(
  '[Accounts] Load Accounts Success',
  props<{ accounts: BankAccount[] }>()
);

// Reducer:
const accountsReducer = createReducer(
  initialState,
  on(loadAccountsSuccess, (state, { accounts }) => ({
    ...state, accounts, loading: false
  }))
);

// Component dùng NgRx Store:
store.dispatch(loadAccounts());
accounts$ = store.select(selectAllAccounts); // Observable

// vs Signals đơn giản hơn nhiều
```

**Khi nào dùng Signals (BankX dùng):**
- App có ít hơn 10-15 màn hình
- State không quá phức tạp
- Team nhỏ, muốn code nhanh
- **BankX hoàn toàn phù hợp với Signals**

**Khi nào nên dùng NgRx:**
- App Enterprise cực lớn (> 50 màn hình)
- Nhiều team làm song song
- Cần time-travel debugging
- Cần audit trail mọi state change

---

## 9. Tổng Hợp — Signals API

```typescript
import { signal, computed, effect } from '@angular/core';

// ===== signal() — Tạo writable signal =====
const count = signal(0);
count();           // ĐỌC: trả về 0
count.set(5);      // GHI: đặt về 5
count.update(n => n + 1); // UPDATE: cộng thêm 1

// ===== computed() — Tạo readonly signal tính từ signal khác =====
const doubled = computed(() => count() * 2);
doubled();         // ĐỌC: 12 (không có .set())

// ===== effect() — Chạy khi signal thay đổi =====
effect(() => {
  console.log(`Count là: ${count()}`); // Tự chạy lại khi count thay đổi
});

// ===== toSignal() — Chuyển Observable sang Signal (Angular 16+) =====
import { toSignal } from '@angular/core/rxjs-interop';

export class MyComponent {
  private readonly accountService = inject(AccountService);

  // Chuyển Observable sang Signal tự động
  accounts = toSignal(
    this.accountService.getMyAccounts(),
    { initialValue: [] }
    // initialValue: Giá trị ban đầu trước khi Observable emit
  );
  // Template: {{ accounts().length }} — không cần | async pipe
}
```

---

## Tổng Kết

| Khái Niệm | Ý Nghĩa | Ví Dụ BankX |
|---|---|---|
| `signal<T>(init)` | Tạo reactive state | `signal<BankAccount[]>([])` |
| `signal()` (gọi hàm) | Đọc giá trị | `accounts()`, `selectedAccount()` |
| `.set(value)` | Đặt giá trị mới | `accounts.set(res.data)` |
| `.update(fn)` | Cập nhật dựa trên giá trị cũ | `count.update(n => n + 1)` |
| `computed()` | Signal tự tính từ signal khác | `totalBalance`, `hasAccounts` |
| `effect()` | Side effect khi signal thay đổi | Lưu localStorage |
| Signal trong Service | Chia sẻ state giữa nhiều component | `currentUser` trong `AuthService` |
| `toSignal()` | Observable → Signal | Không cần `async` pipe |

---

**← [Bài 07 — Routing & Guards](./07_routing_va_route_guards.md)** | **→ [Bài 09 — RxJS & HTTP Client](./09_rxjs_va_http_client.md)**
