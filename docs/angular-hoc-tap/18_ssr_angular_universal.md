# Bài 18 — SSR & Angular Universal (Server-Side Rendering)

> **Cấp độ:** Senior  
> **Mục tiêu:** Hiểu SSR, CSR, Hydration — khi nào BankX cần SSR  
> **Ghi chú:** BankX không cần SSR (app yêu cầu login) — bài này dạy để hiểu và biết khi nào áp dụng

---

## 1. SSR vs CSR vs Pre-rendering

```
CSR — Client-Side Rendering (BankX đang dùng):
  1. Browser tải index.html (rỗng, chỉ có <app-root>)
  2. Browser tải main.js (~1.5MB)
  3. Angular chạy, gọi API, render HTML
  ← Ưu điểm: Đơn giản, sau lần đầu nhanh
  ← Nhược điểm: SEO kém, First Contentful Paint chậm

SSR — Server-Side Rendering:
  1. Request → Node.js server chạy Angular, render HTML đầy đủ
  2. Browser nhận HTML có sẵn nội dung → Hiện ngay
  3. Browser tải JS → Hydration (Angular "tiếp quản" HTML từ server)
  ← Ưu điểm: SEO tốt, FCP nhanh
  ← Nhược điểm: Server cần chạy Node.js, phức tạp hơn

Pre-rendering (SSG — Static Site Generation):
  1. Lúc BUILD → Render HTML tĩnh cho từng route
  2. Deploy HTML tĩnh lên CDN
  3. Không cần server Node.js
  ← Tốt cho: Blog, landing page, docs (nội dung ít thay đổi)

Angular Universal = Angular trên server (cho SSR)
```

---

## 2. Khi Nào CẦN SSR?

```
✅ Cần SSR:
  - Public marketing page (SEO quan trọng)
  - E-commerce product catalog (Google crawl)
  - Blog, news (bot cần đọc được nội dung)
  - Landing page cần điểm Lighthouse cao

❌ BankX KHÔNG CẦN SSR (và đây là lý do đúng):
  - App yêu cầu đăng nhập → Bot Google không vào được dù SSR hay không
  - Nội dung cá nhân hóa → Không có "trang tĩnh" để pre-render
  - Data real-time (số dư, giao dịch) → Render trên server vô nghĩa
  - Dùng localStorage (JWT Token) → Không có trên server side

→ BankX đúng khi dùng CSR thuần túy
```

---

## 3. Setup SSR Với Angular 22

```bash
# Thêm SSR vào project Angular:
ng add @angular/ssr

# Hoặc khi tạo project mới:
ng new my-app --ssr

# Angular CLI tạo thêm:
# - server.ts (Express server)
# - app.config.server.ts (config riêng cho server)
# - Sửa angular.json để build 2 targets: browser + server
```

### Cấu Trúc Sau Khi Thêm SSR:

```
src/
├── app/
│   ├── app.config.ts         ← Client-side config
│   └── app.config.server.ts  ← Server-side config
├── main.ts                   ← Browser bootstrap
└── main.server.ts            ← Server bootstrap

server.ts                     ← Express server (điểm vào)
```

---

## 4. app.config.server.ts

```typescript
// app.config.server.ts — Config riêng cho server

import { ApplicationConfig, mergeApplicationConfig } from '@angular/core';
import { provideServerRendering } from '@angular/platform-server';
import { appConfig } from './app.config'; // Dùng chung base config

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(),
    // Thêm providers chỉ cần trên server:
    // - Server-specific HTTP base URL
    // - Mock localStorage (server không có)
  ]
};

// Merge server config vào base config
export const config = mergeApplicationConfig(appConfig, serverConfig);
```

---

## 5. Platform-Aware Code — Code An Toàn Cả Server Lẫn Browser

```typescript
// Problem: Một số API chỉ có ở browser (localStorage, window, document)
// Server (Node.js) KHÔNG CÓ những thứ này → Runtime Error!

// ❌ SAI — Crash trên server:
@Injectable({ providedIn: 'root' })
export class TokenService {
  setToken(token: string): void {
    localStorage.setItem('token', token); // ❌ localStorage undefined trên server!
  }
}

// ✅ ĐÚNG — Check platform trước:
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly platformId = inject(PLATFORM_ID);

  setToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('token', token); // Chỉ chạy trên browser
    }
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('token');
    }
    return null; // Server không có token → trả null
  }
}

// ===== Inject DOCUMENT thay vì dùng document trực tiếp =====
import { DOCUMENT } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  setTheme(theme: string): void {
    this.document.body.classList.toggle('dark-theme', theme === 'dark');
    // document từ DOCUMENT token — Angular cung cấp mock trên server
  }
}
```

---

## 6. Transfer State — Chia Sẻ Data Server → Browser

