# Bài 15 — Performance Optimization (OnPush, Virtual Scroll, Deferrable Views)

> **Cấp độ:** Senior  
> **Mục tiêu:** Tối ưu Angular app để chạy mượt khi có 10.000+ items, nhiều components  
> **Liên quan BankX:** Transaction history list, Engineering Portal metrics grid

---

## 1. Change Detection — Angular Biết Khi Nào Re-render?

```
Angular có 2 chiến lược Change Detection:

Default (Zone.js):
  Sau mỗi async event (click, HTTP, timeout, setInterval)
  → Angular kiểm tra TOÀN BỘ component tree từ gốc xuống
  → Tốn CPU khi tree lớn

OnPush:
  Chỉ check component này khi:
  1. @Input() reference thay đổi (object mới, không phải sửa nội dung)
  2. Event được trigger TRONG component này
  3. Observable với async pipe emit giá trị mới
  4. Signal thay đổi
  5. Gọi markForCheck() thủ công
  → Nhanh hơn đáng kể vì bỏ qua nhiều check không cần thiết
```

---

## 2. ChangeDetectionStrategy.OnPush

```typescript
import { Component, ChangeDetectionStrategy, Input, signal } from '@angular/core';

// ===== COMPONENT KHÔNG DÙNG OnPush (mặc định) =====
@Component({
  selector: 'app-account-card-slow',
  template: `<div>{{ account.balance | number }}</div>`,
})
export class AccountCardSlow {
  @Input() account!: BankAccount;
  // Re-render mỗi khi BẤT KỲ component nào trong app thay đổi
  // Dù account này không đổi → vẫn bị check
}


// ===== COMPONENT DÙNG OnPush (tối ưu) =====
@Component({
  selector: 'app-account-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  //               ↑ CHỈ re-render khi @Input reference đổi hoặc event nội bộ
  template: `
    <div class="card">
      <h4>{{ account().accountName }}</h4>
      <span>{{ account().balance | number:'1.0-0' }} VND</span>
      <span [class.active]="account().status === 'ACTIVE'">
        {{ account().status }}
      </span>
    </div>
  `
})
export class AccountCardComponent {
  @Input({ required: true }) account!: BankAccount;
  // Dùng với Signals thì còn tốt hơn — Signal luôn trigger đúng lúc
}


// ===== BEST PRACTICE: Dùng OnPush + Signals cho component dumb (presentational) =====
@Component({
  selector: 'app-transaction-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, DatePipe],
  template: `
    <tr>
      <td [class]="entry.entryType.toLowerCase()">{{ entry.entryType }}</td>
      <td>{{ entry.amount | number:'1.0-0' }} VND</td>
      <td>{{ entry.createdAt | date:'dd/MM HH:mm' }}</td>
    </tr>
  `
})
export class TransactionRowComponent {
  // Chỉ nhận @Input, không có logic phức tạp
  @Input({ required: true }) entry!: LedgerEntry;
}


// ===== SMART vs DUMB Components Pattern =====
// Smart (Container): Lấy data từ Service/Store, không dùng OnPush nhiều
// Dumb (Presentational): Chỉ nhận @Input, hiển thị, dùng OnPush luôn
```

---

## 3. trackBy — Tối Ưu @for / *ngFor

```html
<!-- ❌ Không có track — mỗi lần list thay đổi, Angular xóa và tạo lại TẤT CẢ DOM nodes -->
@for (acc of accounts(); track $index) {
  <app-account-card [account]="acc" />
}

<!-- ✅ Dùng track acc.id — Angular chỉ update PHẦN THAY ĐỔI -->
@for (acc of accounts(); track acc.id) {
  <app-account-card [account]="acc" />
}
<!-- Nếu chỉ 1 account thay đổi → chỉ 1 DOM node được update
     Không phải xóa toàn bộ và tạo lại -->
```

```typescript
// Với *ngFor cú pháp cũ (vẫn dùng được):
// <div *ngFor="let acc of accounts; trackBy: trackByAccount">
// trackByAccount(index: number, acc: BankAccount): string { return acc.id; }
```

---

## 4. Virtual Scrolling — 100.000 Items Không Lag

