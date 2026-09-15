# Bài 05 — SCSS & CSS Flexbox / Grid Layout

> **Mục tiêu:** Hiểu toàn bộ CSS layout được dùng trong BankX, giải thích từng dòng  
> **File thực tế:** `accounts.page.scss`, `main-layout.component.scss`, `styles.scss`

---

## 1. CSS Variables (Custom Properties) — Hệ Thống Design Tokens BankX

BankX định nghĩa **Design System** toàn cục trong `styles.scss`:

```scss
/* File: frontend-web/src/styles.scss */

:root {
  /* ===== MÀU SẮC NGÂN HÀNG ===== */
  --bank-primary:          #7b2d8b;  /* Tím TPBank - màu chủ đạo */
  --bank-primary-light:    #9b4dca;  /* Tím nhạt hơn - hover states */
  --bank-accent:           #10b981;  /* Xanh lá - trạng thái thành công */
  --bank-danger:           #ef4444;  /* Đỏ - lỗi, DEBIT, danger */
  --bank-warning:          #f59e0b;  /* Vàng - cảnh báo, chờ xử lý */

  /* ===== MÀU NỀN (Dark Theme) ===== */
  --color-bg:              #0f172a;  /* Nền chính - xanh đen đậm */
  --color-surface:         #1e293b;  /* Nền card - xanh đen nhạt hơn */
  --color-surface-2:       #273348;  /* Nền card lồng nhau */
  --color-border:          rgba(148, 163, 184, 0.15); /* Viền mỏng, mờ */

  /* ===== MÀU CHỮ ===== */
  --color-text-primary:    #f1f5f9;  /* Chữ chính - trắng nhạt */
  --color-text-secondary:  #94a3b8;  /* Chữ phụ - xám */
  --color-text-muted:      #64748b;  /* Chữ mờ - ghi chú */

  /* ===== ĐỘ BO TRÒN GÓC ===== */
  --border-radius-sm:      6px;      /* Bo nhỏ - badge */
  --border-radius-md:      10px;     /* Bo vừa - card nhỏ */
  --border-radius-lg:      14px;     /* Bo lớn - card chính */
  --border-radius-xl:      20px;     /* Bo rất lớn - modal */

  /* ===== BÓNG ĐỔ (Shadow) ===== */
  --shadow-card:           0 4px 24px rgba(0,0,0,0.3); /* Shadow card */
  --shadow-button:         0 4px 14px rgba(123,45,139,0.4); /* Tím glow */

  /* ===== GRADIENT ===== */
  --bank-gradient-button:  linear-gradient(135deg, #7b2d8b 0%, #9b4dca 100%);
  /* Gradient tím → tím nhạt hơn, dùng cho nút chính */
}
```

**Cách dùng biến CSS:**

```scss
/* Bất kỳ file .scss nào trong app cũng dùng được var() */
.my-button {
  background: var(--bank-gradient-button); /* Gọi biến bằng var() */
  color: var(--color-text-primary);
  border-radius: var(--border-radius-lg);
}
```

---

## 2. CSS Flexbox — Căn Chỉnh Theo Một Chiều

**Flexbox** = Dàn layout theo **1 chiều** (hàng ngang HOẶC cột dọc).

### Các Thuộc Tính Trên Container (Cha):

```scss
/* Ví dụ từ accounts.page.scss */

.page-header {
  display: flex;
  /* ↑ Kích hoạt Flexbox. Tất cả con trực tiếp trở thành "flex items" */

  justify-content: space-between;
  /* ↑ Căn chỉnh theo CHIỀU CHÍNH (mặc định = ngang)
     Các giá trị:
     - flex-start   → Dồn về bên trái
     - flex-end     → Dồn về bên phải
     - center       → Căn giữa
     - space-between→ Item đầu sát trái, cuối sát phải, còn lại chia đều
     - space-around → Khoảng cách đều ở 2 bên mỗi item
     - space-evenly → Khoảng cách hoàn toàn bằng nhau */

  align-items: center;
  /* ↑ Căn chỉnh theo CHIỀU PHỤ (vuông góc với chiều chính)
     Với flex-direction: row (mặc định), chiều phụ là DỌCZ
     Các giá trị:
     - stretch       → Giãn full chiều phụ (mặc định)
     - flex-start    → Căn trên cùng
     - flex-end      → Căn dưới cùng
     - center        → Căn GIỮA theo chiều dọc ← Hay dùng nhất
     - baseline      → Căn theo đường baseline của text */

  flex-wrap: wrap;
  /* ↑ Khi không đủ chỗ → các item tự xuống dòng
     Giá trị: nowrap (mặc định, không xuống dòng), wrap, wrap-reverse */

  gap: 1rem;
  /* ↑ Khoảng cách giữa các item (thay thế margin, gọn hơn)
     1rem = 16px (1rem = font-size gốc của HTML) */
}

/* ===== HEADER ACTIONS: nút bên phải header ===== */
.header-actions {
  display: flex;        /* Flexbox ngang */
  align-items: center;  /* Căn giữa dọc */
  gap: 0.75rem;         /* 12px khoảng cách giữa các nút */
}
```

