# Bài 03 — Component & Lifecycle Hooks

> **Mục tiêu:** Hiểu Component là gì, 3 file `.ts` / `.html` / `.scss` giao tiếp nhau ra sao  
> **File thực tế:** `accounts.page.ts`, `accounts.page.html`, `accounts.page.scss`

---

## 1. Component là Gì?

**Component = Khối xây dựng cơ bản của Angular.**  
Mỗi màn hình, mỗi phần UI đều là một hoặc nhiều Component lồng nhau.

Mỗi Component gồm đúng 3 file:

```
accounts.page.ts       ← Logic & Data (TypeScript)
accounts.page.html     ← Giao diện (Template HTML)
accounts.page.scss     ← Kiểu dáng (SCSS/CSS)
```

---

## 2. Giải Phẫu File `.ts` — Trái Tim Component

```typescript
// File: accounts.page.ts — Giải thích từng dòng

// ===== PHẦN 1: IMPORTS =====
import { Component, OnInit, inject, signal } from '@angular/core';
//       ↑ Decorator  ↑ Interface ↑ Hàm DI  ↑ State reactive

import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
//       ↑ Directives cơ bản  ↑ Format số    ↑ Format ngày

import { FormsModule } from '@angular/forms';
//       ↑ Cho phép dùng [(ngModel)] two-way binding

import { forkJoin } from 'rxjs';
//       ↑ Chạy song song nhiều Observable, chờ tất cả hoàn thành


// ===== PHẦN 2: DECORATOR @Component — Metadata của Component =====
@Component({
  selector: 'bankx-accounts-page',
  // ↑ Tên thẻ HTML. Dùng component này trong HTML bằng: <bankx-accounts-page />
  // Convention: prefix 'bankx-' để phân biệt với thẻ HTML chuẩn

  standalone: true,
  // ↑ Angular 17+: Component tự đứng độc lập, không cần NgModule
  // ← BankX dùng toàn bộ Standalone Components

  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  // ↑ Khai báo những gì component này SẼ DÙNG trong HTML template
  // Giống như import trong Java — cần gì thì import nấy
  // Nếu quên import → HTML sẽ không nhận ra directive/pipe

  templateUrl: './accounts.page.html',
  // ↑ Trỏ đến file HTML template
  // (Có thể viết inline: template: `<div>...</div>` — nhưng BankX tách file)

  styleUrl: './accounts.page.scss'
  // ↑ Trỏ đến file SCSS styles
  // SCOPE: CSS trong file này CHỈ ảnh hưởng component này, không leak ra ngoài
})


// ===== PHẦN 3: CLASS — Logic & Data =====
export class AccountsPage implements OnInit {
//           ↑ Tên class    ↑ Khai báo implement lifecycle hook

  // --- DEPENDENCY INJECTION ---
  private readonly accountService = inject(AccountService);
  // inject() là cách Angular 22 inject dependency vào component
  // private = chỉ class này dùng
  // readonly = không ai gán lại reference

  private readonly ledgerService = inject(LedgerService);


  // --- STATE (DỮ LIỆU) ---
  public accounts = signal<BankAccount[]>([]);
  // signal() là State Management của Angular 22 (mới, thay NgRx đơn giản)
  // Khi signal thay đổi → HTML tự động re-render phần dùng signal đó

  public selectedAccount = signal<BankAccount | null>(null);
  // BankAccount | null = có thể là BankAccount hoặc null

  public loadingAccounts = true;
  // boolean thường — dùng để hiện spinner loading


  // --- LIFECYCLE HOOK ---
  ngOnInit(): void {
    // Được Angular gọi TỰ ĐỘNG khi component được tạo xong
    // Đây là nơi gọi API load dữ liệu ban đầu
    this.loadAccounts();
  }


  // --- METHODS (Hàm xử lý) ---
  loadAccounts(): void {
    this.loadingAccounts = true;
    // Gọi API → subscribe nhận kết quả
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        this.loadingAccounts = false;
        this.accounts.set(res.data); // Cập nhật signal → HTML tự update
      },
      error: () => {
        this.loadingAccounts = false; // Tắt loading dù có lỗi
      }
    });
  }

  // private = chỉ dùng nội bộ trong class
  private showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      this.toastMessage = null; // Ẩn toast sau 4 giây
    }, 4000);
  }
}
```

