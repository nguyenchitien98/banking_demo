# Bài 14 — RxJS Nâng Cao

> **Cấp độ:** Trung → Senior  
> **Mục tiêu:** Thành thạo các operators phức tạp, Subject, multicasting, error handling patterns  
> **Liên quan BankX:** WebSocket trong Engineering Portal, Auto-refresh, Concurrency control

---

## 1. Subject — Observable Và Observer Cùng Lúc

```typescript
import { Subject, BehaviorSubject, ReplaySubject } from 'rxjs';

// ===== Subject — "Bộ phát/thu" thủ công =====
const subject = new Subject<string>();

// Subscribe TRƯỚC rồi mới nhận được:
subject.subscribe(val => console.log('A nhận:', val));
subject.subscribe(val => console.log('B nhận:', val));

subject.next('Xin chào'); // → A và B cùng nhận
subject.next('Chuyển tiền');
subject.complete(); // Kết thúc

// Đặc điểm Subject:
// - Subscriber đến SAU KHÔNG nhận giá trị cũ
// - Multicasting: nhiều subscriber nhận cùng giá trị


// ===== BehaviorSubject — Có giá trị ban đầu, subscriber mới nhận ngay =====
const currentUser$ = new BehaviorSubject<UserSummary | null>(null);
// ↑ Giá trị ban đầu = null

// Component A subscribe:
currentUser$.subscribe(user => {
  console.log('A nhận user:', user); // Nhận null ngay lập tức
});

// Auth Service cập nhật:
currentUser$.next({ id: '1', username: 'user01', email: '...', roles: [] });

// Component B subscribe SAU:
currentUser$.subscribe(user => {
  console.log('B nhận user:', user); // Nhận NGAY giá trị hiện tại (user01)
});

// Lấy giá trị hiện tại ngay lập tức (không cần subscribe):
const currentValue = currentUser$.getValue();
// ↑ Chỉ BehaviorSubject có .getValue()

// BankX pattern — BehaviorSubject trong Service:
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private unreadCount$ = new BehaviorSubject<number>(0);
  // Expose như Observable (không cho ngoài .next())
  readonly unreadCount = this.unreadCount$.asObservable();

  markAsRead(id: string): void {
    this.http.patch(`.../${id}/read`, {}).subscribe(() => {
      this.unreadCount$.next(Math.max(0, this.unreadCount$.getValue() - 1));
    });
  }
}


// ===== ReplaySubject — Phát lại N giá trị cuối cho subscriber mới =====
const history$ = new ReplaySubject<Transfer>(5); // Buffer 5 items

history$.next(transfer1);
history$.next(transfer2);
history$.next(transfer3);

// Subscriber mới vào → nhận ngay 5 giá trị gần nhất:
history$.subscribe(t => console.log('Lịch sử:', t.id));
// → Nhận transfer1, transfer2, transfer3 ngay lập tức


// ===== AsyncSubject — Chỉ phát giá trị CUỐI KHI complete =====
const oneTimeResult$ = new AsyncSubject<string>();
oneTimeResult$.next('Giá trị 1');
oneTimeResult$.next('Giá trị 2');
oneTimeResult$.next('Giá trị 3 — cuối cùng');
oneTimeResult$.complete(); // → Tất cả subscriber nhận "Giá trị 3"
```

---

## 2. Operators Kết Hợp Nhiều Observable