```typescript
// Vấn đề: Render 10.000 transaction rows → 10.000 DOM nodes → lag nghiêm trọng
// Giải pháp: Virtual Scroll — chỉ render rows ĐANG NHÌN THẤY

// Cài đặt:
// npm install @angular/cdk

import { ScrollingModule } from '@angular/cdk/scrolling';

@Component({
  standalone: true,
  imports: [ScrollingModule, DecimalPipe, DatePipe],
  template: `
    <!-- cdk-virtual-scroll-viewport: Container ảo với chiều cao cố định -->
    <cdk-virtual-scroll-viewport itemSize="60" style="height: 400px; overflow: auto">
    <!--                          ↑ Mỗi item cao 60px — cần biết để tính toán -->

      <!-- *cdkVirtualFor thay thế @for — chỉ render items trong viewport -->
      <div *cdkVirtualFor="let entry of allEntries; trackBy: trackById"
           class="transaction-row">
        <span [class]="entry.entryType.toLowerCase()">{{ entry.entryType }}</span>
        <span>{{ entry.amount | number:'1.0-0' }} VND</span>
        <span>{{ entry.createdAt | date:'HH:mm dd/MM' }}</span>
      </div>
    </cdk-virtual-scroll-viewport>
  `
})
export class TransactionHistoryPage {
  allEntries = signal<LedgerEntry[]>([]);
  // Có thể có 100.000 entries — Virtual Scroll chỉ render ~10 cái đang nhìn thấy

  trackById(index: number, entry: LedgerEntry): string { return entry.id; }
}
```

---

## 5. Deferrable Views `@defer` — Lazy Load Component Con

```html
<!-- Angular 17+ — Lazy load component con khi cần (không cần router) -->

<!-- ===== @defer cơ bản — Load khi trình duyệt rảnh =====  -->
@defer {
  <!-- Component nặng — chỉ load khi browser idle -->
  <app-engineering-portal />
}


<!-- ===== @defer (on viewport) — Load khi scroll đến =====  -->
@defer (on viewport) {
  <app-performance-chart />
  <!-- Biểu đồ nặng — chỉ load khi user scroll đến vị trí này -->
} @placeholder {
  <div class="chart-skeleton">Đang tải biểu đồ...</div>
  <!-- Hiện placeholder trong khi chờ -->
} @loading (minimum 200ms) {
  <div class="spinner">Loading...</div>
  <!-- Hiện loading indicator (ít nhất 200ms để tránh flash) -->
} @error {
  <div class="error">Không tải được biểu đồ</div>
  <!-- Hiện nếu có lỗi -->
}


<!-- ===== @defer (on interaction) — Load khi user hover/click =====  -->
@defer (on interaction) {
  <app-fraud-detail-modal />
} @placeholder {
  <button>Xem chi tiết Fraud</button>
}


<!-- ===== @defer (when condition) — Load khi điều kiện thoả =====  -->
@defer (when isAdminUser()) {
  <app-admin-panel />
  <!-- Chỉ load Admin Panel code nếu user là admin -->
}
```

---

## 6. Pure Pipes — Tránh Tính Toán Lại Không Cần Thiết

```typescript
// Pipe mặc định là PURE: Chỉ tính lại khi INPUT THAY ĐỔI

@Pipe({
  name: 'formatVnd',
  standalone: true,
  pure: true  // Mặc định là true — không cần viết
})
export class FormatVndPipe implements PipeTransform {
  transform(value: number): string {
    if (!value && value !== 0) return '0 VND';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0
    }).format(value);
  }
}
// Pure pipe: 5000000 → "5.000.000 ₫"
// Nếu giá trị không đổi → Angular không gọi transform() lại


// IMPURE PIPE (pure: false) — Gọi lại mỗi change detection
// Ít dùng vì tốn performance:
@Pipe({ name: 'currentTime', pure: false })
export class CurrentTimePipe implements PipeTransform {
  transform(): string {
    return new Date().toLocaleTimeString('vi-VN'); // Luôn tính lại
  }
}
```

---

## 7. Lazy Loading Routes — Đã Học Ở Bài 07, Recap Nhanh

```typescript
// Mỗi feature module chỉ tải khi cần — BankX đã làm đúng
{
  path: 'engineering',
  loadChildren: () => import('./features/monitoring/monitoring.routes')
    .then(m => m.MONITORING_ROUTES)
}
// Engineering Portal (52 KB) → Chỉ tải khi user vào /monitoring/engineering
```

---

## 8. preloadingStrategy — Preload Thông Minh