---

## 3. Lifecycle Hooks — Vòng Đời Component

Angular tự động gọi các hàm này theo thứ tự:

```typescript
// Thứ tự lifecycle Angular Component:

// 1. constructor() — Được gọi ĐẦU TIÊN, khởi tạo class
//    → Không nên gọi API ở đây, chỉ khởi tạo giá trị đơn giản

// 2. ngOnChanges() — Khi @Input() nhận giá trị mới (nếu có @Input)

// 3. ngOnInit() — SAU KHI component khởi tạo hoàn toàn
//    → ĐÂY là nơi gọi API, khởi tạo dữ liệu
//    → Được gọi 1 lần duy nhất

// 4. ngDoCheck() — Mỗi lần Angular chạy change detection

// 5. ngOnDestroy() — KHI component sắp bị xóa khỏi DOM
//    → Dùng để unsubscribe Observable, clear timer

export class AccountsPage implements OnInit, OnDestroy {
  private timerSub?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    // Gọi API lần đầu
    this.loadAccounts();

    // Bắt đầu auto-refresh mỗi 30 giây
    this.timerSub = setInterval(() => {
      this.loadAccounts();
    }, 30000);
  }

  ngOnDestroy(): void {
    // Dọn dẹp khi component bị destroy — tránh memory leak
    if (this.timerSub) {
      clearInterval(this.timerSub);
    }
  }
}

// ===== TRONG BANKX — engineering-portal.page.ts =====
// Auto-refresh telemetry mỗi 3 giây:
export class EngineeringPortalPage implements OnInit, OnDestroy {
  autoRefreshSub: Subscription | null = null;

  ngOnInit(): void {
    this.fetchHealthSummary();
    // Tạo Observable interval 3000ms
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.fetchHealthSummary(true); // silent refresh
    });
  }

  ngOnDestroy(): void {
    // QUAN TRỌNG: Unsubscribe để tránh memory leak
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }
}
```

---

## 4. Sự Liên Hệ Giữa 3 File `.ts`, `.html`, `.scss`

```
┌──────────────────────────────────────────────────────┐
│                   accounts.page.ts                    │
│                                                      │
│  public accounts = signal<BankAccount[]>([]);         │
│  public loadingAccounts = true;                       │
│                                                      │
│  ① Expose data lên HTML (public)                     │
│  ② Nhận event từ HTML (method như loadAccounts())    │
└──────────────────┬────────────────────┬──────────────┘
                   │ ① Data binding     │ ② Event binding
                   ▼                    ▼
┌──────────────────────────────────────────────────────┐
│                  accounts.page.html                   │
│                                                      │
│  <!-- ① Data hiện ra HTML -->                        │
│  <div>{{ accounts().length }}</div>                   │
│                                                      │
│  <!-- ② Event bắn về .ts -->                         │
│  <button (click)="loadAccounts()">Tải lại</button>  │
│                                                      │
│  <!-- CSS class từ .scss -->                         │
│  <div class="account-card">...</div>                  │
└──────────────────┬───────────────────────────────────┘
                   │ class="account-card"
                   ▼
┌──────────────────────────────────────────────────────┐
│                  accounts.page.scss                   │
│                                                      │
│  .account-card {                                     │
│    background: #1e293b;  /* Màu nền tối BankX */     │
│    border-radius: 12px;  /* Bo tròn góc */           │
│    padding: 16px;                                    │
│  }                                                   │
└──────────────────────────────────────────────────────┘
```

### `.ts` → `.html`: Data Binding

```typescript
// accounts.page.ts
public loadingAccounts = true;     // boolean
public accounts = signal<BankAccount[]>([]); // Signal array
```

```html
<!-- accounts.page.html -->
<!-- Đọc property loadingAccounts từ .ts -->
@if (loadingAccounts) {
  <div>Đang tải...</div>
}

<!-- Đọc signal accounts() - phải gọi như hàm -->
<p>Có {{ accounts().length }} tài khoản</p>
```

### `.html` → `.ts`: Event Binding

```html
<!-- HTML: Gắn event (click) → gọi method bên .ts -->
<button (click)="loadAccounts()">Tải lại</button>
<button (click)="openDoubleEntryModal()">Hạch toán</button>
<button (click)="simulateRaceCondition()">Test Concurrent</button>
```