### flex-direction — Chiều Của Flex:

```scss
/* flex-direction: row (mặc định) → Item xếp NGANG trái sang phải */
.horizontal-layout {
  display: flex;
  flex-direction: row;       /* → Item 1 | Item 2 | Item 3 */
}

/* flex-direction: column → Item xếp DỌC từ trên xuống dưới */
.vertical-layout {
  display: flex;
  flex-direction: column;    /* ↓ Item 1
                                ↓ Item 2
                                ↓ Item 3 */
}

/* Ví dụ từ BankX: accounts-container */
.accounts-container {
  display: flex;
  flex-direction: column; /* Xếp các section dọc: Header → Grid → Footer */
  gap: 1.5rem;            /* 24px giữa các section */
}

/* column-left và column-right cũng xếp dọc */
.column-left, .column-right {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
```

### flex trên Item Con:

```scss
/* flex: grow shrink basis */
/* Hay dùng: flex: 1 → item chiếm phần còn lại */

.sidebar {
  flex: 0 0 240px; /* Không co giãn, cố định 240px */
}

.main-content {
  flex: 1;         /* Chiếm toàn bộ không gian còn lại */
}

/* Centering hoàn toàn với Flexbox */
.centered-container {
  display: flex;
  justify-content: center; /* Căn giữa NGANG */
  align-items: center;     /* Căn giữa DỌC */
  min-height: 100vh;       /* Chiều cao toàn màn hình */
}
```

---

## 3. CSS Grid — Dàn Layout Theo Hai Chiều

**Grid** = Dàn layout theo **2 chiều** (hàng VÀ cột cùng lúc).

```scss
/* ===== ACCOUNTS GRID: 2 cột cạnh nhau ===== */
/* Từ accounts.page.scss */

.accounts-grid {
  display: grid;
  /* ↑ Kích hoạt CSS Grid */

  grid-template-columns: 360px 1fr;
  /* ↑ Định nghĩa CỘT:
     - Cột 1: Cố định 360px (danh sách tài khoản)
     - Cột 2: 1fr = chiếm toàn bộ không gian còn lại (sổ ledger)
     "fr" = fractional unit = đơn vị phần */

  gap: 1.5rem;
  /* ↑ Khoảng cách giữa các ô (cả hàng lẫn cột) */

  /* Responsive: Thu nhỏ màn hình → chuyển về 1 cột */
  @media (max-width: 992px) {
    grid-template-columns: 1fr; /* Khi màn hình < 992px → 1 cột */
  }
}

/* ===== DASHBOARD GRID: nhiều card ===== */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  /* repeat(3, 1fr) = 3 cột bằng nhau
     Giống như: grid-template-columns: 1fr 1fr 1fr; */

  gap: 1rem;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr); /* Tablet: 2 cột */
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr; /* Mobile: 1 cột */
  }
}

/* ===== Khác nhau giữa Flexbox và Grid =====
 * Flexbox: 1 chiều (dàn theo hàng HOẶC cột)
 *   → Dùng cho Navigation, buttons hàng ngang, cards theo hàng
 *
 * Grid: 2 chiều (hàng VÀ cột)
 *   → Dùng cho layouts phức tạp: dashboard, form nhiều cột
 * ============================================ */
```

---

## 4. SCSS — CSS Được Nâng Cấp

### 4.1. Nesting (Lồng Selector)

```scss
/* Thay vì viết CSS dài dòng: */
.account-item-card { ... }
.account-item-card:hover { ... }
.account-item-card .acc-header { ... }
.account-item-card .acc-header .acc-type { ... }

/* SCSS cho phép LỒNG nhau, gọn hơn nhiều: */
.account-item-card {
  padding: 1rem;
  border-radius: var(--border-radius-md);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {                           /* & = selector cha (.account-item-card:hover) */
    border-color: var(--bank-primary-light);
    transform: translateY(-2px);      /* Nâng lên 2px khi hover */
  }

  &.active {                          /* .account-item-card.active */
    border-color: var(--bank-primary);
    background: rgba(123, 45, 139, 0.15);
  }

  .acc-header {                       /* .account-item-card .acc-header */
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;

    .acc-type {                       /* .account-item-card .acc-header .acc-type */
      font-weight: 700;
      font-size: 0.9rem;
      color: var(--color-text-primary);
    }
  }

  .acc-balance-display {
    .val {
      font-size: 1.4rem;
      font-weight: 800;
      color: var(--bank-accent);      /* Xanh lá cho số dư */
    }
  }
}
```

