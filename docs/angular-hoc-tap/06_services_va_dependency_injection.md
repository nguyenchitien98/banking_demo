# Bài 06 — Services & Dependency Injection (DI)

> **Mục tiêu:** Hiểu Service là gì, DI hoạt động thế nào, `inject()` vs Constructor injection  
> **File thực tế:** `account.service.ts`, `auth.service.ts`, `token.service.ts`, `api.interceptor.ts`

---

## 1. Service là Gì? Tại Sao Cần?

**Vấn đề không có Service:**

```typescript
// ❌ SAI — Logic API nằm trực tiếp trong Component
export class AccountsPage {
  accounts: BankAccount[] = [];

  loadAccounts() {
    fetch('/api/v1/accounts')
      .then(r => r.json())
      .then(data => this.accounts = data);
  }
}

export class DashboardPage {
  accounts: BankAccount[] = [];

  loadAccounts() {
    // Phải COPY y chang code này → Duplicated code
    fetch('/api/v1/accounts')
      .then(r => r.json())
      .then(data => this.accounts = data);
  }
}
```

**Giải pháp với Service:**

```typescript
// ✅ ĐÚNG — Logic API tập trung trong Service, Component chỉ dùng lại
@Injectable({ providedIn: 'root' })
export class AccountService {
  getMyAccounts(): Observable<ApiResponse<BankAccount[]>> {
    return this.http.get<ApiResponse<BankAccount[]>>('/api/v1/accounts');
  }
}

// AccountsPage dùng:
export class AccountsPage {
  private readonly accountService = inject(AccountService);
  loadAccounts() { this.accountService.getMyAccounts().subscribe(...); }
}

// DashboardPage cũng dùng CÙNG service:
export class DashboardPage {
  private readonly accountService = inject(AccountService);
  loadBalance() { this.accountService.getMyAccounts().subscribe(...); }
}
```

---

## 2. Anatomy — Giải Phẫu File Service

```typescript
// File: frontend-web/src/app/core/services/account.service.ts

// ===== IMPORT =====
import { Injectable, inject } from '@angular/core';
//       ↑ Decorator       ↑ Hàm inject DI
import { HttpClient } from '@angular/common/http';
//       ↑ Service HTTP của Angular (giống Axios / Fetch nhưng Observable)
import { Observable } from 'rxjs';
//       ↑ Kiểu dữ liệu Observable (sẽ học ở Bài 09)


// ===== INTERFACE ĐỊNH NGHĨA DỮ LIỆU =====
// Định nghĩa "hình dạng" dữ liệu trả về từ API
export interface BankAccount {
  id: string;
  customerId: string;
  accountNumber: string;
  accountName: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | string;
  version: number;
}

export interface CreateAccountRequest {
  accountName: string;
  initialBalance?: number; // ? = optional field
}


// ===== DECORATOR @Injectable — Đánh Dấu Class Là Service =====
@Injectable({
  providedIn: 'root',
  // ↑ 'root' = Service là Singleton toàn app
  //   Chỉ tạo 1 instance duy nhất, chia sẻ cho tất cả components
  //   Angular tự động đưa service vào DI container khi app khởi động
  //
  // Thay thế: providedIn: SomeModule → Scoped to that module (ít dùng)
})
export class AccountService {

  // ===== INJECT DEPENDENCIES =====
  private readonly API_URL = '/api/v1/accounts';
  //              ↑ const cho URL — readonly = không bao giờ thay đổi

  private readonly http = inject(HttpClient);
  // inject(HttpClient):
  // → Angular tìm HttpClient trong DI container
  // → Trả về instance HttpClient đã được tạo sẵn
  // → Gán vào this.http
  // Giống @Autowired HttpClient trong Spring Boot


  // ===== METHODS =====

  /**
   * Lấy danh sách tài khoản của user hiện tại
   */
  getMyAccounts(): Observable<ApiResponse<BankAccount[]>> {
    return this.http.get<ApiResponse<BankAccount[]>>(this.API_URL);
    // http.get<T>(url): Gửi GET request, trả về Observable<T>
    // <ApiResponse<BankAccount[]>>: Generic type hint cho TypeScript
  }

  createAccount(request: CreateAccountRequest): Observable<ApiResponse<BankAccount>> {
    return this.http.post<ApiResponse<BankAccount>>(this.API_URL, request);
    // http.post<T>(url, body): Gửi POST request với body
  }

  freezeAccount(accountId: string): Observable<ApiResponse<BankAccount>> {
    return this.http.patch<ApiResponse<BankAccount>>(
      `${this.API_URL}/${accountId}/freeze`, // URL dynamic với template literal
      {}  // Body rỗng
    );
  }
}
```

