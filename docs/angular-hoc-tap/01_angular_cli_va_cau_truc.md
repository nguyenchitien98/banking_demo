# Bài 01 — Angular CLI & Cấu Trúc Dự Án

> **Dành cho:** Người mới bắt đầu Angular  
> **Project thực tế:** Titan BankX Frontend (`frontend-web/`)  
> **Thời gian học:** ~2 giờ

---

## 1. Angular CLI là gì?

**Angular CLI (Command Line Interface)** là công cụ dòng lệnh chính thức của Angular giúp bạn:
- Tạo project mới chỉ với 1 lệnh
- Sinh ra component, service, guard... tự động
- Chạy dev server, build production, chạy tests

**Cài đặt Angular CLI:**

```bash
npm install -g @angular/cli        # Cài toàn cục trên máy
npx ng version                     # Kiểm tra phiên bản (không cần cài global)
```

---

## 2. Các Lệnh CLI Quan Trọng

### 2.1. Tạo Project Mới

```bash
# Tạo project mới tên "my-bank-app"
ng new my-bank-app

# Angular sẽ hỏi bạn:
# ? Which stylesheet format? → SCSS (chọn SCSS giống BankX)
# ? Do you want to enable Server-Side Rendering? → No
```

**BankX dùng lệnh gì khi tạo project?**

```bash
# Tạo project với tất cả cấu hình mặc định, không hỏi
npx -y create-angular@latest frontend-web --routing --style=scss
```

---

### 2.2. ng serve — Chạy Dev Server

```bash
cd frontend-web          # Vào thư mục project

ng serve                 # Chạy dev server tại http://localhost:4200
ng serve --open          # Tự động mở trình duyệt
ng serve --port 3000     # Đổi port sang 3000 nếu 4200 bị chiếm

# BankX dùng:
$env:NG_DISABLE_VERSION_CHECK="true"
npx ng serve --open
```

**Khi thay đổi file `.ts`, `.html`, `.scss` → Angular tự động reload trình duyệt (Hot Module Replacement).**

---

### 2.3. ng build — Build Production Bundle

```bash
ng build                              # Build development (nhanh, có source maps)
ng build --configuration production   # Build production (tối ưu, minify, tree-shaking)

# Output ra thư mục dist/frontend-web/browser/
# BankX dùng:
npx ng build --configuration development
```

**Kết quả build BankX thực tế:**

```
Initial chunk files:
chunk-FZQWFFO3.js   |  1.18 MB  ← Angular core framework
main.js             | 53.20 kB  ← App code của mình
styles.css          |  5.07 kB  ← Global CSS

Lazy chunk files (load khi cần):
chunk-engineering   | 52.78 kB  ← Engineering Portal Page
chunk-transfers     | 99.45 kB  ← Transfer Page
```

> **Lazy Chunks** = Code Splitting. Angular chỉ tải code của trang nào khi user điều hướng đến trang đó → App load nhanh hơn.

---

### 2.4. ng generate — Sinh Code Tự Động

```bash
# Tạo Component mới
ng generate component features/transfers/pages/transfer-form
# Hoặc viết tắt:
ng g c features/transfers/pages/transfer-form

# Tạo Service mới
ng generate service core/services/transfer
ng g s core/services/transfer

# Tạo Interface
ng generate interface core/models/transfer
ng g i core/models/transfer

# Tạo Guard (bảo vệ route)
ng generate guard core/auth/auth
ng g guard core/auth/auth
```

**BankX có cấu trúc thư mục như này (được tạo bằng `ng generate`):**

```
frontend-web/src/app/
├── core/                           ← Module dùng chung toàn app
│   ├── auth/
│   │   ├── auth.service.ts         ← ng g service core/auth/auth
│   │   ├── auth.guard.ts           ← ng g guard core/auth/auth
│   │   └── token.service.ts        ← ng g service core/auth/token
│   ├── services/
│   │   ├── account.service.ts      ← ng g service core/services/account
│   │   ├── transfer.service.ts     ← ng g service core/services/transfer
│   │   └── ...                     ← Các service khác
│   └── http/
│       ├── api.interceptor.ts      ← ng g interceptor core/http/api
│       └── error.interceptor.ts    ← ng g interceptor core/http/error
│
├── features/                       ← Các tính năng (feature modules)
│   ├── auth/                       ← Tính năng Đăng nhập
│   │   └── pages/login/
│   │       ├── login.page.ts       ← ng g component
│   │       ├── login.page.html
│   │       └── login.page.scss
│   ├── accounts/                   ← Tính năng Tài khoản
│   ├── transfers/                  ← Tính năng Chuyển tiền
│   └── ...
│
├── layouts/
│   └── main-layout/                ← Layout chính (Header + Sidebar)
│       ├── main-layout.component.ts
│       ├── main-layout.component.html
│       └── main-layout.component.scss
│
├── app.component.ts                ← Component gốc
├── app.routes.ts                   ← Cấu hình Routing
└── app.config.ts                   ← Cấu hình App (providers, interceptors)
```

