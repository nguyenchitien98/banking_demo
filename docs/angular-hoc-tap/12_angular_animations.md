# Bài 12 — Angular Animations

> **Cấp độ:** Trung  
> **Mục tiêu:** Tạo animations mượt mà với `@angular/animations` và CSS transitions  
> **Liên quan BankX:** Toast notifications, Modal open/close, Tab transitions, Dashboard metrics

---

## 1. Hai Cách Làm Animation Trong Angular

```
Cách 1: CSS Transitions / Keyframes (BankX chủ yếu dùng)
  → Trong file .scss
  → Đơn giản, nhanh, không cần import thêm
  → Dùng cho: hover effects, fade in, slide up

Cách 2: @angular/animations API
  → Trong file .ts với trigger(), state(), transition(), animate()
  → Mạnh hơn, kiểm soát tốt hơn, có thể trigger từ TypeScript
  → Dùng cho: route transitions, list enter/leave, phức tạp hơn
```

---

## 2. CSS Animations — Cách BankX Đang Dùng

### fadeIn — Animation Cơ Bản Nhất

```scss
/* accounts.page.scss (pattern đang dùng trong BankX) */

/* Định nghĩa keyframe */
@keyframes fadeIn {
  from {
    opacity: 0;              /* Bắt đầu trong suốt hoàn toàn */
    transform: translateY(10px); /* Bắt đầu thấp hơn 10px */
  }
  to {
    opacity: 1;              /* Kết thúc hiện rõ */
    transform: translateY(0); /* Về vị trí bình thường */
  }
}

/* Áp dụng animation */
.account-item-card {
  animation: fadeIn 0.3s ease-out;
  /* animation: tên thời-gian timing-function */
}

/* Stagger — Các card xuất hiện lần lượt */
.account-item-card:nth-child(1) { animation-delay: 0ms; }
.account-item-card:nth-child(2) { animation-delay: 50ms; }
.account-item-card:nth-child(3) { animation-delay: 100ms; }
.account-item-card:nth-child(4) { animation-delay: 150ms; }
/* Kết quả: Card 1 xuất hiện ngay, card 2 sau 50ms, card 3 sau 100ms... */
```

### Slide In từ Trái / Phải

```scss
@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px); /* Bắt đầu lệch phải 30px */
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px); /* Bắt đầu lệch trái 30px */
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* Modal xuất hiện từ dưới lên */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-box {
  animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  /* cubic-bezier: custom easing với overshoot (hơi nảy lên một chút) */
}

/* Overlay mờ dần */
.modal-overlay {
  animation: fadeIn 0.2s ease;
}
```

### Pulse / Glow — Hiệu Ứng Nhấp Nháy

```scss
/* Badge thông báo nhấp nháy */
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.7; transform: scale(1.1); }
}

.notification-dot {
  animation: pulse 2s ease-in-out infinite; /* Lặp mãi */
}

/* Loading skeleton shimmer */
@keyframes shimmer {
  0%   { background-position: -200px 0; }
  100% { background-position: calc(200px + 100%) 0; }
}

.skeleton {
  background: linear-gradient(
    90deg,
    rgba(255,255,255,0.0)   0%,
    rgba(255,255,255,0.1)  50%,
    rgba(255,255,255,0.0) 100%
  );
  background-size: 200px 100%;
  animation: shimmer 1.5s infinite;
}
```

---

## 3. Angular Animations API — `@angular/animations`

### Setup

```typescript
// app.config.ts
import { provideAnimations } from '@angular/platform-browser/animations';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(), // ← Thêm dòng này để dùng @angular/animations
    // ...
  ]
};
```

```typescript
// Trong component cần dùng animation:
import {
  trigger,     // Đặt tên cho animation
  state,       // Trạng thái
  style,       // CSS styles cho state
  transition,  // Quy tắc chuyển state
  animate,     // Thời gian và easing
  query,       // Query elements con
  stagger,     // Delay lần lượt
  keyframes,   // Custom keyframes
  group,       // Chạy nhiều animation song song
  sequence,    // Chạy nhiều animation tuần tự
} from '@angular/animations';
```