---

## 3. `@Injectable({ providedIn: 'root' })` — Singleton Pattern

```typescript
// Khi providedIn: 'root':
// - Angular tạo 1 instance duy nhất của AccountService khi app khởi động
// - Mọi Component dùng inject(AccountService) đều nhận CÙNG instance
// - Giống Singleton Bean @Service trong Spring Boot

@Injectable({ providedIn: 'root' })
export class AuthService {
  public currentUser = signal<UserSummary | null>(null);
  // ↑ Signal này được CHIA SẺ giữa tất cả component
  // MainLayout đọc currentUser() → hiện tên user trên header
  // AccountsPage đọc currentUser() → kiểm tra quyền
  // TẤT CẢ đều nhìn thấy CÙNG giá trị

  public isAuthenticated = signal<boolean>(false);
}

// Component A:
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  readonly currentUser = this.authService.currentUser; // CÙNG signal
}

// Component B:
export class AccountsPage {
  private readonly authService = inject(AuthService);
  // authService ở đây CÙNG instance với MainLayoutComponent
  // Khi login, authService.currentUser.set(user) → CẢ HAI component tự cập nhật
}
```

---

## 4. `inject()` — Cách Inject Dependency Angular 22

```typescript
// ===== CÁCH MỚI (Angular 14+, BankX dùng) =====
export class AccountsPage {
  private readonly accountService = inject(AccountService);
  private readonly ledgerService  = inject(LedgerService);
  private readonly router         = inject(Router);
}

// ===== CÁCH CŨ (Constructor Injection — vẫn hoạt động) =====
export class AccountsPageOld {
  constructor(
    private readonly accountService: AccountService,
    private readonly ledgerService: LedgerService,
    private readonly router: Router,
  ) {}
}

// ===== SO SÁNH =====
// inject() mới:
//   ✅ Ngắn gọn hơn
//   ✅ Không cần constructor
//   ✅ Dùng được ở Guard, Interceptor (functional)
//
// Constructor injection cũ:
//   ✅ Quen thuộc hơn với developer Java (Spring @Autowired tương tự)
//   ✅ Dễ mock trong unit tests
```

---

## 5. HTTP Interceptor — Middleware Cho HTTP Requests

**Interceptor** là "middleware" nằm giữa HTTP request và response, giống `Filter` trong Spring Boot.

```typescript
// File: frontend-web/src/app/core/http/api.interceptor.ts

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../auth/token.service';

// Interceptor là 1 function (không phải class — đây là functional interceptor Angular 22)
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // req  = HTTP Request đang được gửi
  // next = "bước tiếp theo" trong chain (giống FilterChain trong Spring)

  const tokenService = inject(TokenService);
  const token = tokenService.getAccessToken(); // Lấy JWT từ localStorage

  let headers = req.headers; // headers hiện tại của request

  if (token) {
    // Thêm header Authorization: Bearer {jwt_token}
    // Mọi request gửi đi đều tự động có JWT → Backend verify
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  // Clone request (bất biến — không thể sửa req trực tiếp)
  const clonedReq = req.clone({ headers });

  // Chuyển request đã clone sang bước tiếp theo
  return next(clonedReq);
  // return next(req) → Tiếp tục chain (như FilterChain.doFilter() trong Java)
};
```

**Đăng ký Interceptor trong `app.config.ts`:**

```typescript
// app.config.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { apiInterceptor } from './core/http/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([
        apiInterceptor,    // Interceptor 1: Gắn JWT token
        errorInterceptor,  // Interceptor 2: Xử lý lỗi 401/500
        loadingInterceptor // Interceptor 3: Bật/tắt loading spinner
      ])
    )
  ]
};
// Thứ tự interceptors: apiInterceptor → errorInterceptor → loadingInterceptor → API
```

---

## 6. Token Service — Pattern Lưu Token an Toàn

```typescript
// File: token.service.ts

@Injectable({ providedIn: 'root' })
export class TokenService {

  private readonly ACCESS_TOKEN_KEY  = 'bankx_access_token';
  private readonly REFRESH_TOKEN_KEY = 'bankx_refresh_token';
  private readonly USER_INFO_KEY     = 'bankx_user_info';

  // Lưu token vào localStorage (persist qua refresh)
  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  // Lấy token
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  // Kiểm tra còn token không
  hasToken(): boolean {
    return !!this.getAccessToken();
    // !! = double negation → convert to boolean
    // null → !null = true → !!null = false (không có token)
    // "jwt..." → !"jwt..." = false → !!"jwt..." = true (có token)
  }

  // Lưu user info (serialize sang JSON)
  setUserInfo(user: UserSummary): void {
    localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(user));
  }

  // Lấy user info (deserialize từ JSON)
  getUserInfo(): UserSummary | null {
    const raw = localStorage.getItem(this.USER_INFO_KEY);
    return raw ? JSON.parse(raw) : null;
    // JSON.parse chuyển chuỗi JSON → JavaScript object
  }

  // Xóa tất cả tokens (khi logout)
  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_INFO_KEY);
  }
}
```