### 4.2. Variables SCSS (Khác với CSS Variables)

```scss
/* Variables SCSS — chỉ tồn tại lúc compile, không phải runtime */
$primary-color: #7b2d8b;
$card-padding:  1.5rem;

.my-card {
  background: $primary-color;
  padding: $card-padding;
}
/* ↑ Compile ra CSS thuần: background: #7b2d8b; padding: 1.5rem; */

/* BankX ưa dùng CSS Variables (--var) hơn vì có thể thay đổi runtime (Dark/Light mode) */
```

### 4.3. Mixin — Tái Sử Dụng Khối CSS

```scss
/* Định nghĩa mixin */
@mixin flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

@mixin card-base($padding: 1.5rem) {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius-lg);
  padding: $padding;
  box-shadow: var(--shadow-card);
}

/* Dùng mixin: */
.otp-container {
  @include flex-center;      /* Thêm toàn bộ CSS trong mixin */
  min-height: 100vh;
}

.card {
  @include card-base;         /* padding mặc định 1.5rem */
}

.card-compact {
  @include card-base(0.75rem); /* padding tùy chỉnh */
}
```

---

## 5. Box Model — Cách Tính Kích Thước Phần Tử

```scss
/* Box Model từ ngoài vào trong:
   +------------------------------------------+
   |              MARGIN (khoảng cách ngoài)   |
   |  +------------------------------------+   |
   |  |          BORDER (viền)             |   |
   |  |  +------------------------------+  |   |
   |  |  |        PADDING (đệm trong)   |  |   |
   |  |  |  +------------------------+  |  |   |
   |  |  |  |      CONTENT           |  |  |   |
   |  |  |  |  (width x height)      |  |  |   |
   |  |  |  +------------------------+  |  |   |
   |  |  +------------------------------+  |   |
   |  +------------------------------------+   |
   +------------------------------------------+
*/

/* Mặc định: width không bao gồm padding và border */
/* → Dùng box-sizing: border-box để width BAO GỒM tất cả: */

* {
  box-sizing: border-box; /* BankX đặt trong styles.scss — áp dụng toàn app */
}

.card {
  width: 360px;      /* Tổng chiều rộng = 360px (bao gồm padding + border) */
  padding: 1.5rem;   /* 24px padding 4 phía */
  border: 2px solid var(--color-border);
  margin: 1rem;      /* 16px margin bên ngoài */
}
```

---

## 6. Transitions & Animations — Hiệu Ứng Mượt Mà

```scss
/* ===== TRANSITION: Thay đổi trơn tru ===== */

.account-item-card {
  transition: all 0.2s ease;
  /* transition: thuộc-tính thời-gian timing-function
     all       → Áp dụng cho tất cả thuộc tính thay đổi
     0.2s      → Diễn ra trong 0.2 giây
     ease      → Bắt đầu chậm, tăng tốc, cuối chậm lại
     
     Các timing function:
     - ease         → Chậm-nhanh-chậm (mặc định)
     - linear       → Tốc độ đều
     - ease-in      → Bắt đầu chậm
     - ease-out     → Kết thúc chậm (hay dùng nhất)
     - ease-in-out  → Cả hai đầu chậm */

  &:hover {
    transform: translateY(-2px); /* Nâng lên 2px - cần transition để mượt */
    border-color: var(--bank-primary-light);
  }
}

/* ===== KEYFRAME ANIMATION ===== */

@keyframes fadeIn {
  from {
    opacity: 0;           /* Bắt đầu trong suốt */
    transform: translateY(10px); /* Bắt đầu thấp hơn 10px */
  }
  to {
    opacity: 1;           /* Kết thúc hiện rõ */
    transform: translateY(0);    /* Về vị trí bình thường */
  }
}

/* Áp dụng animation */
.toast-notification {
  animation: fadeIn 0.3s ease-out;
  /* animation: tên thời-gian timing
     Component xuất hiện với hiệu ứng fade + slide lên */
}

/* Spinner loading */
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.spinner {
  animation: spin 1s linear infinite; /* Quay mãi mãi */
}
```

---

## 7. Responsive Design — Media Queries

```scss
/* ===== BREAKPOINTS BankX ===== */
/* Mobile First: Viết CSS cho mobile trước, rồi override cho màn lớn hơn */

/* Màn hình nhỏ (default) — mobile */
.accounts-grid {
  display: grid;
  grid-template-columns: 1fr; /* 1 cột trên mobile */
}

/* Tablet (>= 768px) */
@media (min-width: 768px) {
  .accounts-grid {
    grid-template-columns: 300px 1fr; /* 2 cột */
  }
}

/* Desktop (>= 992px) */
@media (min-width: 992px) {
  .accounts-grid {
    grid-template-columns: 360px 1fr; /* Cột trái rộng hơn */
  }
}

/* Large Desktop (>= 1280px) */
@media (min-width: 1280px) {
  .accounts-grid {
    grid-template-columns: 420px 1fr;
  }
}
```

