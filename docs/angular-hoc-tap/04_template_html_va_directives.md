# Bài 04 — Template HTML, Directives: @if, @for, @switch

> **Mục tiêu:** Thành thạo cú pháp Template HTML của Angular 22  
> **File thực tế:** `accounts.page.html`, `main-layout.component.html`

---

## 1. Interpolation — Hiển Thị Dữ Liệu `{{ }}`

**Interpolation** là cách hiện giá trị từ file `.ts` ra HTML.

```html
<!-- ======= CÚ PHÁP =======
{{ biểu_thức_TypeScript }}
Mọi expression TypeScript đều được
======= =================== -->

<!-- Hiển thị độ dài mảng (từ signal) -->
<h3>Tài khoản của bạn ({{ accounts().length }})</h3>
<!--                     ↑ Gọi signal như hàm — thêm () -->

<!-- Hiển thị field của object -->
<span>{{ acc.accountName }}</span>
<span>{{ acc.accountNumber }}</span>

<!-- Toán học trong {{ }} -->
<span>Tổng: {{ amount * 1.05 }}</span>

<!-- Optional chaining — tránh lỗi null -->
<span>{{ selectedAccount()?.accountNumber }}</span>
<!--                     ↑ Nếu null → hiện rỗng, không crash -->

<!-- Gọi hàm trong {{ }} -->
<div>{{ getUserInitials() }}</div>
<!--   ↑ Gọi method trong .ts, lấy kết quả ra HTML -->

<!-- String concatenation -->
<span>{{ 'Xin chào, ' + currentUser()?.username }}</span>

<!-- Pipe: format dữ liệu (sẽ học ở bài riêng) -->
{{ acc.balance | number:'1.0-0' }}
<!--           ↑ Format số: 5000000 → "5,000,000" -->
```

---

## 2. Property Binding — Gán Giá Trị Cho Thuộc Tính `[property]`

```html
<!-- ===== CÚ PHÁP =====
[thuộc_tính]="biểu_thức_ts"
Khác với interpolation: giá trị là biểu thức TS, không phải string
====================== -->

<!-- [class.tên-class]="điều-kiện" — gán CSS class có điều kiện -->
<div class="account-item-card"
     [class.active]="selectedAccount()?.id === acc.id">
<!--                ↑ Nếu đây là tài khoản được chọn → thêm class 'active' -->

<span class="acc-status-tag"
      [class.frozen]="acc.status === 'FROZEN'">
<!--               ↑ Nếu tài khoản bị phong tỏa → thêm class 'frozen' → màu đỏ -->

<!-- [disabled]="điều kiện" — vô hiệu hóa nút -->
<button [disabled]="submittingLedger">
  {{ submittingLedger ? 'Đang xử lý...' : 'Xác nhận' }}
</button>

<!-- [href] cho anchor tag -->
<a [href]="'https://example.com/' + userId">Xem profile</a>

<!-- [src] cho image -->
<img [src]="user.avatarUrl" [alt]="user.username">

<!-- [style] — gán inline style -->
<div [style.color]="riskScore > 70 ? 'red' : 'green'">
  Score: {{ riskScore }}
</div>

<!-- [ngStyle] — nhiều style cùng lúc -->
<div [ngStyle]="{
  'background-color': account.status === 'ACTIVE' ? '#10b981' : '#ef4444',
  'font-weight': 'bold'
}">{{ account.status }}</div>
```

---

## 3. Event Binding — Bắt Sự Kiện `(event)`

```html
<!-- ===== CÚ PHÁP =====
(tên_sự_kiện)="xử_lý()"
Bắt event của DOM rồi gọi method trong .ts
====================== -->

<!-- (click) — click chuột -->
<button (click)="loadAccounts()">Tải lại</button>
<button (click)="openDoubleEntryModal()">Hạch toán</button>
<div (click)="selectAccount(acc)">{{ acc.accountName }}</div>

<!-- (click) với $event — truyền event object -->
<button (click)="onButtonClick($event)">Xem chi tiết</button>
<!-- Trong .ts: onButtonClick(event: MouseEvent) { event.preventDefault(); } -->

<!-- (keyup) — phím được nhả -->
<input (keyup.enter)="submitForm()">
<!--         ↑ keyup.enter = chỉ trigger khi Enter được nhấn -->

<!-- (input) — mỗi lần text thay đổi -->
<input (input)="onSearchChange($event)">

<!-- (change) — khi mất focus và giá trị đã thay đổi -->
<select (change)="onStatusChange($event)">...</select>

<!-- (submit) — khi form submit -->
<form (submit)="onFormSubmit($event)">...</form>
```

---

## 4. Two-Way Binding — `[(ngModel)]`