---

### 2.5. ng test — Chạy Unit Tests

```bash
ng test                 # Chạy unit tests với Karma + Jasmine
ng test --watch=false   # Chạy 1 lần, không watch
ng test --code-coverage # Xuất báo cáo coverage %
```

---

## 3. Cấu Trúc File Project Angular (BankX)

### Các File Quan Trọng Ở Root:

```
frontend-web/
├── src/
│   ├── app/                ← Toàn bộ code Angular
│   ├── assets/             ← Hình ảnh, fonts, static files
│   ├── styles.scss         ← Global CSS áp dụng cho toàn app
│   ├── index.html          ← HTML gốc (chỉ 1 file, Angular là SPA)
│   └── main.ts             ← Entry point khởi động app
│
├── angular.json            ← Cấu hình Angular CLI (build, serve, test)
├── package.json            ← Dependencies (như pom.xml của Java)
├── tsconfig.json           ← Cấu hình TypeScript compiler
└── proxy.conf.json         ← Proxy dev server (forward /api → backend)
```

### File `main.ts` — Điểm Khởi Động:

```typescript
// frontend-web/src/main.ts

import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent }         from './app/app.component';
import { appConfig }            from './app/app.config';

// bootstrapApplication = "Khởi động Angular App"
// Giống như SpringApplication.run() trong Spring Boot
bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

### File `app.config.ts` — Cấu Hình Providers:

```typescript
// frontend-web/src/app/app.config.ts

import { ApplicationConfig } from '@angular/core';
import { provideRouter }     from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes }            from './app.routes';
import { apiInterceptor }    from './core/http/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Cung cấp Router với các routes đã định nghĩa
    provideRouter(routes),

    // Cung cấp HttpClient với Interceptors
    // apiInterceptor tự động gắn JWT Token vào mọi request
    provideHttpClient(withInterceptors([apiInterceptor])),
  ]
};
```

---

## 4. Standalone Components (Angular 17+, BankX dùng)

**Trước Angular 17:** Phải khai báo component trong `NgModule`.  
**Từ Angular 17 (BankX dùng Angular 22):** Component tự đứng độc lập, không cần NgModule.

```typescript
// Ví dụ từ BankX: accounts.page.ts

@Component({
  selector:     'bankx-accounts-page',  // Tên thẻ HTML của component
  standalone:   true,                   // ← ĐÂY! Khai báo Standalone
  imports: [
    CommonModule,     // Để dùng *ngIf, *ngFor, @if, @for
    FormsModule,      // Để dùng [(ngModel)] — Two-way binding
    DecimalPipe,      // Để format số: {{ 1234567 | number:'1.0-0' }}
    DatePipe,         // Để format ngày: {{ date | date:'dd/MM/yyyy' }}
  ],
  templateUrl: './accounts.page.html',  // File HTML template
  styleUrl:    './accounts.page.scss'   // File SCSS styles
})
export class AccountsPage implements OnInit {
  // Code TypeScript...
}
```

---

## 5. Tổng Kết — Lộ Trình Học Angular BankX

| Thứ tự | Bài học | File liên quan |
|---|---|---|
| Bài 01 (file này) | CLI & Cấu trúc dự án | `angular.json`, `app.config.ts` |
| Bài 02 | TypeScript & Interface | `account.service.ts`, `auth.service.ts` |
| Bài 03 | Component & Lifecycle | `accounts.page.ts`, `main-layout.component.ts` |
| Bài 04 | Template Syntax HTML | `accounts.page.html`, `main-layout.component.html` |
| Bài 05 | SCSS & CSS Flexbox | `main-layout.component.scss`, `accounts.page.scss` |
| Bài 06 | Services & Dependency Injection | `account.service.ts`, `auth.service.ts` |
| Bài 07 | Routing & Route Guards | `app.routes.ts`, `auth.guard.ts` |
| Bài 08 | Signals & State Management | `auth.service.ts`, `accounts.page.ts` |
| Bài 09 | RxJS & HTTP Client | `transfer.service.ts`, `api.interceptor.ts` |
| Bài 10 | Forms & Validation | `login.page.ts`, `transfer.page.ts` |

---

**→ Tiếp theo: [Bài 02 — TypeScript & Interface](./02_typescript_va_interface.md)**
