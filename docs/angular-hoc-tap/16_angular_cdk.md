# Bài 16 — Angular CDK (Component Dev Kit)

> **Cấp độ:** Senior  
> **Mục tiêu:** Dùng Angular CDK để tạo Overlay, Drag&Drop, Clipboard, A11y, Portal  
> **Liên quan BankX:** Modal overlay, Toast positioning, Drag-to-reorder beneficiaries

---

## 1. Angular CDK là Gì?

**Angular CDK** = Bộ công cụ cấp thấp để xây dựng UI components **không có style** (unstyled primitives).

```
Angular Material  = CDK + CSS (Material Design)
→ Dùng nếu muốn có sẵn Material Design

Angular CDK  = Building blocks không có CSS
→ Dùng khi muốn style theo thiết kế riêng (BankX Purple Theme)
```

```bash
# Cài đặt (tách với Angular Material)
npm install @angular/cdk
```

```typescript
// Import các module cần:
import { OverlayModule }      from '@angular/cdk/overlay';
import { DragDropModule }     from '@angular/cdk/drag-drop';
import { ClipboardModule }    from '@angular/cdk/clipboard';
import { A11yModule }         from '@angular/cdk/a11y';
import { PortalModule }       from '@angular/cdk/portal';
import { ScrollingModule }    from '@angular/cdk/scrolling';  // Virtual Scroll
```

---

## 2. Overlay — Toast / Popup / Dropdown Chuyên Nghiệp