```typescript
import { combineLatest, merge, zip, race, concat } from 'rxjs';

// ===== combineLatest — Kết hợp, phát khi BẤT KỲ observable nào phát =====
// Phát tuple [val1, val2, ...] với giá trị mới nhất của MỖI observable

const accounts$ = this.accountService.getMyAccounts().pipe(map(r => r.data));
const transfers$ = this.transferService.getTransfers().pipe(map(r => r.data));
const notifications$ = this.notifService.getNotifications().pipe(map(r => r.data));

// Dashboard cần cả 3 — tải song song, load tuần tự khi có data:
combineLatest([accounts$, transfers$, notifications$]).subscribe(
  ([accounts, transfers, notifications]) => {
    this.accounts.set(accounts);
    this.transfers.set(transfers);
    this.notifications.set(notifications);
  }
);

// Khác forkJoin:
// forkJoin → chờ TẤT CẢ complete rồi mới emit 1 lần
// combineLatest → emit mỗi khi BẤT KỲ observable nào phát giá trị mới
//                 (dùng khi observables phát nhiều lần như BehaviorSubject)


// ===== withLatestFrom — Kết hợp với giá trị mới nhất của observable khác =====
// Khác combineLatest: chỉ emit khi source (cái đầu) phát, lấy latest từ cái còn lại

// Ví dụ: Khi user click "transfer" → lấy thêm selectedAccount
this.transferClick$.pipe(
  withLatestFrom(this.selectedAccount$),
  map(([clickEvent, account]) => ({
    fromAccount: account.id,
    ...clickEvent
  }))
).subscribe(transferData => this.executeTransfer(transferData));


// ===== merge — Gộp nhiều observables, phát khi bất kỳ cái nào phát =====
const allNotifications$ = merge(
  this.emailNotifications$,    // Phát khi có email mới
  this.pushNotifications$,     // Phát khi có push notification
  this.smsNotifications$       // Phát khi có SMS
);
// Khi bất kỳ source nào phát → subscriber nhận ngay


// ===== zip — Kết hợp theo cặp (phải đợi tất cả cùng phát tại index đó) =====
zip(obs1$, obs2$, obs3$).subscribe(
  ([val1, val2, val3]) => {
    // Nhận (obs1 item 0, obs2 item 0, obs3 item 0)
    // Rồi (obs1 item 1, obs2 item 1, obs3 item 1)
    // Ít dùng hơn combineLatest
  }
);
```

---

## 3. Higher-Order Operators — Flatten Nested Observables

```typescript
import { switchMap, mergeMap, concatMap, exhaustMap } from 'rxjs/operators';

// Tình huống: Observable phát ra ID → cần gọi API với ID đó
// → Observable of Observables → cần "flatten"

const accountId$ = this.selectedId$.pipe(/* ... */);

// ===== switchMap — Hủy request cũ, chỉ giữ request MỚI NHẤT =====
// Dùng khi: Search, autocomplete, select dropdown → API call
accountId$.pipe(
  switchMap(id => this.accountService.getAccountDetail(id))
  // Nếu user chọn account A → gọi API A
  // Trước khi A xong → user chọn B → request A BỊ HỦY → gọi API B
).subscribe(detail => this.accountDetail.set(detail));


// ===== mergeMap (flatMap) — Chạy tất cả song song =====
// Dùng khi: Nhiều independent requests, thứ tự KHÔNG quan trọng
const accountIds = ['acc-1', 'acc-2', 'acc-3'];
from(accountIds).pipe(
  mergeMap(id => this.accountService.getBalance(id))
  // Gọi cả 3 API song song, kết quả về theo thứ tự nào đó về trước
).subscribe(balance => this.balances.push(balance));


// ===== concatMap — Tuần tự, đợi xong mới làm tiếp =====
// Dùng khi: Thứ tự QUAN TRỌNG, không được bỏ qua
const transferRequests = [transfer1, transfer2, transfer3];
from(transferRequests).pipe(
  concatMap(req => this.transferService.executeTransfer(req))
  // Transfer 1 xong → Transfer 2 → Transfer 3 (theo thứ tự)
).subscribe(result => this.results.push(result));


// ===== exhaustMap — Bỏ qua event mới khi đang xử lý =====
// Dùng khi: Login button — không được submit 2 lần
this.loginButtonClicks$.pipe(
  exhaustMap(() => this.authService.login(credentials))
  // Nếu đang xử lý login → click thêm → BỊ BỎ QUA
  // Tự động chống double-submit
).subscribe(/* ... */);
```

---

## 4. Error Handling Nâng Cao

