# Bài 10 — Forms, @Input/@Output & Component Communication

> **Mục tiêu:** Thành thạo Forms, giao tiếp Parent↔Child component, Reactive Forms  
> **File thực tế:** `accounts.page.html`, `accounts.page.ts`, `login.page.ts`

---

## 1. Hai Loại Forms Trong Angular

| | Template-Driven Forms | Reactive Forms |
|---|---|---|
| **Cách tiếp cận** | Định nghĩa trong HTML | Định nghĩa trong TypeScript |
| **Module cần** | `FormsModule` | `ReactiveFormsModule` |
| **Two-way binding** | `[(ngModel)]` | `formControl.value` |
| **Validation** | HTML attributes | `.ts` programmatic |
| **Testing** | Khó | Dễ |
| **BankX dùng** | ✅ (đơn giản hơn) | ✅ (login form) |

---

## 2. Template-Driven Forms — BankX Modal Ledger

```typescript
// accounts.page.ts — Khai báo state cho form
export class AccountsPage {
  // Form state — các biến này "bind" với HTML bằng [(ngModel)]
  public debitAccountId = '';
  public creditAccountId = '';
  public transferAmount = 1000000;
  public transferDescription = 'Chuyển tiền ghi sổ kép thử nghiệm';
  public submittingLedger = false;
  public modalError: string | null = null;

  submitDoubleEntry(): void {
    // ===== VALIDATION thủ công =====

    // Kiểm tra field bắt buộc
    if (!this.debitAccountId || !this.creditAccountId) {
      this.modalError = 'Vui lòng chọn cả tài khoản trích Nợ và Thụ hưởng!';
      return; // Dừng lại, không submit
    }

    // Kiểm tra business rule
    if (this.debitAccountId === this.creditAccountId) {
      this.modalError = 'Tài khoản trích Nợ và Thụ hưởng không được giống nhau!';
      return;
    }

    // Kiểm tra số tiền tối thiểu
    if (this.transferAmount < 1000) {
      this.modalError = 'Số tiền tối thiểu là 1,000 VND!';
      return;
    }

    // Tất cả hợp lệ → Submit
    this.submittingLedger = true;
    this.modalError = null;

    this.ledgerService.recordDoubleEntry({
      debitAccountId: this.debitAccountId,
      creditAccountId: this.creditAccountId,
      amount: this.transferAmount,
      description: this.transferDescription
    }).subscribe({
      next: (res) => {
        this.submittingLedger = false;
        this.showDoubleEntryModal = false;
        this.showToast(`Thành công! Mã GD: ${res.data.transactionReference}`);
      },
      error: (err) => {
        this.submittingLedger = false;
        this.modalError = err?.error?.message || 'Hạch toán thất bại!';
      }
    });
  }
}
```

```html
<!-- accounts.page.html — Modal form -->
@if (showDoubleEntryModal) {
  <div class="modal-overlay">
    <div class="modal-box">
      <h3>Hạch toán Bút toán Ghi sổ kép</h3>

      <!-- Select Tài khoản Nợ — Two-way binding với [(ngModel)] -->
      <label>Tài khoản Trích Nợ:</label>
      <select [(ngModel)]="debitAccountId">
        <!--   ↑ Khi user chọn → debitAccountId trong .ts cập nhật ngay -->
        @for (acc of accounts(); track acc.id) {
          <option [value]="acc.id">
            {{ acc.accountName }} — {{ acc.balance | number:'1.0-0' }} VND
          </option>
        }
      </select>

      <!-- Input Số tiền -->
      <label>Số tiền (VND):</label>
      <input type="number"
             [(ngModel)]="transferAmount"
             placeholder="Nhập số tiền"
             min="1000">
      <!--   ↑ Thay đổi input → transferAmount tự cập nhật trong .ts -->

      <!-- Input Nội dung -->
      <label>Nội dung:</label>
      <input type="text"
             [(ngModel)]="transferDescription"
             placeholder="Nội dung chuyển tiền">

      <!-- Hiển thị lỗi -->
      @if (modalError) {
        <div class="error-msg">⚠️ {{ modalError }}</div>
      }

      <!-- Nút Submit -->
      <button (click)="submitDoubleEntry()"
              [disabled]="submittingLedger">
        {{ submittingLedger ? 'Đang xử lý...' : 'Xác nhận Hạch toán' }}
        <!--  ↑ Ternary operator: Nếu đang submit → hiện text khác -->
      </button>

      <button (click)="closeDoubleEntryModal()">Hủy</button>
    </div>
  </div>
}
```

