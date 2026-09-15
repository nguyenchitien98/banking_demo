# Bài 09 — RxJS & HTTP Client

> **Mục tiêu:** Hiểu Observable, subscribe, pipe, các operators RxJS hay dùng nhất  
> **File thực tế:** `auth.service.ts`, `accounts.page.ts`, `error.interceptor.ts`

---

## 1. RxJS là Gì? Tại Sao Angular Dùng?

**RxJS (Reactive Extensions for JavaScript)** = Thư viện xử lý dữ liệu bất đồng bộ (async) theo luồng (stream).

```
Không có RxJS (Promise):
fetch('/api/accounts')
  .then(res => res.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));
// Chỉ xử lý được 1 giá trị → không cancel được → khó compose

Với RxJS (Observable):
this.http.get('/api/accounts').pipe(
  retry(3),               // Tự động thử lại 3 lần nếu lỗi
  timeout(5000),          // Timeout sau 5 giây
  catchError(e => of([])) // Trả về [] nếu lỗi
).subscribe(data => console.log(data));
// Có thể cancel, retry, timeout, compose, transform...
```

---

## 2. Observable — Luồng Dữ Liệu Async

```typescript
import { Observable, of, from, interval } from 'rxjs';

// ===== Observable là gì? =====
// Observable = "lời hứa" sẽ phát ra 0 hoặc nhiều giá trị theo thời gian

// HTTP Request → Observable phát ra 1 giá trị (response) rồi complete
const accounts$ = this.http.get('/api/accounts');
// accounts$ → Ký hiệu $ cuối tên biến = Observable (convention)

// interval → Observable phát ra số đếm mỗi X milliseconds (mãi mãi)
const timer$ = interval(1000); // 0, 1, 2, 3... mỗi giây


// ===== Observable vs Promise =====
// Promise: 1 giá trị, không cancel được, eager (chạy ngay khi tạo)
// Observable: 0+ giá trị, cancel được, lazy (chỉ chạy khi subscribe)

// Observable LAZY — không chạy gì cho đến khi subscribe:
const obs$ = this.http.get('/api/accounts');
// → Chưa gửi request nào!

obs$.subscribe(); // ← BÂY GIỜ mới gửi request
```

---

## 3. subscribe() — Nhận Dữ Liệu Từ Observable

```typescript
// ===== Cú pháp subscribe() =====
observable$.subscribe({
  next: (value) => {
    // ← Gọi khi Observable phát ra giá trị
    // value = dữ liệu nhận được
  },
  error: (err) => {
    // ← Gọi khi có lỗi (HTTP 4xx, 5xx, network error)
    // Observable dừng lại sau khi gặp lỗi
  },
  complete: () => {
    // ← Gọi khi Observable phát xong (không còn giá trị nữa)
    // HTTP request: complete ngay sau khi nhận response
    // interval: không bao giờ complete (mãi mãi phát)
  }
});


// ===== DÙNG TRONG BANKX: accounts.page.ts =====

loadAccounts(): void {
  this.loadingAccounts = true;

  this.accountService.getMyAccounts()
  // ↑ Trả về Observable<ApiResponse<BankAccount[]>>

  .subscribe({
    next: (res) => {
      // res = ApiResponse<BankAccount[]>
      this.loadingAccounts = false;

      if (res.code === 0 && res.data) {
        this.accounts.set(res.data);    // Cập nhật signal
        if (res.data.length > 0) {
          this.selectAccount(res.data[0]); // Tự động chọn tài khoản đầu tiên
        }
      }
    },
    error: () => {
      // Network error, backend down, timeout...
      this.loadingAccounts = false;
      // Fallback: dùng dữ liệu mock để demo vẫn chạy được
      this.accounts.set([/* mock data */]);
    }
    // complete: Không cần cho HTTP request (Angular tự xử lý)
  });
}
```

---

## 4. pipe() + Operators — Biến Đổi Observable

