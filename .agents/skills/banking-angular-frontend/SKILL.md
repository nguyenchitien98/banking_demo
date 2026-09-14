---
name: banking-angular-frontend
description: >
  Skill dành cho việc implement Angular frontend cho BankX Digital Banking Platform.
  Trigger khi: viết Angular components, services, pipes, guards, interceptors,
  implement transfer flow, account management, OTP screen, QR payment,
  dùng Angular Signals, NgRx, Ionic/Capacitor mobile.
---

# Skill: BankX Angular Banking Frontend

## Khi Nào Trigger
- Viết Angular component, service, pipe, guard, interceptor
- Implement các màn hình banking (Login, OTP, Dashboard, Transfer, Payment, Cards...)
- Ionic/Capacitor mobile development
- NgRx state management
- Angular Signals cho local state
- OTP input, money display, account card components

## Bắt Buộc Đọc Trước Khi Code
1. `banking/docs/07_UI_UX_Standard.md` — Design tokens TPBank, component specs
2. `banking/docs/02_Coding_Guideline.md` — Angular folder structure, naming conventions
3. `banking/docs/01_Architecture_Bible.md` — Banking flows (transfer, OTP...)

## Folder Structure

```
banking/frontend-web/src/app/
├── core/
│   ├── auth/
│   │   ├── auth.service.ts          # Login, logout, token management
│   │   ├── auth.guard.ts            # Route protection
│   │   ├── role.guard.ts            # RBAC guard
│   │   └── token.service.ts         # JWT parse, localStorage
│   ├── http/
│   │   ├── api.interceptor.ts       # Add JWT to every request
│   │   ├── error.interceptor.ts     # Handle 401/403/500
│   │   └── loading.interceptor.ts
│   └── models/
│       └── user.model.ts
│
├── shared/
│   ├── components/
│   │   ├── money-display/           # <bankx-money-display [amount]="145000000" />
│   │   ├── account-card/            # Card tài khoản gradient tím
│   │   ├── transaction-item/        # Item giao dịch (xanh/đỏ)
│   │   ├── otp-input/               # 6-digit OTP input
│   │   ├── loading-spinner/
│   │   └── empty-state/
│   ├── pipes/
│   │   ├── currency-vnd.pipe.ts     # 145000000 → "145.000.000 ₫"
│   │   ├── mask-account.pipe.ts     # 1012345678 → "**** 5678"
│   │   └── relative-date.pipe.ts    # "2 giờ trước"
│   └── validators/
│       ├── account-number.validator.ts
│       └── transfer-amount.validator.ts
│
├── features/
│   ├── auth/
│   │   ├── pages/
│   │   │   ├── login/
│   │   │   └── otp-verify/
│   │   └── auth.routes.ts
│   ├── dashboard/
│   │   ├── pages/dashboard/
│   │   └── dashboard.routes.ts
│   ├── accounts/
│   │   ├── pages/
│   │   │   ├── account-list/
│   │   │   ├── account-detail/
│   │   │   └── account-statement/
│   │   ├── services/
│   │   │   └── account-api.service.ts
│   │   └── accounts.routes.ts
│   └── transfers/
│       ├── pages/
│       │   ├── transfer-form/       # Screen 06
│       │   ├── transfer-confirm/    # Screen 07
│       │   ├── transfer-otp/        # Screen 08
│       │   └── transfer-result/     # Screen 09
│       ├── services/
│       │   └── transfer-api.service.ts
│       ├── models/
│       │   └── transfer.model.ts
│       └── transfers.routes.ts
│
└── layouts/
    ├── main-layout/                 # Bottom nav layout (mobile)
    ├── auth-layout/                 # Login layout (no nav)
    └── admin-layout/                # Sidebar layout
```

## Component Templates

### Standalone Component Template
```typescript
/**
 * Component [Tên Component] — [Mô tả mục đích]
 *
 * Sử dụng Angular Signals để quản lý local state thay vì Subject/BehaviorSubject
 * vì Signals có fine-grained reactivity và dễ đọc hơn Observable streams.
 */
import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CurrencyVndPipe } from '@shared/pipes/currency-vnd.pipe';

@Component({
  selector: 'bankx-[component-name]',
  standalone: true,
  imports: [CommonModule, RouterModule, CurrencyVndPipe],
  templateUrl: './[component-name].component.html',
  styleUrls: ['./[component-name].component.scss']
})
export class [ComponentName]Component implements OnInit {
  
  // === DI ===
  private readonly [service] = inject([ServiceClass]);

  // === LOCAL STATE (Angular Signals) ===
  // Dùng signal thay vì ngOnInit + subscribe pattern cũ
  protected readonly items = signal<[Type][]>([]);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  // === COMPUTED STATE ===
  protected readonly hasItems = computed(() => this.items().length > 0);

  ngOnInit(): void {
    this.loadData();
  }

  protected loadData(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.[service].get[Data]().subscribe({
      next: (data) => {
        this.items.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Không thể tải dữ liệu. Vui lòng thử lại.');
        this.isLoading.set(false);
      }
    });
  }
}
```

