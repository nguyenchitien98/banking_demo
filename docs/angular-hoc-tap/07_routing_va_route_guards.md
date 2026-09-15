# Bài 07 — Routing & Route Guards

> **Mục tiêu:** Hiểu Angular Router, Lazy Loading, và bảo vệ route bằng Guard  
> **File thực tế:** `app.routes.ts`, `auth.guard.ts`, `main-layout.component.html`

---

## 1. Routing là Gì? SPA và URL

**Angular là SPA (Single Page Application):**

```
Ứng dụng web thông thường (Multi-Page):
  User click link → Browser tải trang MỚI từ server → Reload hoàn toàn

Angular SPA:
  User click link → Angular Router thay đổi component hiển thị → KHÔNG tải lại trang
  URL thay đổi nhưng không có request server nào được gửi
```

```
URL: http://localhost:4200/dashboard    → DashboardPage component
URL: http://localhost:4200/accounts    → AccountsPage component
URL: http://localhost:4200/transfers   → TransfersPage component
URL: http://localhost:4200/auth/login  → LoginPage component
```

---

## 2. Giải Phẫu File `app.routes.ts`

```typescript
// File: frontend-web/src/app/app.routes.ts

import { Routes } from '@angular/router';
// Routes = kiểu TypeScript cho mảng route config

import { authGuard } from './core/auth/auth.guard';
// authGuard: Kiểm tra user đã đăng nhập chưa trước khi vào route

// ===== ĐỊNH NGHĨA ROUTES =====
export const routes: Routes = [

  // ===== ROUTE 1: Redirect / → /dashboard =====
  {
    path: '',                  // URL: http://localhost:4200/
    redirectTo: 'dashboard',  // Chuyển hướng đến dashboard
    pathMatch: 'full',        // Chỉ match khi URL CHÍNH XÁC là ''
    //          ↑ Quan trọng! Nếu không có pathMatch: 'full',
    //            '' sẽ match MỌI URL (vì mọi URL đều bắt đầu bằng '')
  },

  // ===== ROUTE 2: Auth pages (Login, Register) =====
  {
    path: 'auth',             // URL: /auth/*
    // Lazy Loading: Chỉ tải code Auth khi user đến trang Auth
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
    //   ↑ Dynamic import — tải module theo yêu cầu, không tải ngay từ đầu
    //     auth.routes.ts chứa routes con: /auth/login, /auth/register...
  },

  // ===== ROUTE 3: Protected routes (cần đăng nhập) =====
  {
    path: '',                          // URL gốc (layout wrap)
    loadComponent: () =>
      import('./layouts/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    // loadComponent: Lazy load 1 component cụ thể (MainLayout)
    // MainLayout chứa Header + Sidebar + <router-outlet>

    canActivate: [authGuard],
    // canActivate: Danh sách Guards phải pass để vào route này
    // authGuard kiểm tra: Có JWT token không?
    //   - Có → Cho vào
    //   - Không → Redirect đến /auth/login

    // Route con (hiển thị TRONG <router-outlet> của MainLayout):
    children: [
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(
            (m) => m.DASHBOARD_ROUTES
          ),
      },
      {
        path: 'accounts',
        loadChildren: () =>
          import('./features/accounts/accounts.routes').then(
            (m) => m.ACCOUNTS_ROUTES
          ),
      },
      {
        path: 'transfers',
        loadChildren: () =>
          import('./features/transfers/transfers.routes').then(
            (m) => m.TRANSFERS_ROUTES
          ),
      },
      // ... các route khác: payments, cards, notifications, fraud...
    ],
  },

  // ===== ROUTE CUỐI: Wildcard — Bắt tất cả URL không tồn tại =====
  {
    path: '**',               // ** = match mọi URL không khớp route nào ở trên
    redirectTo: 'dashboard',  // Redirect về dashboard thay vì 404
  },
];
```

---

## 3. Lazy Loading — Tại Sao Quan Trọng?