```html
<!-- ===== CÚ PHÁP =====
[(ngModel)]="tên_biến"
"Banana in a box" — kết hợp [] binding và () event
Thay đổi từ HTML → .ts và từ .ts → HTML cùng lúc
====================== -->

<!-- Phải import FormsModule trong @Component({imports: [...]}) -->

<!-- Input số tiền — typed vào HTML → cập nhật biến .ts -->
<input type="number"
       [(ngModel)]="transferAmount"
       placeholder="Nhập số tiền">
<!-- Khi user gõ 500000 → biến transferAmount trong .ts = 500000 -->
<!-- Khi .ts thay đổi transferAmount → input hiển thị giá trị mới -->

<!-- Input text -->
<input type="text"
       [(ngModel)]="transferDescription"
       placeholder="Nội dung chuyển khoản">

<!-- Select / Dropdown -->
<select [(ngModel)]="debitAccountId">
  @for (acc of accounts(); track acc.id) {
    <option [value]="acc.id">{{ acc.accountName }}</option>
  }
</select>

<!-- ===== NGUYÊN LÝ PHÍA SAU [(ngModel)] ===== -->
<!-- [(ngModel)] là viết tắt của: -->
<input
  [ngModel]="transferAmount"           <!-- []: hiển thị giá trị từ .ts ra UI -->
  (ngModelChange)="transferAmount = $event"> <!-- (): bắt event thay đổi → gán lại biến -->
```

---

## 5. `@if` / `@else` — Hiển Thị Có Điều Kiện (Angular 17+)

```html
<!-- ===== CÚ PHÁP MỚI Angular 17+ =====
@if (điều kiện) { ... }
@else if (điều kiện 2) { ... }
@else { ... }
Thay thế *ngIf cũ (vẫn dùng được trong BankX)
======================================= -->

<!-- Ví dụ 1: Loading spinner -->
@if (loadingAccounts) {
  <div class="spinner">Đang tải dữ liệu...</div>
} @else {
  <div class="account-list">
    <!-- Hiện danh sách tài khoản -->
  </div>
}

<!-- Ví dụ 2: Kiểm tra signal (phải gọi bằng ()) -->
@if (selectedAccount()) {
  <span>Tài khoản: {{ selectedAccount()?.accountNumber }}</span>
} @else {
  <span>Chưa chọn tài khoản</span>
}

<!-- Ví dụ 3: Nhiều điều kiện -->
@if (riskScore < 40) {
  <span class="badge green">An toàn</span>
} @else if (riskScore < 70) {
  <span class="badge yellow">Cần OTP</span>
} @else {
  <span class="badge red">Bị chặn</span>
}

<!-- Ví dụ 4: Toast thông báo -->
@if (toastMessage) {
  <div class="toast">{{ toastMessage }}</div>
}

<!-- ===== CÁCH CŨ *ngIf (vẫn dùng trong BankX) ===== -->
<!-- BankX có cả 2 cách — cách cũ và mới đều chạy được -->
<span *ngIf="selectedAccount()">
  Tài khoản: {{ selectedAccount()?.accountNumber }}
</span>
<!-- *ngIf cần import CommonModule hoặc NgIf -->
```

---

## 6. `@for` — Lặp Danh Sách (Angular 17+)

```html
<!-- ===== CÚ PHÁP =====
@for (item of collection; track item.id) {
  <!-- HTML cho mỗi item -->
}
@empty {
  <!-- Hiện khi collection rỗng -->
}
=============================== -->

<!-- track item.id — QUAN TRỌNG:
     Giúp Angular nhận biết item nào đã thay đổi → re-render hiệu quả
     Phải track bằng ID hoặc field unique, không dùng track $index nếu có thể -->

<!-- Ví dụ 1: Danh sách tài khoản (từ accounts.page.html) -->
@for (acc of accounts(); track acc.id) {
  <div class="account-item-card"
       [class.active]="selectedAccount()?.id === acc.id"
       (click)="selectAccount(acc)">
    <span>{{ acc.accountName }}</span>
    <span>{{ acc.balance | number:'1.0-0' }} VND</span>
  </div>
} @empty {
  <p>Bạn chưa có tài khoản nào.</p>
}

<!-- Ví dụ 2: Bảng bút toán ledger -->
@for (entry of entries(); track entry.id) {
  <tr>
    <td [class.debit]="entry.entryType === 'DEBIT'"
        [class.credit]="entry.entryType === 'CREDIT'">
      {{ entry.entryType }}
    </td>
    <td>{{ entry.amount | number:'1.0-0' }}</td>
    <td>{{ entry.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
  </tr>
}

<!-- Ví dụ 3: Các biến đặc biệt trong @for -->
@for (item of list; track item.id; let i = $index; let isFirst = $first; let isLast = $last) {
  <div [class.first-item]="isFirst">
    {{ i + 1 }}. {{ item.name }}
    @if (isLast) {
      <span>(Cuối)</span>
    }
  </div>
}
<!-- $index = vị trí (0, 1, 2...) -->
<!-- $first = true nếu là item đầu tiên -->
<!-- $last = true nếu là item cuối cùng -->
<!-- $even / $odd = chẵn / lẻ -->
```

---

## 7. `@switch` — Phân Nhánh Nhiều Trường Hợp (Angular 17+)

