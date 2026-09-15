# Bài 20 — Micro-Frontends (Module Federation)

> **Cấp độ:** Senior (Architecture)  
> **Mục tiêu:** Hiểu Micro-Frontend pattern, Module Federation với Webpack 5, khi nào áp dụng  
> **Liên quan BankX:** Tách thành customer-portal + admin-portal + fraud-portal độc lập

---

## 1. Micro-Frontend là Gì?

```
Monolith Frontend (BankX hiện tại):
  1 Angular App lớn chứa tất cả:
  ├── Customer features (accounts, transfers, payments)
  ├── Admin features
  └── Engineering Portal

Micro-Frontend:
  Nhiều Angular Apps nhỏ, độc lập, ghép lại thành 1 UI:
  ┌───────────────────────────────────────────────┐
  │                 Shell App (host)              │
  │  Routing, Layout, Authentication              │
  │                                               │
  │  ┌──────────────┐  ┌──────────────────────┐  │
  │  │ Customer App │  │    Admin Portal       │  │
  │  │ (Remote 1)   │  │    (Remote 2)         │  │
  │  │ accounts/    │  │    users/             │  │
  │  │ transfers/   │  │    settings/          │  │
  │  └──────────────┘  └──────────────────────┘  │
  │                                               │
  │  ┌──────────────────────────────────────────┐ │
  │  │         Engineering Portal (Remote 3)    │ │
  │  │         monitoring/, engineering/        │ │
  │  └──────────────────────────────────────────┘ │
  └───────────────────────────────────────────────┘
```

---

## 2. Lợi Ích & Đánh Đổi

```
✅ Lợi ích:
  - Team A deploy Customer App — không ảnh hưởng Team B (Admin)
  - Mỗi team dùng Angular version khác nhau (nếu cần)
  - Mỗi micro-app có deployment pipeline riêng
  - Lỗi ở 1 app không crash toàn bộ hệ thống
  - Scale team: 5 teams làm 5 micro-apps song song

❌ Đánh đổi:
  - Phức tạp hơn nhiều (build, deploy, routing)
  - Shared state giữa apps khó hơn
  - Bundle size lớn hơn (mỗi app có angular core riêng — trừ khi share)
  - Testing integration khó
  - Khó debug cross-app issues

→ BankX hiện tại: KHÔNG CẦN Micro-Frontend
  (1 team, project học tập, không có multi-team requirements)

→ Khi nào nên dùng:
  - 5+ teams làm việc song song
  - Cần deploy từng phần độc lập
  - Các phần app có SLA khác nhau (Admin có thể down, Customer không được)
```

---

## 3. Module Federation — Kỹ Thuật Nền Tảng

**Webpack 5 Module Federation** cho phép 1 bundle **import code từ bundle khác lúc runtime**.

```
Build Time:
  customer-app/  → bundle1.js
  admin-app/     → bundle2.js
  shell-app/     → shell.js

Runtime (khi user mở app):
  shell.js tải → thấy user vào /admin
  → Fetch bundle2.js từ admin-app server
  → Render Admin component ngay trong Shell
  (Không cần reload trang)
```

---

## 4. Setup Với @angular-architects/module-federation

```bash
# Cài đặt plugin
ng add @angular-architects/module-federation --project shell-app --type host --port 4200
ng add @angular-architects/module-federation --project customer-app --type remote --port 4201
ng add @angular-architects/module-federation --project admin-app --type remote --port 4202
```

---

## 5. Remote App — Customer Portal

```javascript
// customer-app/webpack.config.js
const { shareAll, withModuleFederationPlugin } = require('@angular-architects/module-federation/webpack');

module.exports = withModuleFederationPlugin({

  name: 'customerApp',     // ← Tên unique của remote app

  exposes: {
    // Khai báo những gì muốn share với host:
    './AccountsModule':  './src/app/features/accounts/accounts.routes.ts',
    './TransfersModule': './src/app/features/transfers/transfers.routes.ts',
    './PaymentsModule':  './src/app/features/payments/payments.routes.ts',
  },

  shared: {
    ...shareAll({                // Share Angular packages để tránh duplicate
      singleton: true,           // Chỉ 1 instance của Angular trong toàn bộ app
      strictVersion: true,       // Phải cùng version
      requiredVersion: 'auto',   // Tự detect version
    }),
  },
});
```