```typescript
// ===== KHÔNG dùng Lazy Loading (Eager Loading) =====
// Angular tải TẤT CẢ code ngay khi app khởi động
// → Initial bundle lớn → App load chậm

// ===== CÓ Lazy Loading (BankX dùng) =====
loadChildren: () =>
  import('./features/transfers/transfers.routes').then((m) => m.TRANSFERS_ROUTES)
// → Chỉ tải code Transfers khi user đến /transfers
// → Initial bundle nhỏ → App load nhanh

// Kết quả build BankX với Lazy Loading:
// main.js               53 kB   ← Chỉ core framework + routing
// chunk-transfers.js    99 kB   ← Chỉ tải khi vào /transfers
// chunk-engineering.js  52 kB   ← Chỉ tải khi vào /monitoring/engineering
// chunk-admin.js        45 kB   ← Chỉ tải khi vào /admin
```

---

## 4. Route Guard — Bảo Vệ Route

```typescript
// File: frontend-web/src/app/core/auth/auth.guard.ts

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenService } from './token.service';

// Guard là function (functional guard — Angular 14+)
// Nhận vào route và state, trả về: boolean | UrlTree
export const authGuard: CanActivateFn = (_route, state) => {
  //                                      ↑ route info  ↑ current router state

  const tokenService = inject(TokenService);
  const router       = inject(Router);

  if (tokenService.hasToken()) {
    return true;  // ✅ Có token → cho vào route
  }

  // ❌ Không có token → Redirect đến login
  // createUrlTree: Tạo URL redirect an toàn
  // queryParams: Lưu lại URL hiện tại → sau khi login quay về URL cũ
  return router.createUrlTree(
    ['/auth/login'],
    { queryParams: { returnUrl: state.url } }
  );
  // Ví dụ: User vào /transfers → redirect đến /auth/login?returnUrl=/transfers
  //         Sau khi login xong → quay về /transfers
};
```

**Đăng ký Guard trong routes:**

```typescript
// app.routes.ts
{
  path: '',
  canActivate: [authGuard], // ← Guard chạy TRƯỚC khi render component
  // ...
}
```

**Các loại Guard Angular:**

```typescript
// canActivate — Kiểm tra trước khi VÀO route
canActivate: [authGuard]

// canDeactivate — Kiểm tra trước khi RỜI route
// Dùng cho: "Bạn có chắc muốn rời trang? Dữ liệu chưa lưu."
canDeactivate: [unsavedChangesGuard]

// canActivateChild — Kiểm tra trước khi vào route CON
canActivateChild: [authGuard]

// resolve — Tải dữ liệu TRƯỚC KHI render component
// Route sẽ không render cho đến khi dữ liệu sẵn sàng
resolve: { account: accountResolver }
```

---

## 5. Router trong HTML Template

```html
<!-- File: main-layout.component.html -->

<!-- routerLink — Điều hướng khi click (thay thế href) -->
<a routerLink="/dashboard" class="nav-item">Dashboard</a>
<a routerLink="/accounts" class="nav-item">Tài khoản</a>
<a routerLink="/transfers" class="nav-item">Chuyển tiền</a>

<!-- routerLinkActive — Tự động thêm class khi route đang active -->
<a routerLink="/dashboard"
   routerLinkActive="active"
   class="nav-item">Dashboard</a>
<!-- Khi URL là /dashboard → thêm class "active" → CSS highlight nav item -->

<!-- routerLinkActiveOptions — Tùy chỉnh exact matching -->
<a routerLink="/"
   routerLinkActive="active"
   [routerLinkActiveOptions]="{ exact: true }">
<!-- exact: true → Chỉ active khi URL chính xác là "/" -->
<!-- Không có exact → "/" sẽ active với mọi URL (vì mọi URL đều bắt đầu bằng /) -->

<!-- router-outlet — Nơi component con được inject -->
<router-outlet />
<!-- Khi URL là /accounts → AccountsPage được render ở đây -->
<!-- Khi URL là /transfers → TransfersPage được render ở đây -->
```

---

## 6. Router trong TypeScript — Điều Hướng Bằng Code

```typescript
// main-layout.component.ts

import { Router } from '@angular/router';

export class MainLayoutComponent {
  private readonly router = inject(Router);

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']); // Điều hướng bằng code sau logout
  }

  goToTransfer(accountId: string): void {
    // Điều hướng với params
    this.router.navigate(['/transfers'], {
      queryParams: { fromAccount: accountId }
    });
    // URL: /transfers?fromAccount=acc-001
  }

  goToAccountDetail(id: string): void {
    // Route params
    this.router.navigate(['/accounts', id]);
    // URL: /accounts/acc-001
  }
}
```

---

## 7. Route Parameters — Nhận Dữ Liệu Từ URL