```typescript
// Vấn đề: SSR render trang với data từ API
// Sau khi Hydration, browser Angular lại gọi API đó LẦN NỮA → 2 requests!

// Giải pháp: TransferState — Truyền data từ server xuống browser qua HTML

import { Injectable, inject } from '@angular/core';
import { makeStateKey, TransferState } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap, of } from 'rxjs';

// Tạo key để lưu state
const ACCOUNTS_KEY = makeStateKey<BankAccount[]>('accounts');

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http          = inject(HttpClient);
  private readonly transferState = inject(TransferState);
  private readonly platformId    = inject(PLATFORM_ID);

  getMyAccounts(): Observable<BankAccount[]> {
    // Kiểm tra data đã có trong transfer state chưa (browser side)
    const cachedData = this.transferState.get(ACCOUNTS_KEY, null);

    if (cachedData) {
      this.transferState.remove(ACCOUNTS_KEY); // Xóa sau khi dùng
      return of(cachedData); // Trả data từ cache — không gọi API lần 2
    }

    return this.http.get<ApiResponse<BankAccount[]>>('/api/v1/accounts').pipe(
      tap(res => {
        if (isPlatformServer(this.platformId)) {
          // Lưu vào transfer state (chỉ trên server)
          // Angular nhúng data này vào HTML, browser đọc được
          this.transferState.set(ACCOUNTS_KEY, res.data);
        }
      }),
      map(res => res.data)
    );
  }
}
```

---

## 7. Server.ts — Express Server

```typescript
// server.ts — Entry point cho SSR server

import 'zone.js/node'; // Zone.js cho Node.js
import { APP_BASE_HREF } from '@angular/common';
import { renderApplication } from '@angular/platform-server';
import express from 'express';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import bootstrap from './src/main.server';

export function app(): express.Express {
  const server = express();
  const distFolder = join(dirname(fileURLToPath(import.meta.url)), 'browser');

  // Serve static files
  server.set('view engine', 'html');
  server.set('views', distFolder);
  server.use(express.static(distFolder, { maxAge: '1y' }));

  // Tất cả routes → Angular SSR
  server.get('*', (req, res) => {
    renderApplication(bootstrap, {
      document: '<app-root></app-root>',
      url: req.url,
      platformProviders: [
        { provide: APP_BASE_HREF, useValue: req.baseUrl }
      ]
    }).then(html => {
      res.send(html); // HTML đầy đủ với nội dung đã render
    });
  });

  return server;
}

// Start server
const server = app();
server.listen(4000, () => {
  console.log('BankX SSR Server: http://localhost:4000');
});
```

---

## 8. Hydration — Angular 22 Feature

```typescript
// Angular 16+ có Non-destructive Hydration
// Trước đây: Sau SSR, browser Angular xóa HTML từ server và render lại (flickering)
// Với Hydration: Browser Angular "tiếp quản" HTML từ server — KHÔNG render lại

// app.config.ts — Bật hydration:
import { provideClientHydration } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration(), // ← Bật non-destructive hydration
    // withEventReplay() — Ghi lại events trước khi hydration xong
  ]
};
```

---

## 9. Pre-rendering (SSG) — Cho Landing Page BankX

```json
// angular.json — Thêm prerender configuration
{
  "prerender": {
    "builder": "@angular-devkit/build-angular:prerender",
    "options": {
      "routes": [
        "/",           // Landing page
        "/about",      // About page
        "/features"    // Features page
      ]
    }
  }
}
```

```bash
# Build với pre-rendering:
ng run frontend-web:prerender

# Output: dist/frontend-web/browser/
# ├── index.html          ← / route pre-rendered
# ├── about/index.html    ← /about pre-rendered
# └── features/index.html ← /features pre-rendered

# Deploy lên Netlify/Vercel/S3 → Không cần Node.js server!
```

---

## 10. Checklist SSR cho BankX (Nếu Muốn Thêm Landing Page)

```typescript
// Scenario: Thêm public landing page cần SEO
// Solution: Hybrid rendering — SSR cho public pages, CSR cho app pages

const routes: Routes = [
  // Public pages — sẽ pre-render
  { path: '',          component: LandingPage },    // → pre-render
  { path: 'features',  component: FeaturesPage },   // → pre-render
  { path: 'pricing',   component: PricingPage },    // → pre-render

  // App pages — CSR (yêu cầu đăng nhập)
  {
    path: 'app',
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardPage }, // → CSR
      { path: 'accounts',  component: AccountsPage },  // → CSR
    ]
  }
];
```

---

## Tổng Kết

| Rendering | SEO | Speed | Server | BankX |
|---|---|---|---|---|
| **CSR** | ❌ | ⚡ sau lần đầu | Không cần | ✅ Đang dùng |
| **SSR** | ✅ | ⚡ First paint | Node.js | Cho landing page |
| **Pre-rendering** | ✅ | ⚡⚡ (CDN) | Không cần | Cho static content |
| **Hybrid** | ✅ | ⚡⚡ | Node.js | Tốt nhất cho enterprise |

**Rule of thumb:**
> - Cần đăng nhập? → **CSR** là đủ  
> - Nội dung public, SEO quan trọng? → **SSR hoặc Pre-rendering**  
> - Blog, docs? → **Pre-rendering (SSG)**  
> - E-commerce lớn? → **Hybrid**

---

**← [Bài 17 — Libraries & Monorepo](./17_custom_libraries_monorepo.md)** | **→ [Bài 19 — Web Workers](./19_web_workers.md)**