```typescript
import { catchError, retry, retryWhen, delay, tap, throwError, EMPTY } from 'rxjs';

// ===== retryWhen — Retry có logic phức tạp =====
this.http.get('/api/accounts').pipe(
  retryWhen(errors =>
    errors.pipe(
      // Chờ 1 giây, thử lại tối đa 3 lần
      scan((retryCount, error) => {
        if (retryCount >= 3) throw error; // Quá 3 lần → ném lỗi ra ngoài
        return retryCount + 1;
      }, 0),
      delay(1000) // Chờ 1000ms trước khi retry
    )
  )
);

// ===== retry với exponential backoff =====
import { timer } from 'rxjs';

function exponentialBackoffRetry(maxRetries: number) {
  return retryWhen(errors =>
    errors.pipe(
      scan((retries, error) => {
        if (retries >= maxRetries) throw error;
        return retries + 1;
      }, 0),
      switchMap(retries => timer(Math.pow(2, retries) * 1000))
      // Retry 1: chờ 2s, Retry 2: chờ 4s, Retry 3: chờ 8s
    )
  );
}

// Dùng:
this.transferService.executeTransfer(data).pipe(
  exponentialBackoffRetry(3)
).subscribe(/* ... */);


// ===== catchError — Fallback strategies =====

// Strategy 1: Trả về giá trị mặc định
.pipe(
  catchError(() => of([]))  // Trả về mảng rỗng khi lỗi
)

// Strategy 2: Trả về EMPTY (không phát giá trị, complete ngay)
.pipe(
  catchError(() => EMPTY)  // Observable hoàn thành, không phát gì
)

// Strategy 3: Log rồi re-throw
.pipe(
  catchError(err => {
    console.error('API Error:', err);
    this.errorService.report(err); // Báo cáo lỗi
    return throwError(() => err);   // Re-throw để component handle tiếp
  })
)

// Strategy 4: Materialize/dematerialize — wrap kết quả
import { materialize, dematerialize } from 'rxjs/operators';
// → Chuyển error thành notification object để xử lý uniform
```

---

## 5. Custom RxJS Operators

```typescript
// Tạo operator tái sử dụng cho BankX

import { pipe, OperatorFunction } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';

// Operator: Tự động unwrap ApiResponse<T>
function unwrapApiResponse<T>(): OperatorFunction<ApiResponse<T>, T> {
  return pipe(
    map(response => {
      if (response.code !== 0) {
        throw new Error(response.message);
      }
      return response.data;
    })
  );
}

// Operator: Log với prefix
function logWith<T>(prefix: string): OperatorFunction<T, T> {
  return tap(value => console.log(`[${prefix}]`, value));
}

// Operator: Retry với toast notification
function retryWithNotification<T>(
  maxRetries: number,
  toastService: ToastService
): OperatorFunction<T, T> {
  return pipe(
    retryWhen(errors =>
      errors.pipe(
        scan((count, err) => {
          if (count >= maxRetries) throw err;
          toastService.show(`Đang thử lại... (${count + 1}/${maxRetries})`);
          return count + 1;
        }, 0),
        delay(2000)
      )
    )
  );
}


// Dùng custom operators:
this.accountService.getMyAccounts().pipe(
  unwrapApiResponse(),                       // Tự động lấy .data
  logWith('AccountService'),                 // Log response
  retryWithNotification(3, this.toast),      // Retry với toast
).subscribe(accounts => {
  this.accounts.set(accounts);  // accounts là BankAccount[] trực tiếp
});
```

---

## 6. Multicasting — share, shareReplay