```typescript
// ===== Định nghĩa route với param =====
// accounts.routes.ts
export const ACCOUNTS_ROUTES: Routes = [
  {
    path: '',
    component: AccountsPage      // /accounts
  },
  {
    path: ':id',
    component: AccountDetailPage  // /accounts/:id (id là dynamic)
  }
];

// ===== Nhận param trong Component =====
import { ActivatedRoute } from '@angular/router';

export class AccountDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);

  ngOnInit() {
    // Lấy route param :id
    const accountId = this.route.snapshot.paramMap.get('id');
    // Ví dụ URL /accounts/acc-001 → accountId = 'acc-001'

    if (accountId) {
      this.loadAccountDetail(accountId);
    }
  }
}

// ===== Query Params =====
// URL: /transfers?fromAccount=acc-001

export class TransfersPage implements OnInit {
  private readonly route = inject(ActivatedRoute);

  ngOnInit() {
    const fromAccount = this.route.snapshot.queryParamMap.get('fromAccount');
    // fromAccount = 'acc-001'
  }
}
```

---

## 8. Feature Routes — Cách Tổ Chức Routes Theo Feature

```typescript
// File: accounts.routes.ts (routes riêng cho feature Accounts)

import { Routes } from '@angular/router';

export const ACCOUNTS_ROUTES: Routes = [
  {
    path: '',            // /accounts → accounts page
    loadComponent: () =>
      import('./pages/accounts/accounts.page')
        .then((m) => m.AccountsPage),
  },
  // Thêm routes khác nếu cần:
  // { path: ':id', component: AccountDetailPage }
];

// ===== Tại sao tách routes theo feature? =====
// 1. Dễ maintain — feature A không ảnh hưởng feature B
// 2. Code splitting tốt hơn — lazy load theo feature
// 3. Team có thể làm song song (feature A và B không conflict)
```

---

## 9. Layout Route Pattern — BankX Architecture

```
App
├── /auth/*              ← KHÔNG qua MainLayout (trang login không cần header/sidebar)
│   └── AuthLayout
│       ├── /auth/login
│       └── /auth/register
│
└── /*                   ← QUA MainLayout (cần đăng nhập)
    └── MainLayout (Header + Sidebar + <router-outlet>)
        ├── /dashboard      ← DashboardPage  hiện trong <router-outlet>
        ├── /accounts       ← AccountsPage   hiện trong <router-outlet>
        ├── /transfers      ← TransfersPage  hiện trong <router-outlet>
        ├── /payments       ← PaymentsPage   hiện trong <router-outlet>
        ├── /cards          ← CardsPage      hiện trong <router-outlet>
        └── /notifications  ← NotificationsPage hiện trong <router-outlet>
```

**Đây là "Nested Routes" pattern — BankX dùng:**

```typescript
// Parent route (MainLayout):
{
  path: '',
  component: MainLayoutComponent,  // Luôn render
  canActivate: [authGuard],
  children: [                      // Con render TRONG <router-outlet> của MainLayout
    { path: 'dashboard', component: DashboardPage },
    { path: 'accounts', component: AccountsPage },
  ]
}
```

---

## Tổng Kết

| Khái Niệm | Tác Dụng | Ví Dụ BankX |
|---|---|---|
| `Routes` | Cấu hình mapping URL → Component | `app.routes.ts` |
| `redirectTo` | Chuyển hướng URL | `/` → `/dashboard` |
| `pathMatch: 'full'` | Match chính xác URL | Tránh match mọi URL |
| `loadChildren` | Lazy load feature module | `accounts.routes` |
| `loadComponent` | Lazy load 1 component | `MainLayoutComponent` |
| `canActivate` | Guard kiểm tra trước khi vào | `authGuard` |
| `path: '**'` | Wildcard — catch-all | Redirect 404 về dashboard |
| `routerLink` | Điều hướng trong template | Nav sidebar |
| `routerLinkActive` | Class active khi route khớp | Highlight nav item |
| `<router-outlet>` | Nơi render component con | MainLayout |
| `ActivatedRoute` | Đọc params/query từ URL | `:id`, `?fromAccount=` |

---

**← [Bài 06 — Services & DI](./06_services_va_dependency_injection.md)** | **→ [Bài 08 — Signals & State Management](./08_signals_va_state_management.md)**
