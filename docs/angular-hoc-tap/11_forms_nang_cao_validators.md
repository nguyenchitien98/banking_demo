# Bài 11 — Angular Forms Nâng Cao (Custom Validators & Async Validators)

> **Cấp độ:** Trung  
> **Mục tiêu:** Viết Custom Validator, Async Validator, Cross-field validation, Dynamic Form  
> **Liên quan BankX:** Transfer form validation, OTP form, KYC form

---

## 1. Tại Sao Cần Custom Validator?

```typescript
// Built-in validators không đủ cho banking:
// ❌ Không có: isVietnamesePhone, isValidBankAccount, isMaxDailyLimit

// Tình huống BankX cần custom validator:
// - Số điện thoại VN: 10 số, bắt đầu 03x/07x/08x/09x
// - Số tài khoản: 10-14 số
// - Số tiền: Không vượt daily limit của user
// - OTP: Phải đúng 6 chữ số

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
```

---

## 2. Custom Validator — Synchronous

### Pattern: Validator Function

```typescript
// ===== CÁCH 1: Validator Function (khuyến nghị, đơn giản) =====

// Validator kiểm tra số điện thoại Việt Nam
export function vietnamesePhoneValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;

    // Không validate nếu field rỗng (kết hợp với Validators.required)
    if (!value || value.trim() === '') return null;

    // Regex số điện thoại VN: 10 số, bắt đầu 03x/05x/07x/08x/09x
    const phoneRegex = /^(03[2-9]|05[6-9]|07[0|6-9]|08[0-9]|09[0-9])[0-9]{7}$/;

    if (phoneRegex.test(value)) {
      return null; // ✅ Hợp lệ → trả null
    }

    // ❌ Không hợp lệ → trả object mô tả lỗi
    return {
      vietnamesePhone: {
        message: 'Số điện thoại không hợp lệ (VD: 0901234567)',
        actualValue: value
      }
    };
  };
}


// Validator kiểm tra số tài khoản ngân hàng
export function bankAccountNumberValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return null;

    // Số tài khoản: 10-14 chữ số
    if (/^\d{10,14}$/.test(value)) return null;

    return {
      bankAccountNumber: {
        message: 'Số tài khoản phải từ 10-14 chữ số'
      }
    };
  };
}


// Validator số tiền transfer
export function transferAmountValidator(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as number;
    if (value === null || value === undefined) return null;

    if (value < min) {
      return {
        minTransferAmount: {
          message: `Số tiền tối thiểu là ${min.toLocaleString('vi-VN')} VND`,
          min, actual: value
        }
      };
    }

    if (value > max) {
      return {
        maxTransferAmount: {
          message: `Số tiền tối đa một lệnh là ${max.toLocaleString('vi-VN')} VND`,
          max, actual: value
        }
      };
    }

    return null;
  };
}
```

### Dùng Custom Validator trong Form:

```typescript
// transfers/pages/transfer-form/transfer-form.page.ts

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { vietnamesePhoneValidator, transferAmountValidator, bankAccountNumberValidator }
  from '../../../shared/validators/banking.validators';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './transfer-form.page.html'
})
export class TransferFormPage {
  private readonly fb = inject(FormBuilder);

  transferForm: FormGroup = this.fb.group({
    // Số tài khoản người nhận
    recipientAccount: [
      '',
      [
        Validators.required,
        bankAccountNumberValidator()   // ← Custom validator
      ]
    ],

    // Số điện thoại (nếu chuyển qua SDT)
    recipientPhone: [
      '',
      [
        vietnamesePhoneValidator()     // ← Custom validator
      ]
    ],

    // Số tiền — phải từ 1,000 đến 50,000,000 VND
    amount: [
      0,
      [
        Validators.required,
        Validators.min(1000),
        transferAmountValidator(1000, 50_000_000)  // ← Custom validator
      ]
    ],

    // Nội dung
    description: [
      '',
      [
        Validators.required,
        Validators.maxLength(200)
      ]
    ]
  });

  // Getter helpers để truy cập từng field
  get recipientAccount() { return this.transferForm.get('recipientAccount')!; }
  get amount()           { return this.transferForm.get('amount')!; }
  get description()      { return this.transferForm.get('description')!; }
}
```

### HTML hiển thị lỗi Custom Validator:

```html
<!-- transfer-form.page.html -->

<div class="form-group">
  <label>Số tài khoản người nhận</label>
  <input formControlName="recipientAccount"
         type="text"
         placeholder="Nhập số tài khoản">

  <!-- Hiển thị lỗi validation -->
  @if (recipientAccount.invalid && recipientAccount.touched) {

    @if (recipientAccount.errors?.['required']) {
      <span class="error">Số tài khoản không được để trống</span>
    }

    @if (recipientAccount.errors?.['bankAccountNumber']) {
      <!-- Lấy message từ object lỗi custom validator -->
      <span class="error">
        {{ recipientAccount.errors?.['bankAccountNumber'].message }}
      </span>
    }
  }
</div>


<div class="form-group">
  <label>Số tiền (VND)</label>
  <input formControlName="amount"
         type="number"
         placeholder="Nhập số tiền">

  @if (amount.invalid && amount.touched) {

    @if (amount.errors?.['required']) {
      <span class="error">Vui lòng nhập số tiền</span>
    }

    @if (amount.errors?.['minTransferAmount']) {
      <span class="error">
        {{ amount.errors?.['minTransferAmount'].message }}
      </span>
    }

    @if (amount.errors?.['maxTransferAmount']) {
      <span class="error">
        {{ amount.errors?.['maxTransferAmount'].message }}
      </span>
    }
  }
</div>
```

---

## 3. Cross-Field Validator — Validate Nhiều Field Cùng Lúc

```typescript
// Validator cấp FormGroup: Kiểm tra debit ≠ credit account
export function differentAccountsValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const debit  = group.get('debitAccountId')?.value;
    const credit = group.get('creditAccountId')?.value;

    if (!debit || !credit) return null; // Chưa điền đủ → bỏ qua

    if (debit === credit) {
      return {
        sameAccount: {
          message: 'Tài khoản trích Nợ và Thụ hưởng không được giống nhau'
        }
      };
    }

    return null;
  };
}


// Áp dụng validator ở cấp group (không phải control):
const ledgerForm = this.fb.group(
  {
    debitAccountId:  ['', Validators.required],
    creditAccountId: ['', Validators.required],
    amount:          [0,  Validators.required],
  },
  {
    validators: [differentAccountsValidator()]
    //          ↑ Validator ở cấp GROUP, truy cập tất cả fields
  }
);

// HTML:
// @if (ledgerForm.errors?.['sameAccount']) {
//   <span class="error">{{ ledgerForm.errors?.['sameAccount'].message }}</span>
// }
```

---

## 4. Async Validator — Validate Với API (Server-Side Check)

```typescript
// Async Validator: Kiểm tra số tài khoản có tồn tại không (gọi API)

import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap, first } from 'rxjs/operators';
import { TransferService } from '../services/transfer.service';
import { inject } from '@angular/core';

export function accountExistsValidator(transferService: TransferService): AsyncValidatorFn {
  return (control: AbstractControl): Observable<ValidationErrors | null> => {
    const accountNumber = control.value as string;

    // Bỏ qua nếu chưa có giá trị hoặc quá ngắn
    if (!accountNumber || accountNumber.length < 10) {
      return of(null); // Observable phát ra null → hợp lệ
    }

    // Gọi API tra cứu tài khoản
    return of(accountNumber).pipe(
      debounceTime(500),           // Chờ 500ms sau khi user ngừng gõ
      distinctUntilChanged(),      // Bỏ qua nếu cùng giá trị
      switchMap(accNum =>
        transferService.inquireRecipient(accNum).pipe(
          map(res => {
            if (res.code === 0 && res.data) {
              // ✅ Tài khoản tồn tại — trả về null (không có lỗi)
              // Thêm tên chủ tài khoản vào control parent để hiển thị
              control.parent?.patchValue(
                { recipientName: res.data.accountName },
                { emitEvent: false }
              );
              return null;
            }
            // ❌ Không tìm thấy
            return { accountNotFound: { message: 'Số tài khoản không tồn tại' } };
          }),
          catchError(() => {
            return of({ apiError: { message: 'Không thể tra cứu tài khoản lúc này' } });
          })
        )
      ),
      first() // Complete sau 1 emit (bắt buộc với AsyncValidator)
    );
  };
}


// Đăng ký Async Validator trong FormGroup:
this.transferForm = this.fb.group({
  recipientAccount: [
    '',
    [Validators.required, bankAccountNumberValidator()],  // Sync validators (chạy trước)
    [accountExistsValidator(this.transferService)]        // Async validators (chạy sau sync)
  ],
  recipientName: [{ value: '', disabled: true }], // Điền tự động từ async validator
  amount: [0, [Validators.required]],
});


// HTML — Hiển thị loading khi async validator đang chạy:
// <div [class.checking]="recipientAccount.pending">
//   @if (recipientAccount.pending) {
//     <span>🔍 Đang tra cứu tài khoản...</span>
//   }
//   @if (recipientAccount.errors?.['accountNotFound']) {
//     <span class="error">{{ recipientAccount.errors?.['accountNotFound'].message }}</span>
//   }
//   @if (recipientAccount.valid) {
//     <span class="success">✓ {{ recipientName.value }}</span>
//   }
// </div>
```