---

## 6. Shell App — Host

```javascript
// shell-app/webpack.config.js
const { shareAll, withModuleFederationPlugin } = require('@angular-architects/module-federation/webpack');

module.exports = withModuleFederationPlugin({

  remotes: {
    // Khai báo các remote apps và URL của chúng
    'customerApp': 'http://localhost:4201/remoteEntry.js',
    'adminApp':    'http://localhost:4202/remoteEntry.js',
    'fraudApp':    'http://localhost:4203/remoteEntry.js',
  },

  shared: {
    ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' }),
  },
});
```

### Shell Routing — Load Remote Modules:

```typescript
// shell-app/src/app/app.routes.ts

import { loadRemoteModule } from '@angular-architects/module-federation';

export const routes: Routes = [
  {
    path: '',
    component: ShellLayoutComponent,
    canActivate: [authGuard],
    children: [

      // Load từ Customer App remote
      {
        path: 'accounts',
        loadChildren: () =>
          loadRemoteModule({
            type: 'module',
            remoteEntry: 'http://localhost:4201/remoteEntry.js',
            exposedModule: './AccountsModule'
          }).then(m => m.ACCOUNTS_ROUTES)
      },

      // Load từ Admin App remote
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadChildren: () =>
          loadRemoteModule({
            type: 'module',
            remoteEntry: 'http://localhost:4202/remoteEntry.js',
            exposedModule: './AdminModule'
          }).then(m => m.ADMIN_ROUTES)
      },

      // Load từ Fraud Portal remote
      {
        path: 'fraud',
        loadChildren: () =>
          loadRemoteModule({
            type: 'module',
            remoteEntry: 'http://localhost:4203/remoteEntry.js',
            exposedModule: './FraudModule'
          }).then(m => m.FRAUD_ROUTES)
      }
    ]
  },

  // Fallback nếu remote app không load được
  {
    path: 'error',
    loadComponent: () => import('./pages/error/error.page').then(m => m.ErrorPage)
  }
];
```

---

## 7. Shared Services Giữa Micro-Apps

```typescript
// Problem: AuthService ở Shell cần chia sẻ với Customer App và Admin App

// Giải pháp 1: Shared Library (tốt nhất cho cùng tổ chức)
// Tạo library: bankx-shared-auth → npm publish
// → Cả 3 apps cùng import từ npm package

// Giải pháp 2: Shell expose service
// webpack.config.js của shell:
exposes: {
  './AuthService': './src/app/core/auth/auth.service.ts'
}

// Remote app import:
const { AuthService } = await loadRemoteModule({
  remoteEntry: 'http://localhost:4200/remoteEntry.js',
  exposedModule: './AuthService'
});

// Giải pháp 3: Custom Events / BroadcastChannel
// Cross-app communication qua browser API

// Shell:
const channel = new BroadcastChannel('bankx-auth');
channel.postMessage({ type: 'USER_LOGGED_IN', user: userSummary });

// Customer App:
const channel = new BroadcastChannel('bankx-auth');
channel.onmessage = ({ data }) => {
  if (data.type === 'USER_LOGGED_IN') {
    this.currentUser.set(data.user);
  }
};
```

---

## 8. Error Boundaries — Cô Lập Lỗi

```typescript
// Nếu Admin App crash → Không nên crash Customer App

// Custom Error Handler cho lazy loaded micro-apps:
@Component({
  standalone: true,
  template: `
    @if (error) {
      <div class="micro-app-error">
        <h3>Không thể tải module này</h3>
        <p>{{ error }}</p>
        <button (click)="retry()">Thử lại</button>
      </div>
    } @else {
      <router-outlet />
    }
  `
})
export class MicroAppWrapperComponent implements OnInit {
  error: string | null = null;

  ngOnInit(): void {
    // Bắt lỗi load remote module
    window.addEventListener('error', (e) => {
      if (e.message.includes('Loading chunk')) {
        this.error = 'Không thể kết nối đến service này lúc này';
      }
    });
  }

  retry(): void {
    this.error = null;
    window.location.reload();
  }
}
```

---

## 9. CI/CD Cho Micro-Frontends