---

## 7. AuthService — Service Quản Lý State Auth

```typescript
// File: auth.service.ts

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API_URL     = '/api/v1/auth';
  private readonly http        = inject(HttpClient);
  private readonly tokenService = inject(TokenService);

  // ===== SIGNALS (State) =====
  // Khởi tạo từ localStorage → Khi F5 refresh trang, state không mất
  public currentUser     = signal<UserSummary | null>(this.tokenService.getUserInfo());
  public isAuthenticated = signal<boolean>(this.tokenService.hasToken());

  // ===== LOGIN =====
  login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap((res) => {
          // tap() = "nhìn vào" Observable mà không thay đổi nó
          // Dùng để side-effects: lưu token, cập nhật state
          if (res.code === 0 && res.data) {
            this.tokenService.setAccessToken(res.data.accessToken);
            this.tokenService.setRefreshToken(res.data.refreshToken);
            this.tokenService.setUserInfo(res.data.user);

            // Cập nhật Signals → Mọi component đang đọc signal tự động re-render
            this.currentUser.set(res.data.user);
            this.isAuthenticated.set(true);
          }
        })
      );
  }

  // ===== LOGOUT =====
  logout(): void {
    this.http.post(`${this.API_URL}/logout`, {}).subscribe({
      complete: () => this.clearSession(), // Dù thành công hay hết hạn
      error:    () => this.clearSession(), // Dù lỗi → vẫn xóa session local
    });
  }

  private clearSession(): void {
    this.tokenService.clearTokens();
    this.currentUser.set(null);       // Xóa user khỏi signal
    this.isAuthenticated.set(false);  // Đánh dấu chưa đăng nhập
  }
}
```

---

## 8. Service vs Component — Khi Nào Dùng Cái Nào?

| Tiêu Chí | Component | Service |
|---|---|---|
| **Chứa** | UI logic, event handling | Business logic, API calls |
| **Lifecycle** | Tạo/xóa theo route | Singleton (sống suốt app) |
| **Re-use** | Khó (phải copy HTML) | Dễ (inject vào bất kỳ đâu) |
| **Test** | Unit test UI | Unit test business logic |
| **Ví dụ BankX** | `AccountsPage`, `MainLayout` | `AccountService`, `AuthService` |

**Nguyên tắc vàng:**

```
Component = HIỂN THỊ dữ liệu + NHẬN event user
Service   = LẤY dữ liệu (API) + XỬ LÝ business logic
```

---

## 9. Tạo Service Mới — Workflow

```bash
# Tạo service mới bằng CLI
ng generate service core/services/notification
# Tạo: notification.service.ts + notification.service.spec.ts (test)
```

```typescript
// Template service mới
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service'; // Import interface chung

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly API_URL = '/api/v1/notifications';
  private readonly http = inject(HttpClient);

  getNotifications(): Observable<ApiResponse<Notification[]>> {
    return this.http.get<ApiResponse<Notification[]>>(this.API_URL);
  }

  markAsRead(id: string): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.API_URL}/${id}/read`, {}
    );
  }
}
```

---

## Tổng Kết

| Khái Niệm | Ý Nghĩa | Ví Dụ BankX |
|---|---|---|
| `@Injectable` | Đánh dấu class là Service có thể inject | `AccountService`, `AuthService` |
| `providedIn: 'root'` | Singleton toàn app | Tất cả service BankX |
| `inject(ServiceClass)` | Lấy instance service từ DI container | `inject(AccountService)` |
| `HttpClient` | Gửi HTTP requests | `http.get()`, `http.post()` |
| `Observable<T>` | Kết quả async của HTTP request | Return type của mọi method service |
| `HttpInterceptorFn` | Middleware xử lý request/response | `apiInterceptor` thêm JWT |
| `localStorage` | Lưu token persist qua refresh | `TokenService` |
| Signal trong Service | State chia sẻ toàn app | `currentUser`, `isAuthenticated` |

---

**← [Bài 05 — SCSS & CSS](./05_scss_va_css_layout.md)** | **→ [Bài 07 — Routing & Route Guards](./07_routing_va_route_guards.md)**