---

## 3. Reactive Forms — Mạnh Mẽ Hơn

```typescript
// Login form theo Reactive Forms pattern
import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule], // ← Import ReactiveFormsModule
  template: `...`
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);

  // Định nghĩa form trong TypeScript
  loginForm: FormGroup = this.fb.group({
    username: [
      '',                                    // Giá trị khởi đầu
      [Validators.required,                  // Bắt buộc nhập
       Validators.minLength(3)]              // Tối thiểu 3 ký tự
    ],
    password: [
      '',
      [Validators.required,
       Validators.minLength(6)]
    ]
  });

  onSubmit(): void {
    // Kiểm tra toàn bộ form hợp lệ chưa
    if (this.loginForm.invalid) {
      // Mark tất cả là touched → hiện lỗi validation
      this.loginForm.markAllAsTouched();
      return;
    }

    // Lấy giá trị từ form
    const { username, password } = this.loginForm.value;
    this.authService.login({ username, password }).subscribe(/* ... */);
  }

  // Getter shortcut để truy cập từng field
  get username() { return this.loginForm.get('username')!; }
  get password() { return this.loginForm.get('password')!; }
}
```

```html
<!-- Login Form HTML với Reactive Forms -->
<form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
<!--   ↑ Bind form group     ↑ Bắt event submit -->

  <input formControlName="username"
<!--       ↑ Bind vào FormControl 'username' trong FormGroup -->
         type="text"
         placeholder="Tên đăng nhập">

  <!-- Hiển thị lỗi validation -->
  @if (username.invalid && username.touched) {
    @if (username.errors?.['required']) {
      <span class="error">Tên đăng nhập không được để trống</span>
    }
    @if (username.errors?.['minlength']) {
      <span class="error">Tối thiểu 3 ký tự</span>
    }
  }

  <input formControlName="password"
         type="password"
         placeholder="Mật khẩu">

  <button type="submit" [disabled]="loginForm.invalid">
    Đăng nhập
  </button>
</form>
```

---

## 4. `@Input()` — Parent Truyền Dữ Liệu Xuống Child

```typescript
// ===== Kịch bản: Parent có danh sách accounts
//                Child hiển thị 1 account card =====

// CHILD Component: AccountCardComponent
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-account-card',
  standalone: true,
  template: `
    <div class="card" [class.selected]="isSelected">
      <h4>{{ account.accountName }}</h4>
      <p>{{ account.accountNumber }}</p>
      <strong>{{ account.balance | number:'1.0-0' }} VND</strong>
    </div>
  `
})
export class AccountCardComponent {
  @Input() account!: BankAccount;
  // ↑ @Input() = "Nhận dữ liệu từ component cha"
  // ! = Non-null assertion (cam kết sẽ được truyền vào)

  @Input() isSelected: boolean = false;
  // Có giá trị default = false
}

// PARENT Component: AccountsPage
@Component({
  template: `
    @for (acc of accounts(); track acc.id) {
      <!-- Truyền dữ liệu xuống child bằng property binding -->
      <app-account-card
        [account]="acc"
        [isSelected]="selectedAccount()?.id === acc.id"
      />
      <!--  ↑ [] = property binding — truyền dữ liệu từ parent xuống @Input() -->
    }
  `
})
export class AccountsPage {
  accounts = signal<BankAccount[]>([]);
  selectedAccount = signal<BankAccount | null>(null);
}
```