```yaml
# .github/workflows/customer-app.yml
# Mỗi remote app có pipeline riêng

name: Deploy Customer App

on:
  push:
    paths:
      - 'apps/customer-app/**'  # Chỉ trigger khi customer-app thay đổi
      # Admin app thay đổi → pipeline này KHÔNG chạy

jobs:
  build-and-deploy:
    steps:
      - name: Build customer-app
        run: nx build customer-app --configuration=production

      - name: Deploy to CDN
        run: aws s3 sync dist/customer-app s3://bankx-customer-app/
        # Deploy tới CDN riêng của customer-app

      - name: Invalidate CloudFront
        run: aws cloudfront create-invalidation --paths "/*"

# Kết quả:
# - Team Customer deploy customer-app → remoteEntry.js mới ở http://cdn-customer/remoteEntry.js
# - Shell app runtime lấy file mới → Customer features tự cập nhật
# - Admin App không biết, không bị ảnh hưởng
```

---

## 10. Micro-Frontend Decision Tree

```
Bạn có:
├── 1 team?
│   └── → KHÔNG cần MFE. Dùng Angular + Lazy Loading (BankX hiện tại ✅)
│
├── 2-4 teams?
│   └── → Xem xét Nx Monorepo trước (Bài 17)
│       → Nếu vẫn có deployment conflicts → Cân nhắc MFE
│
└── 5+ teams, deploy độc lập bắt buộc?
    └── → Module Federation là lựa chọn phù hợp

Ngoài ra hỏi:
├── Các phần app có SLA khác nhau? (Admin có thể down, Customer không?)
│   → Nên dùng MFE
└── Các phần app cần stack khác nhau? (Angular + React + Vue cùng lúc?)
    → Module Federation hỗ trợ mixed frameworks
```

---

## 11. Alternatives Không Dùng Module Federation

```typescript
// Option 1: iFrame (đơn giản nhất, nhưng cô lập hoàn toàn)
<iframe src="http://admin.bankx.vn" style="width:100%; height:100vh; border:none">
</iframe>
// → Admin chạy trong iframe, hoàn toàn cô lập
// → Không share state, URL, history

// Option 2: Web Components (custom elements)
// Admin team build Angular app → compile ra Web Component
// Shell dùng như HTML thẻ:
<bankx-admin-panel auth-token="{{ token }}" />

// Angular hỗ trợ build ra Web Component:
// ng build --configuration=webcomponent
@NgModule({
  declarations: [AdminPanelComponent],
  // entryComponents: [AdminPanelComponent] — deprecated
})
export class AppModule implements DoBootstrap {
  ngDoBootstrap() {
    const ce = createCustomElement(AdminPanelComponent, { injector: this.injector });
    customElements.define('bankx-admin-panel', ce);
  }
}
```

---

## Tổng Kết Bài 20

| Khái Niệm | Mô Tả |
|---|---|
| Micro-Frontend | Nhiều SPA nhỏ ghép thành 1 UI |
| Module Federation | Webpack 5 feature — import code runtime |
| `exposes` | Remote khai báo gì cho host dùng |
| `remotes` | Host khai báo các remote endpoints |
| `loadRemoteModule()` | Load code từ remote app lúc runtime |
| `singleton: true` | Share Angular, không duplicate |
| `BroadcastChannel` | Cross-app communication |
| Shell App | Host chứa routing, auth, layout |
| Remote App | Micro-app expose features của mình |

---

## 🎓 Chúc Mừng! Bạn Đã Hoàn Thành Lộ Trình

```
Cơ Bản (Bài 01-10):   ✅ Angular CLI → TypeScript → Component → Template
                           → SCSS → Services → Routing → Signals → RxJS → Forms

Trung Cấp (Bài 11-14): ✅ Advanced Forms → Animations → NgRx → RxJS nâng cao

Senior (Bài 15-20):    ✅ Performance → CDK → Libraries → SSR → Web Workers → MFE
```

**Bước tiếp theo:**
1. **Contribute** vào codebase BankX — Thêm 1 feature nhỏ
2. **Build** 1 project Angular của riêng bạn từ đầu
3. **Study** Angular source code: https://github.com/angular/angular
4. **Follow** Angular Blog: https://blog.angular.dev

---

**← [Bài 19 — Web Workers](./19_web_workers.md)** | **← [Quay về Danh Sách](./README.md)**