```typescript
import { tap, map, catchError, retry, timeout, finalize,
         switchMap, forkJoin, debounceTime, distinctUntilChanged,
         filter, take } from 'rxjs/operators';
import { of } from 'rxjs';

// pipe() = chuỗi các bước xử lý Observable
// observable$.pipe(op1, op2, op3).subscribe()


// ===== tap() — Nhìn vào giá trị mà không thay đổi =====
// Dùng cho: Side effects (lưu token, log, cập nhật signal)
this.http.post('/api/auth/login', credentials).pipe(
  tap((res) => {
    // Đọc response nhưng không đổi nó
    if (res.code === 0) {
      this.tokenService.setAccessToken(res.data.accessToken); // Side effect
      this.currentUser.set(res.data.user);                    // Side effect
    }
  })
).subscribe(/* ... */);


// ===== map() — Biến đổi giá trị phát ra =====
this.accountService.getMyAccounts().pipe(
  map(res => res.data) // ApiResponse<BankAccount[]> → BankAccount[]
).subscribe(accounts => {
  this.accounts.set(accounts); // Nhận thẳng BankAccount[] không qua wrapper
});


// ===== catchError() — Xử Lý Lỗi Trên Observable =====
this.accountService.getMyAccounts().pipe(
  catchError((err) => {
    console.error('Lỗi API:', err);
    return of([]); // of([]) = Observable phát ra mảng rỗng rồi complete
    // Thay vì để Observable terminate bởi lỗi → trả về giá trị mặc định
  })
).subscribe(accounts => {
  this.accounts.set(accounts); // Nhận [] nếu lỗi
});


// ===== retry() — Tự Động Thử Lại =====
this.http.get('/api/outbox').pipe(
  retry(3) // Nếu lỗi → thử lại tối đa 3 lần
).subscribe();


// ===== finalize() — Luôn Chạy Dù Thành Công Hay Lỗi =====
// Như finally trong try-catch
this.accountService.getMyAccounts().pipe(
  finalize(() => this.loadingAccounts = false) // Tắt loading dù OK hay lỗi
).subscribe({
  next: (res) => this.accounts.set(res.data),
  error: (err) => this.errorMessage = err.message
});


// ===== debounceTime() — Chờ User Ngừng Gõ =====
// Dùng cho search input: không gọi API mỗi chữ, chờ user ngừng gõ 300ms
searchControl.valueChanges.pipe(
  debounceTime(300),          // Chờ 300ms sau khi gõ xong
  distinctUntilChanged(),     // Bỏ qua nếu giá trị không đổi
  switchMap(query =>          // Hủy request cũ, tạo request mới
    this.search(query)
  )
).subscribe(results => this.searchResults.set(results));


// ===== forkJoin() — Chạy Song Song, Chờ Tất Cả =====
// Dùng trong accounts.page.ts — Giả lập Race Condition
const req1 = this.accountService.freezeAccount(acc.id);
const req2 = this.accountService.freezeAccount(acc.id);

forkJoin([req1, req2]).subscribe({
  next: ([res1, res2]) => {
    // Chỉ vào đây khi CẢ HAI request hoàn thành
    this.showToast('Cả 2 request xong!');
  },
  error: (err) => {
    // Nếu BẤT KỲ request nào lỗi → vào error
    this.showToast('Có lỗi xảy ra');
  }
});
// Giống Promise.all() nhưng cho Observable


// ===== switchMap() — Hủy Request Cũ, Tạo Request Mới =====
// Dùng cho: search, select dropdown → gọi API tiếp theo
this.selectedAccountId$.pipe(
  switchMap(id =>
    this.ledgerService.getAccountTransactions(id)
    // Khi selectedAccountId thay đổi → request cũ bị hủy
    // → Request mới với ID mới được tạo
  )
).subscribe(entries => this.entries.set(entries));


// ===== take() — Chỉ Nhận N Giá Trị Rồi Complete =====
interval(1000).pipe(
  take(5) // Nhận 0, 1, 2, 3, 4 rồi complete (không mãi mãi)
).subscribe(n => console.log(n));


// ===== filter() — Lọc Giá Trị =====
this.http.get<ApiResponse<Transfer[]>>('/api/transfers').pipe(
  map(res => res.data),
  map(transfers => transfers.filter(t => t.status === 'COMPLETED'))
  // Lọc chỉ lấy transfer COMPLETED
).subscribe();
```

---

## 5. HttpClient — Gọi REST API

```typescript
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

// inject(HttpClient) trong Service
private readonly http = inject(HttpClient);


// ===== GET =====
this.http.get<ApiResponse<BankAccount[]>>('/api/v1/accounts');
// Generic <T>: TypeScript type hint cho response
// Trả về Observable<ApiResponse<BankAccount[]>>


// ===== GET với Query Params =====
// URL: /api/v1/transfers?page=1&size=20&status=COMPLETED
const params = new HttpParams()
  .set('page', '1')
  .set('size', '20')
  .set('status', 'COMPLETED');

this.http.get('/api/v1/transfers', { params });


// ===== POST =====
const body = {
  debitAccountId: 'acc-001',
  creditAccountId: 'acc-002',
  amount: 1000000
};
this.http.post<ApiResponse<Transfer>>('/api/v1/transfers/internal', body);


// ===== PATCH =====
this.http.patch(`/api/v1/accounts/${accountId}/freeze`, {});
// PATCH = Cập nhật một phần (khác PUT = thay toàn bộ)


// ===== PUT =====
this.http.put(`/api/v1/customers/me/profile`, updateData);


// ===== DELETE =====
this.http.delete(`/api/v1/cards/${cardId}`);


// ===== Custom Headers =====
const headers = new HttpHeaders({
  'X-Idempotency-Key': uuid,
  'Content-Type': 'application/json'
});
this.http.post('/api/v1/transfers/internal', body, { headers });
```

