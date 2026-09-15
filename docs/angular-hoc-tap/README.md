# 📚 Titan BankX — Khóa Học Angular 22 Thực Hành

> Học Angular từ đầu thông qua code thực tế của dự án **Titan BankX Digital Banking Platform**  
> Mọi ví dụ đều lấy trực tiếp từ file trong `frontend-web/`

---

## 🟢 Lộ Trình Cơ Bản (Bài 01-10)

| # | Bài Học | Nội Dung | File BankX Liên Quan |
|---|---|---|---|
| **01** | [Angular CLI & Cấu Trúc Dự Án](./01_angular_cli_va_cau_truc.md) | `ng new`, `ng serve`, `ng build`, `ng generate`, Standalone Components | `app.config.ts`, `angular.json` |
| **02** | [TypeScript & Interface](./02_typescript_va_interface.md) | Kiểu dữ liệu, interface, generic, optional chaining, arrow function | `account.service.ts`, `auth.service.ts` |
| **03** | [Component & Lifecycle Hooks](./03_component_va_lifecycle.md) | `@Component`, `ngOnInit`, `ngOnDestroy`, 3 file `.ts/.html/.scss` | `accounts.page.ts`, `main-layout.component.ts` |
| **04** | [Template HTML & Directives](./04_template_html_va_directives.md) | `{{ }}`, `[binding]`, `(event)`, `[(ngModel)]`, `@if`, `@for`, `@switch`, Pipes | `accounts.page.html` |
| **05** | [SCSS & CSS Flexbox/Grid](./05_scss_va_css_layout.md) | `display: flex`, `grid-template-columns`, SCSS nesting, animations, responsive | `accounts.page.scss`, `styles.scss` |
| **06** | [Services & Dependency Injection](./06_services_va_dependency_injection.md) | `@Injectable`, `inject()`, `HttpClient`, HTTP Interceptor, Singleton pattern | `account.service.ts`, `api.interceptor.ts` |
| **07** | [Routing & Route Guards](./07_routing_va_route_guards.md) | `Routes`, Lazy Loading, `canActivate`, `routerLink`, Nested Routes | `app.routes.ts`, `auth.guard.ts` |
| **08** | [Signals & State Management](./08_signals_va_state_management.md) | `signal()`, `computed()`, `effect()`, State chia sẻ qua Service | `auth.service.ts`, `accounts.page.ts` |
| **09** | [RxJS & HTTP Client](./09_rxjs_va_http_client.md) | `Observable`, `subscribe()`, `pipe()`, operators: `tap/map/catchError/forkJoin` | `accounts.page.ts`, `error.interceptor.ts` |
| **10** | [Forms, @Input/@Output & Communication](./10_forms_input_output_communication.md) | Template-driven & Reactive Forms, `@Input`, `@Output`, `EventEmitter` | `accounts.page.html`, `accounts.page.ts` |

---

## 🟡 Lộ Trình Cấp Trung (Bài 11-14)

| # | Bài Học | Nội Dung |
|---|---|---|
| **11** | [Forms Nâng Cao — Custom & Async Validators](./11_forms_nang_cao_validators.md) | `ValidatorFn`, `AsyncValidatorFn`, Cross-field validation, `FormArray` dynamic |
| **12** | [Angular Animations](./12_angular_animations.md) | CSS `@keyframes`, `:enter/:leave`, `stagger()`, Route transitions, Micro-interactions |
| **13** | [NgRx State Management](./13_ngrx_state_management.md) | Store, Actions, Reducers (EntityAdapter), Selectors (memoized), Effects |
| **14** | [RxJS Nâng Cao](./14_rxjs_nang_cao.md) | `Subject/BehaviorSubject`, `switchMap/mergeMap/exhaustMap`, `shareReplay`, WebSocket |

---

## 🔴 Lộ Trình Cấp Senior (Bài 15-20)

| # | Bài Học | Nội Dung |
|---|---|---|
| **15** | [Performance Optimization](./15_performance_optimization.md) | `OnPush` ChangeDetection, `track`, Virtual Scroll, `@defer`, Bundle analysis |
| **16** | [Angular CDK](./16_angular_cdk.md) | Overlay, Drag & Drop, Clipboard, FocusTrap (A11y), Stepper, Platform |
| **17** | [Custom Libraries & Nx Monorepo](./17_custom_libraries_monorepo.md) | Angular library, Design System, Nx workspace, Storybook |
| **18** | [SSR & Angular Universal](./18_ssr_angular_universal.md) | CSR vs SSR vs Pre-rendering, `PLATFORM_ID`, `TransferState`, Hydration |
| **19** | [Web Workers](./19_web_workers.md) | Off-main-thread computation, `postMessage`, Comlink, Fraud Analysis example |
| **20** | [Micro-Frontends (Module Federation)](./20_micro_frontends.md) | Shell/Remote pattern, Webpack 5 Module Federation, Cross-app communication |

---

## 🎯 Lộ Trình Học Đề Xuất

### Tuần 1 — Nền Tảng (Bài 01-03)
```
Bài 01 → Bài 02 → Bài 03
Hiểu: Angular CLI, TypeScript cơ bản, Component là gì
Thực hành: Mở accounts.page.ts, đọc và giải thích từng dòng
```

### Tuần 2 — Template & Styles (Bài 04-05)
```
Bài 04 → Bài 05
Hiểu: Cú pháp HTML template, CSS Flexbox/Grid
Thực hành: Mở accounts.page.html + accounts.page.scss, giải thích layout
```