```html
<!-- ===== CÚ PHÁP =====
@switch (biểu_thức) {
  @case (giá_trị_1) { ... }
  @case (giá_trị_2) { ... }
  @default { ... }
}
=============================== -->

<!-- Ví dụ: Hiển thị badge theo trạng thái tài khoản -->
@switch (acc.status) {
  @case ('ACTIVE') {
    <span class="badge badge-active">✓ Hoạt động</span>
  }
  @case ('FROZEN') {
    <span class="badge badge-frozen">🔒 Đã phong tỏa</span>
  }
  @case ('BLOCKED') {
    <span class="badge badge-blocked">⛔ Bị khóa</span>
  }
  @default {
    <span class="badge">{{ acc.status }}</span>
  }
}

<!-- Ví dụ khác: Transfer Step UI -->
@switch (currentStep) {
  @case (1) { <app-enter-info /> }
  @case (2) { <app-confirm-modal /> }
  @case (3) { <app-otp-input /> }
  @case (4) { <app-receipt /> }
}
```

---

## 8. Pipes — Format Dữ Liệu `| pipeName`

```html
<!-- Pipe là bộ lọc biến đổi dữ liệu TRƯỚC KHI hiện ra HTML -->
<!-- Cú pháp: {{ giá_trị | tên_pipe:tham_số }} -->

<!-- DecimalPipe: Format số -->
{{ 5000000 | number }}             <!-- → "5,000,000" -->
{{ 5000000 | number:'1.0-0' }}     <!-- → "5,000,000" (0 chữ số thập phân) -->
{{ 5.12345 | number:'1.2-2' }}     <!-- → "5.12" (2 chữ số thập phân) -->

<!-- CurrencyPipe: Format tiền tệ -->
{{ 5000000 | currency:'VND':'symbol':'1.0-0' }} <!-- → "₫5,000,000" -->
{{ 5000000 | currency:'USD' }}                  <!-- → "$5,000,000.00" -->

<!-- DatePipe: Format ngày -->
{{ entry.createdAt | date:'dd/MM/yyyy' }}         <!-- → "15/09/2026" -->
{{ entry.createdAt | date:'dd/MM/yyyy HH:mm' }}   <!-- → "15/09/2026 23:30" -->
{{ entry.createdAt | date:'relative' }}           <!-- → "3 phút trước" -->

<!-- UpperCasePipe / LowerCasePipe -->
{{ 'bankx' | uppercase }}   <!-- → "BANKX" -->
{{ 'BANKX' | lowercase }}   <!-- → "bankx" -->

<!-- AsyncPipe: Tự động subscribe Observable (quan trọng!) -->
{{ accounts$ | async }}
<!-- accounts$ là Observable<BankAccount[]>
     async pipe tự subscribe và unsubscribe tự động khi component destroy -->

<!-- JsonPipe: Debug object -->
<pre>{{ accounts() | json }}</pre>
<!-- In ra JSON của toàn bộ mảng accounts — hữu ích khi debug -->
```

---

## 9. Template Reference Variables `#tenBien`

```html
<!-- #tenBien gán reference đến DOM element hoặc component -->

<!-- Tham chiếu input field -->
<input #phoneInput type="text" placeholder="Số điện thoại">
<button (click)="sendOtp(phoneInput.value)">Gửi OTP</button>
<!--                    ↑ .value lấy giá trị của input -->

<!-- Tham chiếu component con -->
<app-otp-input #otpRef />
<button (click)="otpRef.reset()">Xóa OTP</button>
<!--              ↑ Gọi method của component con -->

<!-- Dùng với @if để type-safe -->
@if (selectedAccount(); as acc) {
  <!-- acc là BankAccount, không phải null -->
  <p>{{ acc.accountNumber }}</p>
}
```

---

## 10. Class & Style Binding — Nâng Cao

```html
<!-- [ngClass] — nhiều class có điều kiện cùng lúc -->
<div [ngClass]="{
  'is-active': isActive,
  'is-loading': loading,
  'has-error': errorMessage !== null
}">...</div>

<!-- [class] — gán object -->
<div [class]="{ active: isSelected, disabled: isDisabled }">...</div>

<!-- Kết hợp class tĩnh và động -->
<button class="btn btn-primary"
        [class.loading]="isSubmitting"
        [class.disabled]="!isValid">
  Gửi
</button>
```

---

## Tổng Kết

| Cú Pháp | Tác Dụng | Ví Dụ BankX |
|---|---|---|
| `{{ value }}` | Hiển thị giá trị | `{{ accounts().length }}` |
| `[property]` | Gán thuộc tính từ .ts | `[class.active]="..."` |
| `(event)` | Bắt sự kiện DOM | `(click)="selectAccount(acc)"` |
| `[(ngModel)]` | Two-way binding | `[(ngModel)]="transferAmount"` |
| `@if` / `@else` | Hiển thị có điều kiện | Loading spinner, toast |
| `@for` + `track` | Lặp danh sách | Danh sách tài khoản, ledger |
| `@switch` / `@case` | Phân nhánh | Account status badge |
| `\| pipe` | Format dữ liệu | `\| number`, `\| date` |
| `#ref` | Template reference | `#phoneInput` |

---

**← [Bài 03 — Component & Lifecycle](./03_component_va_lifecycle.md)** | **→ [Bài 05 — SCSS & CSS Flexbox/Grid](./05_scss_va_css_layout.md)**