```typescript
// PreloadAllModules: Tải tất cả lazy modules ngầm sau khi app load xong
import { PreloadAllModules } from '@angular/router';

provideRouter(routes, withPreloading(PreloadAllModules));
// App load xong → Trình duyệt bắt đầu tải ngầm tất cả lazy chunks
// → User click vào feature → Đã có sẵn trong cache, load tức thì

// Custom preloading — Chỉ preload route quan trọng:
@Injectable({ providedIn: 'root' })
export class SelectivePreloadStrategy implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<any>): Observable<any> {
    // Chỉ preload routes được đánh dấu data.preload: true
    return route.data?.['preload'] ? load() : EMPTY;
  }
}

// Trong routes:
{ path: 'transfers', loadChildren: ..., data: { preload: true } }
// ↑ Preload Transfers ngay (quan trọng nhất)
{ path: 'admin',     loadChildren: ..., data: { preload: false } }
// ↑ Không preload Admin (ít dùng)
```

---

## 9. Bundle Analysis — Tìm Điểm Bottleneck

```bash
# Cài bundle analyzer
npm install -D webpack-bundle-analyzer

# Build với stats
ng build --stats-json

# Analyze
npx webpack-bundle-analyzer dist/frontend-web/browser/stats.json
# → Mở browser, xem treemap của tất cả packages
# → Tìm package nào quá to để tối ưu
```

**Kết quả build BankX thực tế:**

```
Initial chunk files:
  chunk-FZQWFFO3.js   1.18 MB  ← Angular core (không thể giảm nhiều)
  main.js            53.20 kB  ← App code (quan trọng: phải nhỏ)
  styles.css          5.07 kB

Lazy chunks (tổng):
  ~450 KB tất cả lazy chunks
  → User chỉ tải chunk cần thiết
```

---

## 10. Memoization — Cache Kết Quả Hàm Tính Toán Nặng

```typescript
// Khi một hàm tính toán nặng nhưng input ít thay đổi
import { memoize } from 'lodash-es';

@Injectable({ providedIn: 'root' })
export class FraudAnalysisService {

  // Memoize: Cache kết quả theo input key
  calculateRiskScore = memoize(
    (transaction: Transfer): number => {
      // Tính toán nặng — nhiều rules
      let score = 0;
      if (transaction.amount > 20_000_000) score += 40;
      if (this.isNightTime(transaction.createdAt)) score += 15;
      if (this.isNewDevice(transaction.deviceId)) score += 50;
      // ...nhiều rules khác
      return Math.min(100, score);
    },
    (transaction) => transaction.id // Key function — cache theo ID
  );
  // Cùng transaction ID → trả kết quả từ cache, không tính lại
}
```

---

## 11. Performance Checklist BankX

```
✅ Đã làm:
  [x] Lazy loading routes (tất cả features)
  [x] track trong @for loops
  [x] Signal thay vì property thường cho reactive data
  [x] Java 21 Virtual Threads (Backend)

🔧 Nên thêm nếu cần:
  [ ] OnPush ChangeDetection cho Dumb Components
  [ ] Virtual Scroll cho Transaction History (nếu > 1000 rows)
  [ ] @defer cho Engineering Portal charts
  [ ] shareReplay(1) cho API calls thường dùng
  [ ] Bundle analysis định kỳ

⚠️ Chỉ khi thực sự cần:
  [ ] Web Workers (bài tiếp theo)
  [ ] SSR với Angular Universal
```

---

## Tổng Kết

| Kỹ Thuật | Vấn Đề Giải Quyết | Khi Nào Dùng |
|---|---|---|
| `OnPush` | Tránh check không cần thiết | Mọi Dumb/Presentational component |
| `track acc.id` | Tránh tạo lại DOM không cần | Mọi @for loop |
| `Virtual Scroll` | 10.000+ items không lag | Transaction history lớn |
| `@defer` | Lazy load component con | Feature nặng, ít dùng |
| `Pure Pipe` | Cache pipe transform | Mọi custom pipe |
| `shareReplay(1)` | Cache HTTP response | API gọi nhiều lần |
| `PreloadAllModules` | Load lazy modules ngầm | Sau app đã load |
| Bundle analysis | Tìm packages thừa | Định kỳ khi app nặng |

---

**← [Bài 14 — RxJS Nâng Cao](./14_rxjs_nang_cao.md)** | **→ [Bài 16 — Angular CDK](./16_angular_cdk.md)**