```typescript
// Toast Service dùng CDK Overlay

import { Injectable, inject } from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly overlay = inject(Overlay);
  private overlayRef: OverlayRef | null = null;

  show(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    // Đóng toast cũ nếu có
    this.dismiss();

    // Cấu hình vị trí overlay
    const positionStrategy = this.overlay
      .position()
      .global()
      .bottom('24px')   // Cách bottom 24px
      .centerHorizontally(); // Căn giữa ngang

    // Tạo overlay
    this.overlayRef = this.overlay.create({
      positionStrategy,
      hasBackdrop: false,      // Không có backdrop mờ
      panelClass: 'toast-panel'
    });

    // Tạo portal từ component
    const portal = new ComponentPortal(ToastComponent);
    const componentRef = this.overlayRef.attach(portal);

    // Truyền data vào component
    componentRef.instance.message = message;
    componentRef.instance.type = type;

    // Tự đóng sau 4 giây
    setTimeout(() => this.dismiss(), 4000);
  }

  dismiss(): void {
    this.overlayRef?.dispose();
    this.overlayRef = null;
  }
}


// ToastComponent (được inject vào Overlay)
@Component({
  selector: 'app-toast-overlay',
  standalone: true,
  template: `
    <div class="toast" [class]="type" [@fadeInUp]>
      {{ message }}
    </div>
  `,
  styles: [`
    .toast {
      background: #1e293b;
      color: #f1f5f9;
      padding: 12px 24px;
      border-radius: 10px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.4);
      min-width: 300px;
      text-align: center;

      &.success { border-left: 4px solid #10b981; }
      &.error   { border-left: 4px solid #ef4444; }
      &.info    { border-left: 4px solid #7b2d8b; }
    }
  `]
})
export class ToastComponent {
  message = '';
  type = 'info';
}
```

### Custom Dropdown với Overlay:

```typescript
// Dropdown không dùng select mặc định của browser
// → Có thể style hoàn toàn theo BankX design

@Component({
  selector: 'bankx-select',
  standalone: true,
  imports: [OverlayModule, CommonModule],
  template: `
    <div class="select-trigger" (click)="toggleDropdown()" #trigger>
      {{ selectedLabel || placeholder }}
    </div>

    <ng-template #dropdownTemplate>
      <div class="dropdown-panel"
           cdkTrapFocus  <!-- CDK: Giữ focus trong dropdown (A11y) -->
           [@dropdownAnim]>
        @for (option of options; track option.value) {
          <div class="option"
               [class.selected]="option.value === value"
               (click)="selectOption(option)">
            {{ option.label }}
          </div>
        }
      </div>
    </ng-template>
  `
})
export class BankxSelectComponent {
  @Input() options: { value: string; label: string }[] = [];
  @Input() placeholder = 'Chọn...';
  @Output() valueChange = new EventEmitter<string>();

  private readonly overlay = inject(Overlay);
  private overlayRef: OverlayRef | null = null;
  value = '';
  get selectedLabel() { return this.options.find(o => o.value === this.value)?.label; }

  toggleDropdown(): void {
    if (this.overlayRef?.hasAttached()) {
      this.closeDropdown();
    } else {
      this.openDropdown();
    }
  }

  openDropdown(): void {
    this.overlayRef = this.overlay.create({
      positionStrategy: this.overlay.position()
        .flexibleConnectedTo(this.triggerEl)
        .withPositions([
          { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top' }
        ]),
      scrollStrategy: this.overlay.scrollStrategies.close(),
      width: this.triggerEl.nativeElement.offsetWidth
    });
    this.overlayRef.backdropClick().subscribe(() => this.closeDropdown());
    this.overlayRef.attach(new TemplatePortal(this.dropdownTemplate, this.vcr));
  }

  closeDropdown(): void {
    this.overlayRef?.detach();
  }
}
```

---

## 3. Drag & Drop — Sắp Xếp Danh Sách Beneficiary

```typescript
// beneficiaries.page.ts — Drag to reorder beneficiaries

import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

@Component({
  standalone: true,
  imports: [DragDropModule, CommonModule],
  template: `
    <!-- cdkDropList: Container có thể nhận items drop -->
    <div cdkDropList (cdkDropListDropped)="onDrop($event)" class="beneficiary-list">

      @for (beneficiary of beneficiaries(); track beneficiary.id) {

        <!-- cdkDrag: Item có thể drag -->
        <div cdkDrag class="beneficiary-item">

          <!-- cdkDragHandle: Chỉ drag được khi kéo vào phần này -->
          <div cdkDragHandle class="drag-handle">⠿</div>

          <div class="info">
            <span>{{ beneficiary.name }}</span>
            <span>{{ beneficiary.accountNumber }}</span>
          </div>

          <!-- Placeholder khi đang drag -->
          <div *cdkDragPlaceholder class="drag-placeholder"></div>

          <!-- Preview khi đang drag (custom view) -->
          <div *cdkDragPreview class="drag-preview">
            {{ beneficiary.name }}
          </div>
        </div>
      }
    </div>
  `
})
export class BeneficiariesPage {
  beneficiaries = signal<Beneficiary[]>([]);

  onDrop(event: CdkDragDrop<Beneficiary[]>): void {
    const list = [...this.beneficiaries()]; // Copy mảng
    moveItemInArray(list, event.previousIndex, event.currentIndex);
    // moveItemInArray: CDK util di chuyển item trong array

    this.beneficiaries.set(list);
    this.saveBeneficiaryOrder(list); // Lưu thứ tự mới lên server
  }

  saveBeneficiaryOrder(ordered: Beneficiary[]): void {
    this.beneficiaryService.updateOrder(ordered.map(b => b.id)).subscribe();
  }
}
```

```scss
/* Drag & Drop styles */
.cdk-drag-animating {
  transition: transform 300ms ease; /* Animation khi drop */
}

.cdk-drag-placeholder {
  opacity: 0.3;          /* Item placeholder mờ */
  border: 2px dashed var(--bank-primary);
  border-radius: 10px;
}

.cdk-drag-preview {
  background: var(--color-surface);
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
  border-radius: 10px;
  opacity: 0.9;
}

.cdk-drop-list-dragging .beneficiary-item:not(.cdk-drag-placeholder) {
  transition: transform 250ms ease; /* Items xê dịch mượt mà */
}
```

### Multi-list Drag & Drop (Kanban style):

```html
<!-- Kéo giữa 2 danh sách — Ví dụ: Pending ↔ Completed transfers -->
<div class="kanban">

  <div cdkDropList
       #pendingList="cdkDropList"
       [cdkDropListConnectedTo]="[completedList]"
       (cdkDropListDropped)="onDrop($event, 'PENDING')"
       class="column pending">
    PENDING
    @for (t of pending(); track t.id) {
      <div cdkDrag class="card">{{ t.transactionReference }}</div>
    }
  </div>

  <div cdkDropList
       #completedList="cdkDropList"
       [cdkDropListConnectedTo]="[pendingList]"
       (cdkDropListDropped)="onDrop($event, 'COMPLETED')"
       class="column completed">
    COMPLETED
    @for (t of completed(); track t.id) {
      <div cdkDrag class="card">{{ t.transactionReference }}</div>
    }
  </div>

</div>
```

---

## 4. Clipboard — Copy Số Tài Khoản

```typescript
// Dashboard — Copy số tài khoản với 1 click

import { Clipboard } from '@angular/cdk/clipboard';

@Component({
  standalone: true,
  imports: [ClipboardModule],
  template: `
    <div class="account-number">
      {{ account.accountNumber }}

      <!-- cdkCopyToClipboard directive — copy ngay khi click -->
      <button [cdkCopyToClipboard]="account.accountNumber"
              (cdkCopyToClipboardCopied)="onCopied($event)">
        📋 Sao chép
      </button>
    </div>

    @if (copied) {
      <span class="success-hint">✓ Đã sao chép!</span>
    }
  `
})
export class AccountNumberComponent {
  @Input() account!: BankAccount;
  copied = false;

  onCopied(success: boolean): void {
    if (success) {
      this.copied = true;
      setTimeout(() => this.copied = false, 2000); // Ẩn sau 2 giây
    }
  }
}

// Hoặc dùng Clipboard Service:
@Component({ standalone: true })
export class SomeComponent {
  private readonly clipboard = inject(Clipboard);

  copyToClipboard(text: string): void {
    const success = this.clipboard.copy(text);
    if (success) {
      this.showToast('Đã sao chép!');
    }
  }
}
```

---

## 5. Focus Trap — A11y Cho Modal

```typescript
// Khi modal mở → focus bị giữ trong modal (Keyboard accessibility)

import { A11yModule, FocusTrap, FocusTrapFactory } from '@angular/cdk/a11y';

@Component({
  standalone: true,
  imports: [A11yModule],
  template: `
    @if (isOpen) {
      <!-- cdkTrapFocus: Tab/Shift+Tab chỉ đi trong element này -->
      <div class="modal" cdkTrapFocus cdkTrapFocusAutoCapture>
        <h2>Xác nhận chuyển tiền</h2>
        <input type="text" placeholder="OTP">
        <button (click)="confirm()">Xác nhận</button>
        <button (click)="close()">Hủy</button>
        <!-- Tab từ "Hủy" → quay lại input OTP (không ra ngoài modal) -->
      </div>
    }
  `
})
export class ConfirmModalComponent {
  isOpen = false;
}
```

---

## 6. Stepper — Multi-step Transfer Flow

```typescript
// CDK Stepper — Wizard pattern cho Transfer Form (4 bước BankX)
import { CdkStepperModule, CdkStepper } from '@angular/cdk/stepper';

@Component({
  selector: 'bankx-transfer-stepper',
  standalone: true,
  imports: [CdkStepperModule],
  providers: [{ provide: CdkStepper, useExisting: TransferStepperComponent }],
  template: `
    <!-- Custom stepper không có Material styling -->
    <div class="step-indicators">
      @for (step of steps; track step; let i = $index) {
        <div class="step-dot"
             [class.active]="selectedIndex === i"
             [class.completed]="i < selectedIndex">
          {{ i < selectedIndex ? '✓' : i + 1 }}
        </div>
      }
    </div>

    <div [cdkStepperNext]>Tiếp theo</div>
    <div [cdkStepperPrevious]>Quay lại</div>
  `
})
export class TransferStepperComponent extends CdkStepper { }
```

---

## 7. Platform — Detect Môi Trường

```typescript
import { Platform } from '@angular/cdk/platform';
import { DOCUMENT } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class EnvironmentService {
  private readonly platform = inject(Platform);
  private readonly platformId = inject(PLATFORM_ID);

  isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
    // true khi chạy trên browser (false khi SSR)
  }

  isMobile(): boolean {
    return this.platform.IOS || this.platform.ANDROID;
  }

  canUseLocalStorage(): boolean {
    return this.isBrowser() && typeof localStorage !== 'undefined';
  }
}
```

---

## Tổng Kết CDK

| CDK Module | Tác Dụng | BankX Use Case |
|---|---|---|
| `OverlayModule` | Popup, Dropdown, Toast | Toast service, Custom dropdown |
| `DragDropModule` | Drag & Drop | Reorder beneficiaries |
| `ClipboardModule` | Copy to clipboard | Copy account number |
| `A11yModule` + `cdkTrapFocus` | Keyboard accessibility | Modal focus management |
| `CdkStepperModule` | Multi-step wizard | Transfer 4-step flow |
| `ScrollingModule` | Virtual scroll | Transaction history |
| `Platform` | Detect browser/mobile | SSR-safe code |
| `PortalModule` | Dynamic component rendering | Toast overlay |

---

**← [Bài 15 — Performance](./15_performance_optimization.md)** | **→ [Bài 17 — Custom Libraries & Monorepo](./17_custom_libraries_monorepo.md)**