---

## 5. `@Output()` + EventEmitter — Child Báo Cáo Ngược Lên Parent

```typescript
// CHILD Component: AccountCardComponent
import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-account-card',
  standalone: true,
  template: `
    <div class="card" (click)="onCardClick()">
      <h4>{{ account.accountName }}</h4>
      <!-- ... -->
      <button (click)="onFreezeClick($event)">Phong tỏa</button>
    </div>
  `
})
export class AccountCardComponent {
  @Input()  account!: BankAccount;

  // @Output() khai báo event mà child CÓ THỂ phát lên parent
  @Output() accountSelected = new EventEmitter<BankAccount>();
  //         ↑ Tên event       EventEmitter<kiểu dữ liệu gửi kèm>

  @Output() freezeRequested = new EventEmitter<string>();
  // Phát event kèm accountId (string)

  onCardClick(): void {
    this.accountSelected.emit(this.account);
    //                    ↑ Phát event kèm dữ liệu lên parent
  }

  onFreezeClick(event: MouseEvent): void {
    event.stopPropagation(); // Ngăn bubble lên (click vào nút không trigger click card)
    this.freezeRequested.emit(this.account.id); // Phát event kèm ID
  }
}


// PARENT Component: AccountsPage
@Component({
  template: `
    @for (acc of accounts(); track acc.id) {
      <app-account-card
        [account]="acc"
        [isSelected]="selectedAccount()?.id === acc.id"
        (accountSelected)="selectAccount($event)"
        (freezeRequested)="handleFreeze($event)"
      />
      <!-- (eventName)="handler($event)"
           $event = dữ liệu được phát từ EventEmitter -->
    }
  `
})
export class AccountsPage {
  selectAccount(acc: BankAccount): void {
    this.selectedAccount.set(acc); // Parent xử lý event từ child
  }

  handleFreeze(accountId: string): void {
    this.accountService.freezeAccount(accountId).subscribe(/* ... */);
  }
}
```

---

## 6. Sơ Đồ Luồng Giao Tiếp

```
PARENT (AccountsPage)
    │
    │ @Input([account], [isSelected])
    │ Truyền DỮ LIỆU xuống
    ▼
CHILD (AccountCardComponent)
    │
    │ @Output (accountSelected), (freezeRequested)
    │ Phát EVENT lên
    ▼
PARENT (AccountsPage)
    │
    │ (accountSelected)="selectAccount($event)"
    │ Xử lý event
    ▼
PARENT cập nhật state → Angular re-render CHILD với dữ liệu mới
```

---

## 7. Content Projection — `<ng-content>`

```typescript
// Tạo component Card tái sử dụng với content projection
@Component({
  selector: 'app-card',
  template: `
    <div class="card">
      <div class="card-header">
        <ng-content select="[card-header]" />
        <!-- ↑ Nhận nội dung được đánh dấu [card-header] từ parent -->
      </div>
      <div class="card-body">
        <ng-content />
        <!-- ↑ Nhận nội dung chính từ parent (không có selector) -->
      </div>
      <div class="card-footer">
        <ng-content select="[card-footer]" />
      </div>
    </div>
  `
})
export class CardComponent {}

// Dùng trong parent:
<app-card>
  <h3 card-header>Tài khoản Thanh toán</h3>
  <!-- Phần body mặc định -->
  <p>Số dư: 5,000,000 VND</p>
  <button card-footer>Xem chi tiết</button>
</app-card>
```

---

## 8. ViewChild — Truy Cập Component Con Từ Parent