```typescript
// .ts: Các method được HTML gọi
loadAccounts(): void { /* ... */ }
openDoubleEntryModal(): void { /* ... */ }
simulateRaceCondition(): void { /* ... */ }
```

### `.html` → `.scss`: CSS Class

```html
<!-- HTML dùng class name -->
<div class="account-card active-state">
```

```scss
// SCSS define style cho class đó
.account-card {
  background: rgba(30, 41, 59, 0.6);

  &.active-state {  // & = selector cha (.account-card.active-state)
    border: 2px solid #10b981;
  }
}
```

---

## 5. @Component Selector — Dùng Component Trong HTML

```typescript
// main-layout.component.ts
@Component({
  selector: 'bankx-main-layout',
  // ...
})

// accounts.page.ts
@Component({
  selector: 'bankx-accounts-page',
  // ...
})
```

```html
<!-- Nếu muốn dùng component con trong component cha: -->
<bankx-main-layout>
  <bankx-accounts-page />
</bankx-main-layout>

<!-- Nhưng trong BankX dùng Router → component được load tự động bởi router -->
<!-- <router-outlet /> trong main-layout.html là nơi component con được inject -->
```

---

## 6. `implements OnInit` là Gì?

```typescript
// OnInit là một Interface của Angular:
interface OnInit {
  ngOnInit(): void;  // Phải implement method này
}

// Khi class "implements OnInit":
export class AccountsPage implements OnInit {
  // TypeScript ép buộc phải có method ngOnInit()
  // Nếu không viết ngOnInit() → lỗi compile
  ngOnInit(): void {
    this.loadAccounts(); // Viết logic khởi tạo ở đây
  }
}

// Vậy thì có nhất thiết phải viết "implements OnInit" không?
// - Về mặt kỹ thuật: KHÔNG — Angular sẽ tự tìm và gọi ngOnInit() dù không implements
// - Về mặt best practice: CÓ — giúp TypeScript kiểm tra tên hàm đúng không
//   (tránh lỗi typo: ngOnint thay vì ngOnInit)
```

---

## 7. Một Component Đầy Đủ — Template

```typescript
// my-feature.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MyService, MyData } from '../../core/services/my.service';

@Component({
  selector: 'app-my-feature',        // Tên thẻ
  standalone: true,                  // Standalone component
  imports: [CommonModule],           // Import modules dùng trong HTML
  templateUrl: './my-feature.component.html',
  styleUrl: './my-feature.component.scss'
})
export class MyFeatureComponent implements OnInit {

  // ① Inject services
  private readonly myService = inject(MyService);

  // ② Khai báo state
  public dataList = signal<MyData[]>([]);
  public isLoading = true;
  public errorMessage: string | null = null;

  // ③ Lifecycle hook
  ngOnInit(): void {
    this.loadData();
  }

  // ④ Public methods (HTML gọi được)
  loadData(): void {
    this.isLoading = true;
    this.myService.getData().subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.data) {
          this.dataList.set(res.data);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message;
      }
    });
  }

  // ⑤ Private methods (nội bộ)
  private formatData(data: MyData): string {
    return `${data.name}: ${data.value}`;
  }
}
```

---

## Tổng Kết

| Khái niệm | Ý nghĩa | Ví dụ BankX |
|---|---|---|
| `@Component` | Decorator khai báo metadata | Mọi component trong BankX |
| `selector` | Tên thẻ HTML của component | `'bankx-accounts-page'` |
| `standalone: true` | Component độc lập, không cần NgModule | Mọi component Angular 17+ |
| `imports: []` | Khai báo module/pipe/directive dùng trong template | `[CommonModule, FormsModule]` |
| `ngOnInit()` | Gọi API khởi tạo lần đầu | `accounts.page.ts` |
| `ngOnDestroy()` | Dọn dẹp subscriptions | `engineering-portal.page.ts` |
| `public` | HTML template truy cập được | `accounts`, `loadingAccounts` |
| `private` | Chỉ class nội bộ dùng | `showToast()`, `calculateProof()` |

---

**← [Bài 02 — TypeScript & Interface](./02_typescript_va_interface.md)** | **→ [Bài 04 — Template HTML & Directives](./04_template_html_va_directives.md)**