```typescript
import { share, shareReplay } from 'rxjs/operators';

// VẤN ĐỀ: Observable lạnh (cold) tạo execution mới mỗi subscribe
const accounts$ = this.http.get('/api/accounts');

// Nếu 3 components cùng subscribe → 3 HTTP requests!
accounts$.subscribe(res => componentA.setData(res));
accounts$.subscribe(res => componentB.setData(res));
accounts$.subscribe(res => componentC.setData(res));
// → 3 request giống hệt nhau!


// GIẢI PHÁP 1: share() — Multicasting, 1 request cho tất cả
const shared$ = accounts$.pipe(share());
// share() = publish().refCount() — tự manage subscriptions
shared$.subscribe(res => componentA.setData(res));
shared$.subscribe(res => componentB.setData(res));
// → 1 HTTP request, cả 2 component nhận cùng kết quả


// GIẢI PHÁP 2: shareReplay(n) — Multicasting + cache N giá trị cuối
const cached$ = this.accountService.getMyAccounts().pipe(
  shareReplay(1)  // Cache 1 giá trị cuối cùng
);
// ↑ Subscriber mới vào sau vẫn nhận giá trị đã cache

// Ứng dụng trong BankX — Cache account list 30 giây:
@Injectable({ providedIn: 'root' })
export class AccountCacheService {
  private readonly cache$ = this.accountService.getMyAccounts().pipe(
    shareReplay({ bufferSize: 1, refCount: true, windowTime: 30000 })
    //                                                        ↑ Cache 30 giây
  );

  getAccounts(): Observable<ApiResponse<BankAccount[]>> {
    return this.cache$; // Mọi subscriber dùng chung 1 request + cache
  }
}
```

---

## 7. Scheduler — Kiểm Soát Thời Gian Thực Thi

```typescript
import { observeOn, subscribeOn, asyncScheduler, animationFrameScheduler } from 'rxjs';

// asyncScheduler: Chạy async (setTimeout 0) — tránh block main thread
heavyObservable$.pipe(
  observeOn(asyncScheduler) // Emit value trên async scheduler
).subscribe(/* ... */);

// animationFrameScheduler: Sync với browser's requestAnimationFrame
// Dùng cho smooth animation updates
balanceUpdates$.pipe(
  observeOn(animationFrameScheduler) // Update DOM trong frame tiếp theo
).subscribe(balance => {
  this.animateBalanceChange(balance);
});
```

---

## 8. WebSocket với RxJS — BankX Engineering Portal

```typescript
// engineering-portal.page.ts — Real-time updates

import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retryWhen, delay } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class MetricsWebSocketService {
  private socket$: WebSocketSubject<any> | null = null;

  connect(): Observable<SystemMetrics> {
    if (!this.socket$ || this.socket$.closed) {
      this.socket$ = webSocket('ws://localhost:8081/ws/metrics');
    }

    return this.socket$.pipe(
      retryWhen(errors =>
        errors.pipe(
          tap(() => console.warn('WS bị mất kết nối, đang thử lại...')),
          delay(3000) // Thử lại sau 3 giây
        )
      )
    );
  }

  sendCommand(command: string): void {
    this.socket$?.next({ type: 'COMMAND', payload: command });
  }

  disconnect(): void {
    this.socket$?.complete();
  }
}

// Component dùng:
export class EngineeringPortalPage implements OnInit, OnDestroy {
  private readonly wsService = inject(MetricsWebSocketService);
  private readonly subscription?: Subscription;

  ngOnInit(): void {
    this.subscription = this.wsService.connect().pipe(
      takeUntilDestroyed()
    ).subscribe({
      next: (metrics) => this.updateMetrics(metrics),
      error: (err) => console.error('WS Error:', err)
    });
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }
}
```

---

## Tổng Kết

| Operator | Khi Nào Dùng | Ví Dụ |
|---|---|---|
| `switchMap` | Search, dropdown selection | Chọn account → load ledger |
| `mergeMap` | Parallel independent requests | Bulk balance check |
| `concatMap` | Sequential, order matters | Ordered transfers |
| `exhaustMap` | Prevent double-submit | Login button |
| `combineLatest` | Multiple reactive sources | Dashboard load |
| `withLatestFrom` | Take snapshot of another stream | Transfer + account |
| `BehaviorSubject` | Shared state with initial value | unreadCount, currentUser |
| `shareReplay(1)` | Cache HTTP response | Account list cache |
| `retryWhen` | Complex retry logic | Exponential backoff |
| `webSocket` | Real-time updates | Engineering Portal |

---

**← [Bài 13 — NgRx](./13_ngrx_state_management.md)** | **→ [Bài 15 — Performance Optimization](./15_performance_optimization.md)**