---

## 8. CSS Scoping trong Angular

```scss
/* accounts.page.scss */
/* CSS trong file này CHỈ ảnh hưởng tới accounts.page.html */
/* KHÔNG leak sang component khác */

.card {
  background: var(--color-surface); /* Chỉ .card trong AccountsPage */
}

/* Angular compile ra: */
/* .card[_ngcontent-bankx-c123] { background: ...; }
   ← Angular thêm attribute selector để scope CSS */
```

**Khi muốn style VƯỢT qua scoping (Parent style Child component):**

```scss
/* Dùng :host::ng-deep hoặc ::ng-deep */
::ng-deep .mat-input-element {
  color: white; /* Override style của Angular Material từ bên ngoài */
}
```

---

## 9. CSS Thường Dùng Nhất — Cheat Sheet BankX

```scss
/* ===== TYPOGRAPHY ===== */
font-size: 1rem;        /* 16px - text thường */
font-size: 0.875rem;    /* 14px - text nhỏ, label */
font-size: 0.75rem;     /* 12px - text rất nhỏ, hint */
font-size: 1.25rem;     /* 20px - heading nhỏ */
font-size: 1.5rem;      /* 24px - heading */
font-size: 2rem;        /* 32px - heading lớn */

font-weight: 400;       /* Normal */
font-weight: 600;       /* Semi-bold */
font-weight: 700;       /* Bold */
font-weight: 800;       /* Extra-bold (BankX dùng nhiều) */

/* ===== SPACING ===== */
/* 1rem = 16px — BankX dùng rem cho mọi spacing */
padding: 0.5rem;   /* 8px */
padding: 0.75rem;  /* 12px */
padding: 1rem;     /* 16px */
padding: 1.5rem;   /* 24px */
padding: 2rem;     /* 32px */

margin: 0.5rem;    /* 8px */

gap: 0.5rem;       /* Khoảng cách Flex/Grid nhỏ */
gap: 1rem;         /* Khoảng cách thông thường */
gap: 1.5rem;       /* Khoảng cách lớn */

/* ===== BORDERS ===== */
border: 1px solid var(--color-border);    /* Viền mỏng */
border: 2px solid var(--bank-primary);    /* Viền accent */
border-radius: var(--border-radius-lg);   /* Bo tròn */
border: none;                             /* Không viền */

/* ===== COLORS ===== */
color: var(--color-text-primary);     /* Chữ chính */
color: var(--color-text-secondary);   /* Chữ phụ */
background: var(--color-surface);     /* Nền card */
background: var(--bank-gradient-button); /* Gradient button */

/* ===== DISPLAY ===== */
display: flex;          /* Flexbox */
display: grid;          /* Grid */
display: block;         /* Block element */
display: inline-block;  /* Inline nhưng có width/height */
display: none;          /* Ẩn hoàn toàn */

/* ===== POSITION ===== */
position: relative;     /* Tham chiếu cho absolute con */
position: absolute;     /* Absolute trong parent relative */
position: fixed;        /* Cố định theo viewport */
position: sticky;       /* Dính khi scroll */

/* ===== OVERFLOW ===== */
overflow: hidden;    /* Cắt nội dung tràn */
overflow: auto;      /* Scrollbar khi tràn */
overflow-x: auto;    /* Scrollbar ngang */

/* ===== CURSOR ===== */
cursor: pointer;     /* Con trỏ tay (cho button, clickable) */
cursor: not-allowed; /* Cấm (cho disabled) */
cursor: grab;        /* Bàn tay mở (drag) */
```

---

## Tổng Kết

| Kỹ Thuật | Tác Dụng | Ví Dụ BankX |
|---|---|---|
| `display: flex` | Layout 1 chiều | `.header-actions`, `.page-header` |
| `justify-content` | Căn chỉnh chiều chính | `space-between` trong header |
| `align-items: center` | Căn giữa chiều phụ | Mọi nơi dùng Flex |
| `gap` | Khoảng cách giữa flex/grid items | `gap: 1.5rem` |
| `display: grid` | Layout 2 chiều | `.accounts-grid` |
| `grid-template-columns` | Định nghĩa cột | `360px 1fr` |
| `@media` | Responsive breakpoints | `max-width: 992px` → 1 cột |
| `transition` | Hiệu ứng chuyển tiếp | Hover button, card |
| `var(--name)` | CSS Variables | Design tokens BankX |
| SCSS `&:hover` | Nesting selector | `&.active`, `&:hover` |

---

**← [Bài 04 — Template HTML](./04_template_html_va_directives.md)** | **→ [Bài 06 — Services & Dependency Injection](./06_services_va_dependency_injection.md)**