---

### Animation Cơ Bản — Toast Notification

```typescript
// notification.component.ts

import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-toast',
  standalone: true,
  animations: [
    trigger('toastAnimation', [
    //        ↑ Tên trigger — dùng trong HTML: [@toastAnimation]

      transition(':enter', [
      //         ↑ ':enter' = khi element XUẤT HIỆN trong DOM
        style({ opacity: 0, transform: 'translateY(-20px)' }), // Trạng thái ban đầu
        animate('300ms ease-out',                              // Thời gian và easing
          style({ opacity: 1, transform: 'translateY(0)' })   // Trạng thái cuối
        )
      ]),

      transition(':leave', [
      //         ↑ ':leave' = khi element BỊ XÓA khỏi DOM
        animate('200ms ease-in',
          style({ opacity: 0, transform: 'translateY(-10px)' })
        )
      ])
    ])
  ],
  template: `
    @if (message) {
      <div class="toast" [@toastAnimation]>
        {{ message }}
      </div>
    }
  `
  // [@toastAnimation] kích hoạt trigger 'toastAnimation'
  // Khi @if thêm/xóa element → :enter/:leave được trigger
})
export class ToastComponent {
  @Input() message: string | null = null;
}
```

---

### Animation với State — Open/Close Modal

```typescript
// Dùng khi muốn toggle animation dựa trên state

@Component({
  animations: [
    trigger('modalState', [

      state('open', style({
        opacity: 1,
        transform: 'scale(1) translateY(0)'
      })),

      state('closed', style({
        opacity: 0,
        transform: 'scale(0.9) translateY(20px)'
      })),

      // Chuyển từ closed sang open
      transition('closed => open', [
        animate('300ms cubic-bezier(0.34, 1.56, 0.64, 1)')
        //       ↑ 300ms với easing có overshoot (hiệu ứng nảy nhẹ)
      ]),

      // Chuyển từ open sang closed
      transition('open => closed', [
        animate('200ms ease-in')
      ])
    ])
  ],
  template: `
    <div class="modal-box"
         [@modalState]="isOpen ? 'open' : 'closed'">
      <!-- nội dung modal -->
    </div>
  `
})
export class ModalComponent {
  isOpen = false;

  open()  { this.isOpen = true; }
  close() { this.isOpen = false; }
}
```

---

### Stagger Animation — List Enter/Leave

```typescript
// Dashboard — Cards xuất hiện lần lượt từ trái sang phải

@Component({
  animations: [
    trigger('listAnimation', [
      transition('* => *', [           // Mỗi lần list thay đổi
        query(':enter', [              // Query tất cả elements mới enter
          style({ opacity: 0, transform: 'translateY(20px)' }),
          stagger(60, [               // Mỗi item cách nhau 60ms
            animate('400ms ease-out',
              style({ opacity: 1, transform: 'translateY(0)' })
            )
          ])
        ], { optional: true })        // optional: true nếu có thể không có :enter
      ])
    ])
  ],
  template: `
    <div [@listAnimation]="accounts().length">
    <!--   ↑ Trigger khi length thay đổi → re-run animation -->
      @for (acc of accounts(); track acc.id) {
        <div class="account-card">{{ acc.accountName }}</div>
      }
    </div>
  `
})
export class AccountsPage {
  accounts = signal<BankAccount[]>([]);
}
```

---

### Route Transition Animation