---

## 6. Unsubscribe — Tránh Memory Leak

```typescript
import { Subscription } from 'rxjs';

export class EngineeringPortalPage implements OnInit, OnDestroy {

  // Lưu subscription để có thể unsubscribe
  private autoRefreshSub: Subscription | null = null;

  ngOnInit(): void {
    // interval() phát mãi mãi — nếu không unsubscribe → memory leak
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.fetchHealthSummary();
    });
  }

  ngOnDestroy(): void {
    // Component bị destroy → PHẢI unsubscribe
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }
}


// ===== Cách hiện đại: takeUntilDestroyed (Angular 16+) =====
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export class NewComponent {
  constructor() {
    // KHÔNG cần implements OnDestroy
    // KHÔNG cần lưu subscription
    interval(3000).pipe(
      takeUntilDestroyed() // Tự động unsubscribe khi component destroy
    ).subscribe(() => {
      this.fetchData();
    });
  }
}


// ===== HTTP requests thì KHÔNG cần unsubscribe =====
// HttpClient Observable tự complete sau 1 response
// → Không cần lưu subscription cho HTTP calls
this.http.get('/api/accounts').subscribe(/* không cần unsubscribe */);
```

---

## 7. Async Pipe — Alternative Không Cần subscribe()

```typescript
// Thay vì:
export class AccountsPage {
  accounts: BankAccount[] = [];

  ngOnInit() {
    this.accountService.getMyAccounts().subscribe(
      res => this.accounts = res.data
    );
  }
}
// Template: {{ accounts.length }}


// Dùng async pipe — Angular tự subscribe và unsubscribe:
export class AccountsPage {
  accounts$ = this.accountService.getMyAccounts(); // Chỉ khai báo, không subscribe
}
// Template: {{ (accounts$ | async)?.data?.length }}
// async pipe tự subscribe → nhận giá trị → tự unsubscribe khi destroy
```

---

## 8. Error Interceptor — Xử Lý Lỗi Tập Trung

```typescript
// File: error.interceptor.ts

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Xử lý các lỗi HTTP phổ biến tại 1 chỗ
      switch (error.status) {
        case 401: // Unauthorized — Token hết hạn
          // Redirect đến login (inject router và clear tokens)
          console.warn('Session hết hạn — yêu cầu đăng nhập lại');
          break;

        case 403: // Forbidden — Không đủ quyền
          console.error('Không có quyền truy cập tài nguyên này');
          break;

        case 500: // Server Error
          console.error('Lỗi server nội bộ — vui lòng thử lại sau');
          break;

        case 0: // Network Error (không kết nối được)
          console.error('Không thể kết nối đến server — kiểm tra mạng');
          break;
      }

      // throwError: Ném lỗi để component xử lý tiếp trong .subscribe({ error: ... })
      return throwError(() => error);
    })
  );
};
```

---

## Tổng Kết RxJS Operators

| Operator | Tác Dụng | Ví Dụ BankX |
|---|---|---|
| `tap()` | Side effect, không đổi giá trị | Lưu token sau login |
| `map()` | Biến đổi giá trị | `res → res.data` |
| `catchError()` | Bắt lỗi, fallback | Trả về mock data |
| `retry(n)` | Thử lại n lần khi lỗi | API không ổn định |
| `finalize()` | Luôn chạy cuối | Tắt loading spinner |
| `debounceTime()` | Chờ ngừng emit N ms | Search input |
| `distinctUntilChanged()` | Bỏ qua giá trị trùng | Search query |
| `switchMap()` | Hủy cũ, tạo mới | Select account → load ledger |
| `forkJoin()` | Song song, chờ tất cả | Race condition test |
| `take(n)` | Chỉ nhận N giá trị | Auto-stop polling |
| `filter()` | Lọc giá trị | Chỉ lấy COMPLETED transfers |
| `interval(ms)` | Emit mỗi N ms | Auto-refresh Engineering Portal |

---

**← [Bài 08 — Signals & State](./08_signals_va_state_management.md)** | **→ [Bài 10 — Forms, Input/Output & Component Communication](./10_forms_input_output_communication.md)**