```typescript
import { ViewChild, ElementRef } from '@angular/core';

@Component({
  template: `
    <input #phoneInput type="text">
    <app-otp-input #otpComponent />
  `
})
export class TransferPage {
  @ViewChild('phoneInput') phoneInputRef!: ElementRef;
  // ↑ Truy cập DOM element #phoneInput

  @ViewChild('otpComponent') otpComponent!: OtpInputComponent;
  // ↑ Truy cập instance của component #otpComponent

  autoFocusPhone(): void {
    this.phoneInputRef.nativeElement.focus(); // Focus vào input
  }

  resetOtp(): void {
    this.otpComponent.reset(); // Gọi method của component con
  }
}
```

---

## 9. Validators Tích Hợp Sẵn

```typescript
import { Validators } from '@angular/forms';

// Các validator có sẵn:
Validators.required          // Không được để trống
Validators.minLength(6)      // Tối thiểu 6 ký tự
Validators.maxLength(50)     // Tối đa 50 ký tự
Validators.min(1000)         // Số tối thiểu = 1000
Validators.max(500000000)    // Số tối đa = 500 triệu
Validators.email             // Phải là định dạng email
Validators.pattern(/^[0-9]+$/) // Chỉ chứa số

// Custom validator:
function phoneValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null; // Không validate nếu rỗng (kết hợp với required)

  // Số điện thoại VN: 10 số, bắt đầu bằng 0
  const isValid = /^0[0-9]{9}$/.test(value);
  return isValid ? null : { invalidPhone: 'Số điện thoại không hợp lệ' };
}

// Dùng custom validator:
const form = this.fb.group({
  phone: ['', [Validators.required, phoneValidator]]
});
```

---

## 10. Form trong BankX — Tổng Hợp

```
BANKX FORM PATTERNS:

Transfer Form (4 bước):
  Step 1: [(ngModel)] nhập STK người nhận
  Step 2: API tra cứu tên người nhận
  Step 3: Modal xác nhận (hiện thông tin transfer)
  Step 4: OTP Modal (6-digit input)

Login Form:
  FormGroup với FormBuilder
  Validators.required + Validators.minLength
  ngSubmit → authService.login()

Ledger Modal:
  Template-driven với [(ngModel)]
  Manual validation trong submitDoubleEntry()
  Disable nút khi đang submit: [disabled]="submittingLedger"
```

---

## Tổng Kết Bài 10

| Khái Niệm | Ý Nghĩa | Ví Dụ BankX |
|---|---|---|
| `FormsModule` + `[(ngModel)]` | Template-driven form two-way binding | Ledger modal |
| `ReactiveFormsModule` + `FormGroup` | Reactive form định nghĩa trong TS | Login form |
| `Validators.required` | Validation rules | Login, transfer form |
| `@Input()` | Parent truyền dữ liệu xuống child | `[account]="acc"` |
| `@Output()` + `EventEmitter` | Child phát event lên parent | `(accountSelected)="..."` |
| `$event` | Dữ liệu kèm theo event | `selectAccount($event)` |
| `<ng-content>` | Content projection — slot pattern | Card component |
| `@ViewChild` | Truy cập DOM/component con | Auto-focus input |
| `[disabled]="cond"` | Vô hiệu hóa nút khi điều kiện | `[disabled]="submitting"` |

---

## 📚 Lộ Trình Tiếp Theo

Bạn đã hoàn thành 10 bài học Angular! Đây là những gì tiếp theo để nâng cao:

```
Cấp Trung:
├── Angular Forms nâng cao (Custom Validators, Async Validators)
├── Angular Animations (@trigger, :enter, :leave)
├── NgRx (nếu app lớn cần enterprise state management)
├── RxJS nâng cao (combineLatest, withLatestFrom, scan)
└── Angular Universal (SSR — Server Side Rendering)

Cấp Senior:
├── Micro-frontend với Module Federation
├── Web Workers cho heavy computation
├── Custom Angular Libraries (publishable library)
├── Angular CDK (Component Dev Kit)
└── Performance Optimization (OnPush ChangeDetection, Virtual Scroll)
```

---

**← [Bài 09 — RxJS & HTTP](./09_rxjs_va_http_client.md)** | **← [Quay về Danh Sách Bài Học](./README.md)**
