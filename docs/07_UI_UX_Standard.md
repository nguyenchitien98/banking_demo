# BankX Digital Banking Platform — Tiêu Chuẩn UI/UX Banking (TPBank Style)

Tài liệu này đặc tả quy chuẩn thiết kế giao diện người dùng theo phong cách **TPBank** cho BankX Digital Banking Platform.

---

## 1. Ngôn Ngữ Thiết Kế TPBank (Design Language)

Nhìn vào UI_TPBank.png, ta thấy TPBank dùng:
- **Màu chủ đạo:** Tím (Purple/Violet) #7B2D8B
- **Gradient:** Tím → Xanh dương (Purple to Blue)
- **Background app:** Trắng sáng (#FFFFFF, #F5F5F5)
- **Bottom Navigation:** 5 tabs (Trang chủ, Tài khoản, Quét QR, Ưu đãi, Cá nhân)
- **Card tài khoản:** Gradient tím, góc bo tròn lớn, số tài khoản masked
- **Giao dịch:** Xanh lá (nhận tiền), Đỏ (chuyển tiền)
- **Typography:** Clean, sans-serif (Be Vietnam Pro hoặc Inter)

---

## 2. Design Tokens — CSS Variables

```scss
// banking/frontend-web/src/styles/banking-theme.scss
:root {
  // ====== BANKING COLOR PALETTE (TPBank inspired) ======
  
  // Primary — Tím TPBank
  --bank-purple-50:  #F3E8FF;
  --bank-purple-100: #E9D5FF;
  --bank-purple-200: #D8B4FE;
  --bank-purple-300: #C084FC;
  --bank-purple-400: #A855F7;
  --bank-purple-500: #9333EA;
  --bank-purple-600: #7C3AED;   // ← Màu chủ đạo tím
  --bank-purple-700: #6D28D9;
  --bank-purple-800: #5B21B6;
  --bank-purple-900: #4C1D95;

  // Brand Primary (Main purple like TPBank)
  --bank-primary:         #7B2D8B;   // TPBank signature purple
  --bank-primary-light:   #9B5AB8;
  --bank-primary-dark:    #5B1D6B;
  --bank-primary-rgb:     123, 45, 139;

  // Gradient — Đặc trưng TPBank (Tím → Xanh)
  --bank-gradient-hero:   linear-gradient(135deg, #7B2D8B 0%, #4A6CF7 100%);
  --bank-gradient-card:   linear-gradient(135deg, #6B1F7B 0%, #9B4DBB 100%);
  --bank-gradient-button: linear-gradient(135deg, #7C3AED 0%, #4F46E5 100%);
  --bank-gradient-subtle: linear-gradient(135deg, rgba(123,45,139,0.1) 0%, rgba(74,108,247,0.1) 100%);

  // ====== BACKGROUND ======
  --bg-page:            #F5F5F5;   // Light grey page background
  --bg-card:            #FFFFFF;   // White card background
  --bg-input:           #F5F5F5;   // Input field background
  --bg-bottom-nav:      #FFFFFF;
  --bg-header:          #FFFFFF;
  --bg-overlay:         rgba(0, 0, 0, 0.5);
  --bg-skeleton:        #E5E7EB;   // Skeleton loading color

  // ====== TEXT ======
  --text-primary:        #1A1A2E;   // Dark navy - main text
  --text-secondary:      #6B7280;   // Grey - secondary text
  --text-muted:          #9CA3AF;   // Light grey - muted/caption
  --text-on-primary:     #FFFFFF;   // White text on purple backgrounds
  --text-link:           #7B2D8B;   // Purple link color

  // ====== TRANSACTION COLORS ======
  --color-money-in:      #00875A;   // Nhận tiền - xanh lá đậm
  --color-money-out:     #DE350B;   // Gửi tiền - đỏ
  --color-pending:       #6554C0;   // Đang xử lý - tím nhạt
  --color-fee:           #FF8B00;   // Phí - vàng cam

  // ====== STATUS ======
  --color-success:       #00875A;
  --color-error:         #DE350B;
  --color-warning:       #FF8B00;
  --color-info:          #0052CC;

  // ====== BORDERS ======
  --border-color:        #E5E7EB;
  --border-radius-sm:    8px;
  --border-radius-md:    12px;
  --border-radius-lg:    16px;
  --border-radius-xl:    24px;
  --border-radius-xxl:   32px;
  --border-radius-full:  9999px;

  // ====== SHADOWS ======
  --shadow-sm:   0 1px 3px rgba(0, 0, 0, 0.06);
  --shadow-md:   0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-card: 0 2px 8px rgba(0, 0, 0, 0.06), 0 0 1px rgba(0, 0, 0, 0.04);
  --shadow-button: 0 4px 14px rgba(123, 45, 139, 0.35);
  --shadow-float:  0 8px 24px rgba(0, 0, 0, 0.12);

  // ====== SPACING ======
  --space-1:  4px;
  --space-2:  8px;
  --space-3:  12px;
  --space-4:  16px;
  --space-5:  20px;
  --space-6:  24px;
  --space-8:  32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;

  // ====== TYPOGRAPHY ======
  --font-family: 'Be Vietnam Pro', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  --font-weight-regular:   400;
  --font-weight-medium:    500;
  --font-weight-semibold:  600;
  --font-weight-bold:      700;

  --font-size-xs:   11px;
  --font-size-sm:   13px;
  --font-size-base: 15px;
  --font-size-md:   16px;
  --font-size-lg:   18px;
  --font-size-xl:   20px;
  --font-size-2xl:  24px;
  --font-size-3xl:  28px;
  --font-size-4xl:  32px;

  // ====== ANIMATION ======
  --transition-fast:   0.15s ease-in-out;
  --transition-normal: 0.25s ease-in-out;
  --transition-slow:   0.4s ease-in-out;

  // ====== Z-INDEX ======
  --z-base:       0;
  --z-dropdown:   100;
  --z-sticky:     200;
  --z-fixed:      300;
  --z-modal:      400;
  --z-toast:      500;
  --z-tooltip:    600;
}
```

---

## 3. Component Design Specifications

### 3.1 Account Card (Màn hình Dashboard & Tài khoản)

```scss
// Thẻ tài khoản dạng gradient tím như TPBank
.bank-account-card {
  background: var(--bank-gradient-card);
  border-radius: var(--border-radius-xl);
  padding: var(--space-6);
  color: white;
  position: relative;
  overflow: hidden;
  
  // Pattern trang trí nền (như hoa văn trên thẻ)
  &::before {
    content: '';
    position: absolute;
    top: -50px;
    right: -50px;
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
  }

  .account-type-badge {
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-medium);
    background: rgba(255, 255, 255, 0.2);
    padding: 4px 10px;
    border-radius: var(--border-radius-full);
    display: inline-block;
  }

  .account-number {
    font-size: var(--font-size-base);
    letter-spacing: 2px;    // **** **** **** 1234
    opacity: 0.8;
    margin-top: var(--space-3);
  }

  .balance-label {
    font-size: var(--font-size-sm);
    opacity: 0.7;
    margin-top: var(--space-4);
  }

  .balance-amount {
    font-size: var(--font-size-3xl);
    font-weight: var(--font-weight-bold);
    line-height: 1.2;
    margin-top: var(--space-1);
  }
}
```

### 3.2 Quick Action Shortcuts (Chuyển tiền, QR, Nạp tiền, Rút tiền)

```scss
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    cursor: pointer;
    padding: var(--space-3) var(--space-2);
    border-radius: var(--border-radius-md);
    transition: background-color var(--transition-fast);

    &:active {
      background-color: rgba(123, 45, 139, 0.08);
    }

    .action-icon-wrapper {
      width: 52px;
      height: 52px;
      border-radius: var(--border-radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      
      // Màu icon theo loại action
      &.transfer  { background: #EDE8FF; color: #7B2D8B; }
      &.qr        { background: #FFF0E6; color: #E65C00; }
      &.topup     { background: #E6F9F0; color: #00875A; }
      &.withdraw  { background: #FFE8E6; color: #DE350B; }
    }

    .action-label {
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
      color: var(--text-primary);
      text-align: center;
    }
  }
}
```

### 3.3 Transaction List Item

```scss
.transaction-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-4);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
  transition: background-color var(--transition-fast);

  &:active { background: #F9F9F9; }

  .tx-icon {
    width: 44px;
    height: 44px;
    border-radius: var(--border-radius-full);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    
    &.money-in  { background: #E6F9F0; color: #00875A; }
    &.money-out { background: #FFECE8; color: #DE350B; }
    &.payment   { background: #EDE8FF; color: #7B2D8B; }
  }

  .tx-info {
    flex: 1;
    min-width: 0;  // Quan trọng: cho phép text truncate

    .tx-name {
      font-size: var(--font-size-base);
      font-weight: var(--font-weight-medium);
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .tx-date {
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
      margin-top: 2px;
    }
  }

  .tx-amount {
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-semibold);
    flex-shrink: 0;

    &.money-in  { color: var(--color-money-in); }
    &.money-out { color: var(--color-money-out); }
  }
}
```

### 3.4 Bottom Navigation (Mobile Style)

```scss
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--bg-bottom-nav);
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom);  // iPhone notch support
  z-index: var(--z-fixed);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);

  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 60px;
    padding: var(--space-2);
    cursor: pointer;

    .nav-icon {
      font-size: 22px;
      color: var(--text-muted);
      transition: color var(--transition-fast);
    }

    .nav-label {
      font-size: var(--font-size-xs);
      color: var(--text-muted);
      transition: color var(--transition-fast);
    }

    &.active {
      .nav-icon, .nav-label { color: var(--bank-primary); }
    }

    // Nút QR ở giữa — nổi bật hơn (giống TPBank)
    &.qr-btn {
      .nav-icon-wrapper {
        width: 52px;
        height: 52px;
        background: var(--bank-gradient-button);
        border-radius: var(--border-radius-full);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-top: -20px;
        box-shadow: var(--shadow-button);

        ion-icon { color: white; font-size: 24px; }
      }
    }
  }
}
```

### 3.5 Primary Button

```scss
.btn-primary {
  width: 100%;
  height: 52px;
  background: var(--bank-gradient-button);
  color: white;
  border: none;
  border-radius: var(--border-radius-xl);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family);
  cursor: pointer;
  box-shadow: var(--shadow-button);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(123, 45, 139, 0.45);
  }

  &:active {
    transform: translateY(0);
    box-shadow: var(--shadow-button);
  }

  &:disabled {
    background: #D1D5DB;
    box-shadow: none;
    cursor: not-allowed;
    transform: none;
  }

  &.loading {
    pointer-events: none;
    .spinner {
      width: 20px; height: 20px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
  }
}
```

### 3.6 OTP Input (6 chữ số riêng biệt)

```scss
.otp-input-group {
  display: flex;
  gap: var(--space-3);
  justify-content: center;

  .otp-digit {
    width: 48px;
    height: 56px;
    border: 2px solid var(--border-color);
    border-radius: var(--border-radius-md);
    text-align: center;
    font-size: var(--font-size-2xl);
    font-weight: var(--font-weight-bold);
    color: var(--text-primary);
    background: var(--bg-input);
    transition: border-color var(--transition-fast);

    &:focus {
      border-color: var(--bank-primary);
      outline: none;
      box-shadow: 0 0 0 3px rgba(123, 45, 139, 0.15);
    }

    &.filled {
      border-color: var(--bank-primary);
      background: rgba(123, 45, 139, 0.05);
    }

    &.error {
      border-color: var(--color-error);
      animation: shake 0.3s ease-in-out;
    }
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}
```

---

## 4. Màn Hình Specification

### Screen 01 — Login

```
Layout: Full screen, gradient background tím
- Logo BankX + tagline nhỏ ở trên
- Card trắng bo tròn (glassmorphism) ở giữa chứa form
- Avatar placeholder user (nếu đã đăng nhập trước → hiện tên)
- Input: SĐT (keyboard=tel)
- Input: Mật khẩu (toggle show/hide)
- Button "Đăng nhập" gradient tím (full width)
- Divider "HOẶC"
- 3 phương thức khác: Face ID, Touch ID, OTP (icon + text)
- Link "Đăng ký tài khoản" | "Quên mật khẩu"
```

### Screen 03 — Dashboard

```
Layout: 
- Header: greeting ("Xin chào, NGUYỄN VĂN A"), notification bell, avatar
- Account card (gradient tím, swipe nếu nhiều tài khoản)
- 4 Quick Actions: Chuyển tiền | Quét QR | Nạp tiền | Rút tiền
- Section "Dịch vụ yêu thích" (scrollable horizontal)
- Banner ưu đãi (auto-scroll carousel)
- Section "Giao dịch gần đây" + Link "Xem tất cả"
- 5 transaction items
- Bottom Navigation: 5 tabs
```

### Screen 06 — Transfer Form

```
Layout:
- Header: "Chuyển tiền" + back arrow
- Tab: "Trong BankX" | "Liên ngân hàng"
- Card tài khoản nguồn (mini version)
- Input: STK/SĐT người nhận
  → Auto-lookup tên sau 0.5s debounce
  → Hiện tên người nhận (xác nhận)
- Quick amount buttons: 100K | 500K | 1M | Khác
- Input số tiền (keyboard=numeric)
- Input nội dung (optional, max 200 chars)
- Button "Tiếp tục" (disabled nếu chưa đủ thông tin)
```

### Screen 08 — OTP Input

```
Layout:
- Header: "Xác thực OTP"
- Text: "Nhập mã OTP vừa gửi đến SĐT ***456"
- 6 OTP digit inputs (separated boxes)
- Countdown timer: "1:58" (đếm ngược từ 120s)
- Link "Gửi lại OTP" (disabled khi đang countdown, active sau 60s)
- Tự động submit khi nhập đủ 6 chữ số
```

---

## 5. Micro-animations & Interactions

```scss
// Transfer Success Animation
.success-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10);

  .success-icon {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: var(--color-success);
    display: flex;
    align-items: center;
    justify-content: center;
    animation: successBounce 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
    
    svg { color: white; font-size: 40px; }
  }

  .success-amount {
    font-size: var(--font-size-3xl);
    font-weight: var(--font-weight-bold);
    color: var(--text-primary);
    margin-top: var(--space-4);
    animation: fadeSlideUp 0.4s ease 0.2s both;
  }
}

@keyframes successBounce {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes fadeSlideUp {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

// Shimmer loading skeleton
@keyframes shimmer {
  0% { background-position: -300px 0; }
  100% { background-position: 300px 0; }
}

.skeleton {
  background: linear-gradient(90deg, #F0F0F0 25%, #E0E0E0 50%, #F0F0F0 75%);
  background-size: 600px 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--border-radius-sm);
}

// Ripple effect on button press
.ripple-effect {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.35);
    width: 0; height: 0;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.3s, height 0.3s, opacity 0.3s;
    pointer-events: none;
  }

  &:active::after {
    width: 200px;
    height: 200px;
    opacity: 0;
  }
}
```

---

## 6. Angular Component Naming cho Banking UI

```typescript
// Tên component theo feature → dễ search
// Prefix: bankx- (hoặc bx-)

// Shared components
<bankx-money-display [amount]="145000000" [currency]="'VND'" />
// Output: "145,000,000 VND" với màu trắng nếu trên card tím, đen nếu trên bg trắng

<bankx-account-card [account]="account" (click)="onAccountSelect()" />
<bankx-transaction-item [transaction]="tx" />
<bankx-otp-input (otpComplete)="onOtpComplete($event)" />
<bankx-amount-input [(ngModel)]="amount" [maxAmount]="availableBalance" />
<bankx-bank-logo [bankCode]="'TCB'" />  // Techcombank logo

// Page components (trong features/)
// Login → LoginPageComponent
// Dashboard → DashboardPageComponent  
// Transfer Form → TransferFormPageComponent
// Transfer Confirm → TransferConfirmPageComponent
// Transfer OTP → TransferOtpPageComponent
// Transfer Result → TransferResultPageComponent
```

---

## 7. Responsive Layout — Mobile First

```scss
// BankX là mobile banking app → Design Mobile First
// Mobile: max-width 480px (default)
// Tablet: 481px - 768px (iPad)
// Desktop web: 769px+ (Admin portal, Engineering portal)

.page-container {
  max-width: 480px;   // Giới hạn width mobile banking
  margin: 0 auto;
  min-height: 100vh;
  background: var(--bg-page);
  position: relative;
  padding-bottom: 80px;  // Space for bottom nav
}

// Admin Portal: Full desktop layout với sidebar
.admin-layout {
  display: flex;
  min-height: 100vh;

  .admin-sidebar {
    width: 260px;
    background: var(--bg-card);
    border-right: 1px solid var(--border-color);
    flex-shrink: 0;
  }

  .admin-content {
    flex: 1;
    padding: var(--space-6);
    overflow-y: auto;
    background: var(--bg-page);
  }
}
```
