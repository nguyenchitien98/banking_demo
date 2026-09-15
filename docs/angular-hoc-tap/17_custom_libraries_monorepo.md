# Bài 17 — Custom Angular Libraries & Monorepo (Nx)

> **Cấp độ:** Senior  
> **Mục tiêu:** Tạo shared component library, design system, setup Nx monorepo  
> **Liên quan BankX:** Tách `bankx-ui-kit` thành library dùng chung giữa Web và Admin Portal

---

## 1. Tại Sao Cần Custom Library?

```
Scenario BankX hiện tại (Monolith frontend):
  frontend-web/
    └── app/
        ├── features/accounts/   ← Dùng account-card component
        ├── features/transfers/  ← Cũng dùng account-card component
        └── features/admin/      ← Cũng dùng account-card component

Vấn đề:
- Nếu có thêm admin-portal (dự án Angular riêng) → phải copy component
- Design system (colors, typography) duplicate ở 2 nơi
- Bug fix ở 1 chỗ phải fix ở nhiều nơi

Giải pháp: Tách thành Library
  bankx-ui-kit/         ← Shared library
    ├── src/
    │   ├── account-card/
    │   ├── transfer-badge/
    │   ├── toast/
    │   └── styles/ (design tokens)
  frontend-web/         ← Import từ bankx-ui-kit
  admin-portal/         ← Cũng import từ bankx-ui-kit
```

---

## 2. Tạo Library Bằng Angular CLI

```bash
# Trong workspace Angular:
ng generate library bankx-ui-kit
# Tạo: projects/bankx-ui-kit/ với cấu trúc đầy đủ

# Cấu trúc library:
projects/bankx-ui-kit/
├── src/
│   ├── lib/
│   │   ├── account-card/
│   │   │   ├── account-card.component.ts
│   │   │   ├── account-card.component.html
│   │   │   └── account-card.component.scss
│   │   └── ...
│   └── public-api.ts  ← Export tất cả public APIs
├── ng-package.json    ← Cấu hình build library
└── tsconfig.lib.json
```

### public-api.ts — Export Public API:

```typescript
// projects/bankx-ui-kit/src/public-api.ts
// Đây là "cổng vào" của library — chỉ export những gì muốn public

export * from './lib/account-card/account-card.component';
export * from './lib/transfer-badge/transfer-badge.component';
export * from './lib/toast/toast.service';
export * from './lib/pipes/format-vnd.pipe';
export * from './lib/models/ui.models';
// Internal implementations không export → ẩn implementation details
```

---

## 3. Xây Dựng Component Library

```typescript
// projects/bankx-ui-kit/src/lib/account-card/account-card.component.ts

import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';

// Interface định nghĩa trong library
export interface AccountCardData {
  id: string;
  accountName: string;
  accountNumber: string;
  balance: number;
  status: 'ACTIVE' | 'FROZEN' | string;
  currency?: string;
}

/**
 * BankX Account Card Component
 *
 * Reusable card hiển thị thông tin tài khoản ngân hàng.
 * Dùng trong: Customer Portal, Admin Portal, Mobile WebView.
 *
 * @example
 * <bankx-account-card
 *   [account]="accountData"
 *   [selected]="isSelected"
 *   (cardClick)="onSelect($event)">
 * </bankx-account-card>
 */
@Component({
  selector: 'bankx-account-card',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush, // Library luôn dùng OnPush
  templateUrl: './account-card.component.html',
  styleUrl: './account-card.component.scss'
})
export class AccountCardComponent {
  // Required inputs — dùng required: true
  @Input({ required: true }) account!: AccountCardData;

  // Optional inputs với defaults
  @Input() selected = false;
  @Input() showBalance = true;
  @Input() variant: 'default' | 'compact' | 'mini' = 'default';

  // Events
  @Output() cardClick = new EventEmitter<AccountCardData>();

  onCardClick(): void {
    this.cardClick.emit(this.account);
  }

  get statusClass(): string {
    return `status-${this.account.status.toLowerCase()}`;
  }
}
```

```html
<!-- account-card.component.html -->
<div class="bankx-account-card"
     [class.selected]="selected"
     [class]="variant"
     (click)="onCardClick()">

  <div class="card-header">
    <span class="account-name">{{ account.accountName }}</span>
    <span class="status-badge" [class]="statusClass">{{ account.status }}</span>
  </div>

  <div class="account-number">{{ account.accountNumber }}</div>

  @if (showBalance) {
    <div class="balance">
      {{ account.balance | number:'1.0-0' }} {{ account.currency || 'VND' }}
    </div>
  }
</div>
```

---

## 4. Design System Library — Tokens & Theming

```scss
// projects/bankx-ui-kit/src/styles/tokens.scss
// Design tokens tập trung — 1 nơi duy nhất định nghĩa

:root {
  // Brand colors
  --bankx-color-primary:      #7b2d8b;
  --bankx-color-primary-100:  #9b4dca;
  --bankx-color-success:      #10b981;
  --bankx-color-danger:       #ef4444;
  --bankx-color-warning:      #f59e0b;

  // Dark theme (default)
  --bankx-bg-primary:         #0f172a;
  --bankx-bg-surface:         #1e293b;
  --bankx-text-primary:       #f1f5f9;
  --bankx-text-secondary:     #94a3b8;

  // Spacing scale
  --bankx-space-1: 4px;
  --bankx-space-2: 8px;
  --bankx-space-3: 12px;
  --bankx-space-4: 16px;
  --bankx-space-6: 24px;
  --bankx-space-8: 32px;

  // Typography
  --bankx-font-size-sm:   0.875rem;
  --bankx-font-size-base: 1rem;
  --bankx-font-size-lg:   1.125rem;
  --bankx-font-size-xl:   1.25rem;
  --bankx-font-size-2xl:  1.5rem;

  // Border radius
  --bankx-radius-sm:  6px;
  --bankx-radius-md:  10px;
  --bankx-radius-lg:  16px;
  --bankx-radius-xl:  24px;
}

// Light theme support
[data-theme="light"] {
  --bankx-bg-primary:    #f8fafc;
  --bankx-bg-surface:    #ffffff;
  --bankx-text-primary:  #0f172a;
  --bankx-text-secondary:#64748b;
}
```