### API Service Template
```typescript
/**
 * Service gọi API [Module] từ BankX Backend.
 *
 * Tất cả request đều được Angular HttpClient xử lý,
 * JWT được thêm tự động bởi api.interceptor.ts.
 * Lỗi HTTP được handle tập trung bởi error.interceptor.ts.
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import { ApiResponse } from '@core/models/api-response.model';
import { [Request], [Response] } from './models/[model].model';

@Injectable({ providedIn: 'root' })
export class [Module]ApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/[endpoint]`;

  /**
   * [Mô tả method]
   *
   * @param request Thông tin [feature]
   * @param idempotencyKey UUID chống duplicate (client sinh trước khi call)
   * @returns Observable<ApiResponse<[Response]>>
   */
  create(request: [Request], idempotencyKey: string): Observable<ApiResponse<[Response]>> {
    return this.http.post<ApiResponse<[Response]>>(
      this.baseUrl,
      request,
      {
        headers: {
          // Idempotency-Key được sinh từ frontend để chống duplicate requests
          'Idempotency-Key': idempotencyKey
        }
      }
    );
  }
}
```

### Transfer Flow — Idempotency Key Generation
```typescript
// Luôn sinh idempotency key TRƯỚC khi user click submit
// Không sinh trong click handler (tránh sinh key mới mỗi lần retry UI)
export class TransferFormPageComponent {
  
  // Sinh UUID khi component khởi tạo
  // Khi user bấm "Tiếp tục" nhiều lần → cùng key → backend idempotent
  private readonly idempotencyKey = crypto.randomUUID();

  onContinue(): void {
    // Truyền idempotencyKey xuống confirm screen qua Router state
    this.router.navigate(['/transfers/confirm'], {
      state: {
        formData: this.transferForm.value,
        idempotencyKey: this.idempotencyKey
      }
    });
  }
}
```

### OTP Component Template
```typescript
/**
 * Component nhập OTP 6 chữ số cho xác thực banking.
 *
 * Auto-submit khi đủ 6 chữ số để UX nhanh nhất.
 * Countdown timer 120 giây, nút resend sau 60 giây.
 */
@Component({
  selector: 'bankx-otp-input',
  standalone: true,
  template: `
    <div class="otp-container">
      <div class="otp-input-group">
        @for (i of otpDigits; track i; let idx = $index) {
          <input
            type="tel"
            maxlength="1"
            class="otp-digit"
            [class.filled]="digits()[idx]"
            [class.error]="hasError()"
            (input)="onDigitInput($event, idx)"
            (keydown)="onKeyDown($event, idx)"
            #digitInput
          />
        }
      </div>
      
      <div class="countdown">
        @if (remainingSeconds() > 0) {
          <span>OTP hết hạn sau: {{ formatTime(remainingSeconds()) }}</span>
        } @else {
          <span class="expired">OTP đã hết hạn</span>
        }
      </div>
      
      <button 
        class="resend-btn"
        [disabled]="canResend()"
        (click)="onResendOtp()">
        Gửi lại OTP {{ canResend() ? '' : '(' + remainingSeconds() + 's)' }}
      </button>
    </div>
  `
})
export class OtpInputComponent {
  // ... signal-based implementation
}
```

## TPBank UI Standards Tóm Tắt

```scss
// Luôn import banking-theme.scss
// Màu primary: var(--bank-primary) = #7B2D8B
// Gradient button: var(--bank-gradient-button)
// Màu nhận tiền: var(--color-money-in) = #00875A (xanh)
// Màu gửi tiền: var(--color-money-out) = #DE350B (đỏ)
// Font: var(--font-family) = 'Be Vietnam Pro', 'Inter'
// Border radius button: var(--border-radius-xl) = 24px
// Shadow button: var(--shadow-button) = 0 4px 14px rgba(123, 45, 139, 0.35)
```

## Routing (Lazy Loading)

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'transfers',
        loadChildren: () => import('./features/transfers/transfers.routes').then(m => m.TRANSFER_ROUTES)
      }
    ]
  }
];
```

## Banking UI Checklist

```
Design:
  [ ] Dùng design tokens từ banking-theme.scss (không hard-code màu)
  [ ] Button primary: gradient tím, shadow, hover effect
  [ ] Số tiền nhận: màu xanh; Số tiền gửi: màu đỏ
  [ ] Số tài khoản luôn masked (**** 5678)
  [ ] Số tiền format: "145.000.000 ₫" (dùng CurrencyVndPipe)
  [ ] Loading skeleton khi đang fetch data
  [ ] Empty state khi không có data

Performance:
  [ ] Lazy load feature modules
  [ ] Signals cho local state (không Subject/BehaviorSubject cho local state)
  [ ] OnPush change detection strategy
  [ ] trackBy trong @for loops

Security:
  [ ] Không lưu sensitive data trong localStorage (chỉ access token)
  [ ] Idempotency key được sinh từ frontend trước khi submit
  [ ] Route guard kiểm tra authentication
  [ ] Ownership check: user không xem được account của người khác

Mobile:
  [ ] Safe area insets (iPhone notch)
  [ ] Touch targets đủ lớn (min 44px)
  [ ] Keyboard avoidance khi input focus
  [ ] Bottom navigation fixed với safe-area-inset-bottom
```