### Tuần 3 — Services & Architecture (Bài 06-07)
```
Bài 06 → Bài 07
Hiểu: DI, Services, Routing
Thực hành: Trace 1 request từ button click → Service → API → Render
```

### Tuần 4 — State & Async (Bài 08-10)
```
Bài 08 → Bài 09 → Bài 10
Hiểu: Signals, RxJS, Forms, Component Communication
Thực hành: Implement 1 feature nhỏ (ví dụ: thêm search filter tài khoản)
```

---

## 🔍 Tìm Ví Dụ Theo Keyword

| Bạn muốn tìm | Bài | File |
|---|---|---|
| `@for` loop | Bài 04 | `accounts.page.html` |
| `@if` điều kiện | Bài 04 | `accounts.page.html` |
| `[(ngModel)]` | Bài 04, 10 | `accounts.page.html` |
| `signal()` | Bài 08 | `accounts.page.ts`, `auth.service.ts` |
| `computed()` | Bài 08 | - |
| `inject()` | Bài 06 | `accounts.page.ts` |
| `subscribe()` | Bài 09 | `accounts.page.ts` |
| `forkJoin` | Bài 09 | `accounts.page.ts` |
| `display: flex` | Bài 05 | `accounts.page.scss` |
| `display: grid` | Bài 05 | `accounts.page.scss` |
| `gap` | Bài 05 | `accounts.page.scss` |
| `routerLink` | Bài 07 | `main-layout.component.html` |
| `canActivate` | Bài 07 | `app.routes.ts` |
| `@Input()` | Bài 10 | - |
| `@Output()` | Bài 10 | - |
| `EventEmitter` | Bài 10 | - |
| `HttpClient` | Bài 06, 09 | `account.service.ts` |
| `HttpInterceptorFn` | Bài 06 | `api.interceptor.ts` |
| `tap()` | Bài 09 | `auth.service.ts` |
| `catchError()` | Bài 09 | `error.interceptor.ts` |
| `ngOnInit()` | Bài 03 | `accounts.page.ts` |
| `ngOnDestroy()` | Bài 03 | Engineering portal page |
| `private readonly` | Bài 02, 03 | `accounts.page.ts` |
| TypeScript `interface` | Bài 02 | `account.service.ts` |
| CSS Variables `--var` | Bài 05 | `styles.scss` |
| SCSS nesting `&:hover` | Bài 05 | `accounts.page.scss` |
| `Validators.required` | Bài 10 | Login page |

---

## 📁 File Quan Trọng Trong BankX Frontend

```
frontend-web/src/app/
│
├── ⭐ app.routes.ts                    ← Routing tổng thể (Bài 07)
├── ⭐ app.config.ts                    ← App setup, Interceptors (Bài 06)
│
├── core/
│   ├── auth/
│   │   ├── ⭐ auth.service.ts          ← Signal state, Login/Logout (Bài 06, 08)
│   │   ├── ⭐ auth.guard.ts            ← Route protection (Bài 07)
│   │   └── token.service.ts           ← JWT localStorage (Bài 06)
│   ├── http/
│   │   ├── ⭐ api.interceptor.ts       ← JWT header (Bài 06)
│   │   └── error.interceptor.ts       ← Error handling (Bài 09)
│   └── services/
│       └── ⭐ account.service.ts       ← HTTP API calls (Bài 06, 09)
│
├── features/
│   ├── accounts/
│   │   └── pages/accounts/
│   │       ├── ⭐ accounts.page.ts     ← Component đầy đủ nhất (Bài 03-10)
│   │       ├── ⭐ accounts.page.html   ← Template với @for, @if (Bài 04)
│   │       └── ⭐ accounts.page.scss   ← CSS Flexbox + Grid (Bài 05)
│   └── auth/
│       └── pages/login/
│           └── login.page.ts          ← Reactive Forms (Bài 10)
│
└── layouts/
    └── main-layout/
        ├── ⭐ main-layout.component.ts   ← Signal, Router (Bài 07, 08)
        └── ⭐ main-layout.component.html ← routerLink, routerLinkActive (Bài 07)
```

---

## 💡 Mẹo Học Hiệu Quả

1. **Mở file gốc song song với bài học** — Đừng chỉ đọc bài, hãy mở file thực tế và đọc cùng lúc.

2. **Thực hành "Explain the Code"** — Chọn 1 method bất kỳ trong project, giải thích từng dòng bằng tiếng Việt.

3. **Sửa nhỏ rồi `ng serve`** — Thay đổi màu sắc, text, thêm một `@if` → Thấy kết quả ngay.

4. **Học theo chiều dọc** — Trace 1 chức năng từ đầu đến cuối:
   ```
   Button "Tải lại" (HTML) 
     → (click)="loadAccounts()" (HTML Event)
     → loadAccounts() method (TS)
     → accountService.getMyAccounts() (Service)
     → http.get('/api/v1/accounts') (HttpClient)
     → apiInterceptor thêm JWT (Interceptor)
     → API call đến Backend
     → .subscribe() nhận response
     → accounts.set(res.data) (Signal update)
     → HTML tự re-render danh sách tài khoản
   ```

5. **Đọc lỗi TypeScript** — TypeScript sẽ báo lỗi rõ ràng khi bạn làm sai. Đây là teacher tốt nhất.

---

*Khóa học xây dựng dựa trên code thực tế của Titan BankX Platform (27 Sprints).*