---

## 5. Build và Publish Library

```bash
# Build library
ng build bankx-ui-kit --configuration production

# Output: dist/bankx-ui-kit/
# Bên trong có: FESM, UMD bundles, type definitions, package.json

# Dùng trong cùng workspace (không cần publish lên npm):
# tsconfig.json tự động config paths:
# "bankx-ui-kit": ["dist/bankx-ui-kit"]

# Import trong frontend-web:
import { AccountCardComponent } from 'bankx-ui-kit';
//                                   ↑ Import như npm package

# Publish lên npm (nếu muốn chia sẻ):
cd dist/bankx-ui-kit
npm publish --access public
```

---

## 6. Nx Workspace — Monorepo Nâng Cao

**Nx** là tool quản lý monorepo mạnh mẽ hơn Angular CLI workspace thuần túy.

```bash
# Tạo Nx workspace mới cho BankX Enterprise:
npx create-nx-workspace@latest bankx-enterprise --preset=angular-monorepo

# Thêm apps:
nx g @nx/angular:application frontend-web
nx g @nx/angular:application admin-portal

# Thêm libraries:
nx g @nx/angular:library bankx-ui-kit --directory=libs/shared
nx g @nx/angular:library bankx-data-access --directory=libs/data-access
nx g @nx/angular:library bankx-util --directory=libs/util

# Cấu trúc Nx monorepo:
bankx-enterprise/
├── apps/
│   ├── frontend-web/   ← Customer Portal
│   └── admin-portal/   ← Admin Portal
├── libs/
│   ├── shared/
│   │   └── bankx-ui-kit/    ← UI Components library
│   ├── data-access/
│   │   └── bankx-data-access/ ← Services, Models
│   └── util/
│       └── bankx-util/      ← Pipes, Validators, Helpers
└── nx.json
```

### Nx Commands:

```bash
# Build chỉ app đã thay đổi và dependencies của nó:
nx affected:build

# Test chỉ phần affected:
nx affected:test

# Lint chỉ phần affected:
nx affected:lint

# Xem dependency graph:
nx graph
# → Mở browser, thấy sơ đồ phụ thuộc giữa apps và libs

# Chạy app cụ thể:
nx serve frontend-web
nx serve admin-portal

# Build library:
nx build bankx-ui-kit
```

### Dependency Rules Trong Nx:

```json
// .eslintrc.json — Enforce dependency rules
{
  "rules": {
    "@nx/enforce-module-boundaries": ["error", {
      "depConstraints": [
        // apps chỉ được import từ libs, không được import chéo apps
        { "sourceTag": "type:app",    "onlyDependOnLibsWithTags": ["type:lib"] },
        // Shared UI lib không được import từ data-access
        { "sourceTag": "scope:shared","onlyDependOnLibsWithTags": ["scope:shared"] }
      ]
    }]
  }
}
```

---

## 7. Storybook — Document Component Library

```bash
# Thêm Storybook vào library
nx g @nx/storybook:configuration bankx-ui-kit
npx nx storybook bankx-ui-kit
```

```typescript
// account-card.stories.ts
import type { Meta, StoryObj } from '@storybook/angular';
import { AccountCardComponent } from './account-card.component';

const meta: Meta<AccountCardComponent> = {
  component: AccountCardComponent,
  title: 'BankX/AccountCard',
  tags: ['autodocs'],
};
export default meta;

type Story = StoryObj<AccountCardComponent>;

// Story: Default state
export const Default: Story = {
  args: {
    account: {
      id: 'acc-001',
      accountName: 'Tài khoản Thanh toán',
      accountNumber: '088880001',
      balance: 5000000,
      status: 'ACTIVE'
    }
  }
};

// Story: Frozen account
export const Frozen: Story = {
  args: {
    account: { ...Default.args!.account!, status: 'FROZEN' }
  }
};

// Story: Selected state
export const Selected: Story = {
  args: { ...Default.args, selected: true }
};

// Story: Compact variant
export const Compact: Story = {
  args: { ...Default.args, variant: 'compact' }
};
```

---

## 8. So Sánh Các Cách Tổ Chức

| Cách | Phù Hợp | Không Phù Hợp |
|---|---|---|
| Single Angular project | Dưới 5 features, 1 app | Multiple apps |
| Angular workspace + libs | 1-3 apps cùng workspace | Scale lớn, CI phức tạp |
| Nx monorepo | Enterprise, 5+ apps, nhiều teams | Project nhỏ, overhead cao |
| **BankX hiện tại** | Single project — phù hợp | - |

---

**← [Bài 16 — Angular CDK](./16_angular_cdk.md)** | **→ [Bài 18 — SSR & Angular Universal](./18_ssr_angular_universal.md)**