---

## 5. Dynamic Form — FormArray

```typescript
// FormArray: Form có số lượng field thay đổi
// Ví dụ: Thêm nhiều người thụ hưởng một lúc

import { FormArray, FormControl } from '@angular/forms';

@Component({ standalone: true, imports: [ReactiveFormsModule, CommonModule] })
export class BulkTransferPage {
  private readonly fb = inject(FormBuilder);

  bulkForm: FormGroup = this.fb.group({
    description: ['Chuyển tiền hàng loạt'],
    transfers: this.fb.array([])  // Mảng dynamic
  });

  // Getter cho FormArray
  get transfers(): FormArray {
    return this.bulkForm.get('transfers') as FormArray;
  }

  // Tạo 1 transfer group
  createTransferGroup() {
    return this.fb.group({
      accountNumber: ['', [Validators.required, bankAccountNumberValidator()]],
      amount:        [0,  [Validators.required, Validators.min(1000)]],
      note:          ['']
    });
  }

  // Thêm 1 transfer row mới
  addTransfer(): void {
    this.transfers.push(this.createTransferGroup());
  }

  // Xóa transfer row theo index
  removeTransfer(index: number): void {
    this.transfers.removeAt(index);
  }

  // Khởi đầu với 1 row
  ngOnInit(): void {
    this.addTransfer();
  }
}
```

```html
<!-- Bulk Transfer Form HTML -->
<form [formGroup]="bulkForm" (ngSubmit)="onSubmit()">

  <div formArrayName="transfers">
  <!--     ↑ Khai báo FormArray trong HTML -->

    @for (transfer of transfers.controls; track $index; let i = $index) {
      <div [formGroupName]="i" class="transfer-row">
      <!--     ↑ Khai báo FormGroup trong FormArray bằng index -->

        <input formControlName="accountNumber" placeholder="Số tài khoản">
        <input formControlName="amount" type="number" placeholder="Số tiền">
        <input formControlName="note" placeholder="Nội dung">

        <button type="button" (click)="removeTransfer(i)">🗑️ Xóa</button>
      </div>
    }
  </div>

  <button type="button" (click)="addTransfer()">+ Thêm Transfer</button>
  <button type="submit" [disabled]="bulkForm.invalid">Gửi Tất Cả</button>

</form>
```

---

## 6. Form Status — Valid / Invalid / Pending / Dirty / Touched

```typescript
// Các trạng thái của FormControl/FormGroup:

const ctrl = this.transferForm.get('amount')!;

ctrl.valid;    // true nếu tất cả validators pass
ctrl.invalid;  // true nếu bất kỳ validator nào fail
ctrl.pending;  // true khi async validator đang chạy
ctrl.pristine; // true nếu user CHƯA thay đổi giá trị
ctrl.dirty;    // true nếu user ĐÃ thay đổi giá trị
ctrl.touched;  // true nếu user đã focus rồi blur (mất focus)
ctrl.untouched;// true nếu chưa bao giờ touched

// Dùng trong template:
// [class.is-invalid]="amount.invalid && amount.touched"
// [class.is-valid]="amount.valid && amount.dirty"

// Programmatic:
this.transferForm.patchValue({ description: 'Nội dung mới' });
// patchValue: Cập nhật một số field (không cần điền tất cả)

this.transferForm.setValue({ /* phải điền tất cả fields */ });
// setValue: Cập nhật toàn bộ form

this.transferForm.reset(); // Reset về giá trị ban đầu
this.transferForm.markAllAsTouched(); // Force hiện tất cả lỗi

// Lấy giá trị:
const formValue = this.transferForm.value;        // Tất cả values (kể cả disabled? Không)
const rawValue  = this.transferForm.getRawValue(); // Tất cả values (kể cả disabled)
```

---

## 7. Tổng Kết

| Khái Niệm | Tác Dụng | Ví Dụ |
|---|---|---|
| `ValidatorFn` | Custom sync validator | Số điện thoại VN, số tài khoản |
| `AsyncValidatorFn` | Validator gọi API | Kiểm tra tài khoản tồn tại |
| Group-level validator | Cross-field validation | Debit ≠ Credit account |
| `FormArray` | Form với rows dynamic | Bulk transfer |
| `.pending` | Async validator đang chạy | Loading indicator |
| `.patchValue()` | Cập nhật một số field | Tự điền tên từ API |
| `.markAllAsTouched()` | Force hiện lỗi | Submit button click |

---

**← [Bài 10 — Forms & Communication](./10_forms_input_output_communication.md)** | **→ [Bài 12 — Angular Animations](./12_angular_animations.md)**