```typescript
// app.component.ts — Animation khi chuyển route

@Component({
  selector: 'app-root',
  animations: [
    trigger('routeAnimation', [
      transition('* <=> *', [         // Mọi route change
        query(':enter', [
          style({ opacity: 0, transform: 'translateX(20px)' }),
          animate('350ms ease-out',
            style({ opacity: 1, transform: 'translateX(0)' })
          )
        ], { optional: true }),

        query(':leave', [
          animate('200ms ease-in',
            style({ opacity: 0, transform: 'translateX(-20px)' })
          )
        ], { optional: true })
      ])
    ])
  ],
  template: `
    <div [@routeAnimation]="getRouteAnimationState(outlet)">
      <router-outlet #outlet="outlet" />
    </div>
  `
})
export class AppComponent {
  getRouteAnimationState(outlet: RouterOutlet) {
    return outlet?.activatedRouteData?.['animation'] || '';
  }
}

// Trong routes:
{ path: 'dashboard', component: DashboardPage, data: { animation: 'dashboard' } }
{ path: 'accounts',  component: AccountsPage,  data: { animation: 'accounts' } }
```

---

## 4. AnimationBuilder — Dynamic Animation Từ TypeScript

```typescript
// Khi cần trigger animation hoàn toàn từ TypeScript (không dùng template)
import { AnimationBuilder, AnimationPlayer } from '@angular/animations';

@Component({ standalone: true })
export class BalanceCard {
  private readonly animationBuilder = inject(AnimationBuilder);

  @ViewChild('balanceEl') balanceEl!: ElementRef;

  animateBalanceUpdate(): void {
    const factory = this.animationBuilder.build([
      style({ color: '#10b981', transform: 'scale(1.1)' }),
      animate('500ms ease-out', style({ color: '#f1f5f9', transform: 'scale(1)' }))
    ]);

    const player: AnimationPlayer = factory.create(this.balanceEl.nativeElement);
    player.play();
    // Khi số dư thay đổi → màu xanh flash rồi về màu bình thường
  }
}
```

---

## 5. Micro-Interactions — Hiệu Ứng Nhỏ Tăng UX

```scss
/* Các micro-interaction BankX dùng */

/* 1. Button press effect */
.btn-primary:active {
  transform: scale(0.97);  /* Nhỏ lại 3% khi nhấn */
  transition: transform 0.05s ease;
}

/* 2. Input focus glow */
.form-input:focus {
  box-shadow: 0 0 0 3px rgba(123, 45, 139, 0.25); /* Tím glow */
  border-color: var(--bank-primary);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
  outline: none;
}

/* 3. Card hover lift */
.account-card:hover {
  transform: translateY(-4px);          /* Nâng lên 4px */
  box-shadow: 0 8px 32px rgba(0,0,0,0.4); /* Bóng đổ sâu hơn */
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

/* 4. Loading spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}
.spinner {
  width: 20px; height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* 5. Success checkmark draw */
@keyframes drawCheck {
  from { stroke-dashoffset: 50; }
  to   { stroke-dashoffset: 0; }
}
.success-check path {
  stroke-dasharray: 50;
  animation: drawCheck 0.5s ease-out 0.2s forwards;
}
```

---

## 6. Tổng Kết

| Kỹ Thuật | Dùng Khi | Ví Dụ |
|---|---|---|
| CSS `transition` | Hover, focus, click states | Button hover lift |
| CSS `@keyframes` | Animation phức tạp hơn | FadeIn, slideUp |
| `:enter` / `:leave` | Element xuất/ẩn | Toast, Modal |
| `state()` + `transition()` | Toggle 2+ states | Open/close modal |
| `stagger()` | List xuất hiện lần lượt | Account cards |
| Route animation | Chuyển trang | Page slide left/right |
| `AnimationBuilder` | Trigger từ TypeScript | Balance update flash |

**Nguyên tắc:**
> Animations nên **nhanh** (150-400ms), **có mục đích** (dẫn dắt sự chú ý), và **không gây mất tập trung**. Không thêm animation chỉ để thêm.

---

**← [Bài 11 — Forms Nâng Cao](./11_forms_nang_cao_validators.md)** | **→ [Bài 13 — NgRx State Management](./13_ngrx_state_management.md)**
