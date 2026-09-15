# Bài 13 — NgRx State Management (Redux Pattern)

> **Cấp độ:** Trung → Senior  
> **Mục tiêu:** Hiểu Store, Action, Reducer, Selector, Effect — khi nào cần NgRx  
> **So sánh:** NgRx vs Signals (BankX dùng Signals — bài này giúp hiểu khi nào cần chuyển)

---

## 1. Tại Sao Cần NgRx?

```
Vấn đề khi app lớn với Signals:

Component A (Dashboard)   → đọc accounts signal từ AccountService
Component B (Transfers)   → đọc accounts signal từ AccountService
Component C (Admin)       → đọc accounts signal từ AccountService

→ Tất cả cùng read 1 service → OK với Signals

Nhưng khi có:
- 50+ Services, 100+ Components
- Nhiều team làm song song
- Cần "time-travel" debugging (xem lịch sử state)
- Cần audit trail từng action thay đổi state
- State phức tạp: nhiều level, nhiều actions cùng 1 state

→ NgRx (Redux pattern) phù hợp hơn
```

**BankX hiện tại (Signals) vs NgRx:**

| | BankX (Signals) | NgRx |
|---|---|---|
| Code lượng | Ít | Nhiều boilerplate |
| Học dễ | ✅ | ❌ Learning curve cao |
| Debug | Khó trace | ✅ Redux DevTools |
| Scale | OK cho 10-20 features | Tốt cho 50+ features |
| Performance | Tốt | Tốt (với memoized selectors) |
| Kết luận | **BankX OK với Signals** | Dùng khi thực sự cần |

---

## 2. Cài Đặt NgRx

```bash
ng add @ngrx/store@latest
ng add @ngrx/effects@latest     # Cho async actions (HTTP calls)
ng add @ngrx/entity@latest      # Cho collections (danh sách entities)
ng add @ngrx/store-devtools@latest # Redux DevTools browser extension
```

---

## 3. Các Khái Niệm Cốt Lõi

```
                    ┌─────────────────┐
                    │   NgRx STORE    │ ← Single source of truth (1 object chứa toàn bộ state)
                    └────────┬────────┘
                             │ Selector đọc state
                             ▼
                    ┌─────────────────┐
  User Action  →   │   Component     │ → dispatch(action) → Store
                    └─────────────────┘
                             ↑
                             │ Effect thực hiện HTTP call
                    ┌─────────────────┐
                    │    Effects      │ → API call → dispatch Success/Failure action
                    └─────────────────┘
                             ↑
                    ┌─────────────────┐
                    │    Reducer      │ ← Nhận Action + State cũ → trả State mới
                    └─────────────────┘
```

---

## 4. Actions — Định Nghĩa "Những Gì Xảy Ra"

```typescript
// File: store/accounts/accounts.actions.ts

import { createAction, props } from '@ngrx/store';
import { BankAccount } from '../../core/models/account.model';

// ===== Naming convention: '[Source] Event' =====

// Load accounts
export const loadAccounts = createAction(
  '[Accounts Page] Load Accounts'
);

export const loadAccountsSuccess = createAction(
  '[Accounts API] Load Accounts Success',
  props<{ accounts: BankAccount[] }>()
  //    ↑ Payload đi kèm với action
);

export const loadAccountsFailure = createAction(
  '[Accounts API] Load Accounts Failure',
  props<{ error: string }>()
);

// Freeze account
export const freezeAccount = createAction(
  '[Accounts Page] Freeze Account',
  props<{ accountId: string }>()
);

export const freezeAccountSuccess = createAction(
  '[Accounts API] Freeze Account Success',
  props<{ account: BankAccount }>()
);

export const freezeAccountFailure = createAction(
  '[Accounts API] Freeze Account Failure',
  props<{ error: string }>()
);

// Select account
export const selectAccount = createAction(
  '[Accounts Page] Select Account',
  props<{ accountId: string }>()
);
```

---

## 5. Reducer — Logic Biến Đổi State

```typescript
// File: store/accounts/accounts.reducer.ts

import { createReducer, on } from '@ngrx/store';
import { EntityState, EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { BankAccount } from '../../core/models/account.model';
import * as AccountsActions from './accounts.actions';

// ===== STATE INTERFACE =====
export interface AccountsState extends EntityState<BankAccount> {
  // EntityState chứa: ids[], entities{}
  selectedAccountId: string | null;
  loading: boolean;
  error: string | null;
}

// EntityAdapter — quản lý collection hiệu quả
const adapter: EntityAdapter<BankAccount> = createEntityAdapter<BankAccount>({
  selectId: (account) => account.id, // Chọn field ID
  sortComparer: false                 // Không sort
});

// Initial state
const initialState: AccountsState = adapter.getInitialState({
  selectedAccountId: null,
  loading: false,
  error: null
});

// ===== REDUCER — PURE FUNCTION =====
// Nhận (state, action) → trả state MỚI (không mutate state cũ)
export const accountsReducer = createReducer(
  initialState,

  on(AccountsActions.loadAccounts, (state) => ({
    ...state,        // Spread operator — copy state cũ
    loading: true,   // Cập nhật chỉ trường loading
    error: null
  })),

  on(AccountsActions.loadAccountsSuccess, (state, { accounts }) =>
    adapter.setAll(accounts, {   // adapter.setAll thay thế toàn bộ collection
      ...state,
      loading: false
    })
  ),

  on(AccountsActions.loadAccountsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  on(AccountsActions.freezeAccountSuccess, (state, { account }) =>
    adapter.updateOne(           // Cập nhật 1 entity trong collection
      { id: account.id, changes: account },
      state
    )
  ),

  on(AccountsActions.selectAccount, (state, { accountId }) => ({
    ...state,
    selectedAccountId: accountId
  }))
);

// Export selectors từ adapter
export const {
  selectAll: selectAllAccounts,
  selectEntities: selectAccountEntities,
  selectIds: selectAccountIds,
} = adapter.getSelectors();
```

---

## 6. Selectors — Đọc State Hiệu Quả (Memoized)

```typescript
// File: store/accounts/accounts.selectors.ts

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AccountsState, selectAllAccounts } from './accounts.reducer';

// Feature selector — trỏ đến 'accounts' slice trong Store
const selectAccountsFeature = createFeatureSelector<AccountsState>('accounts');
//                                                                   ↑ Tên key trong Store

// Selectors từ feature:
export const selectAllAccountsList = createSelector(
  selectAccountsFeature,
  selectAllAccounts  // Từ EntityAdapter
);

export const selectAccountsLoading = createSelector(
  selectAccountsFeature,
  (state) => state.loading
);

export const selectAccountsError = createSelector(
  selectAccountsFeature,
  (state) => state.error
);

export const selectSelectedAccountId = createSelector(
  selectAccountsFeature,
  (state) => state.selectedAccountId
);

// Derived selector — tính từ nhiều selectors khác (MEMOIZED!)
export const selectSelectedAccount = createSelector(
  selectAccountsFeature,
  selectSelectedAccountId,
  (state, selectedId) =>
    selectedId ? state.entities[selectedId] ?? null : null
);

export const selectTotalBalance = createSelector(
  selectAllAccountsList,
  (accounts) => accounts.reduce((sum, acc) => sum + acc.balance, 0)
  // Chỉ tính lại khi accounts thay đổi — không tính lại mỗi render
);

export const selectActiveAccounts = createSelector(
  selectAllAccountsList,
  (accounts) => accounts.filter(acc => acc.status === 'ACTIVE')
);
```

---

## 7. Effects — Async Operations (HTTP Calls)

```typescript
// File: store/accounts/accounts.effects.ts
// Effect = Xử lý side effects (HTTP, localStorage...) sau Action

import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, switchMap, of } from 'rxjs';
import { AccountService } from '../../core/services/account.service';
import * as AccountsActions from './accounts.actions';

@Injectable()
export class AccountsEffects {
  private readonly actions$ = inject(Actions); // Observable của tất cả Actions được dispatch
  private readonly accountService = inject(AccountService);

  // Effect cho loadAccounts
  loadAccounts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.loadAccounts),    // Chỉ listen action này
      switchMap(() =>                          // Hủy request cũ nếu có request mới
        this.accountService.getMyAccounts().pipe(
          map(res =>
            AccountsActions.loadAccountsSuccess({ accounts: res.data })
            // Dispatch Success action với data
          ),
          catchError(error =>
            of(AccountsActions.loadAccountsFailure({ error: error.message }))
            // Dispatch Failure action với lỗi
          )
        )
      )
    )
  );

  // Effect cho freezeAccount
  freezeAccount$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AccountsActions.freezeAccount),
      switchMap(({ accountId }) =>
        this.accountService.freezeAccount(accountId).pipe(
          map(res => AccountsActions.freezeAccountSuccess({ account: res.data })),
          catchError(error =>
            of(AccountsActions.freezeAccountFailure({ error: error.message }))
          )
        )
      )
    )
  );
}
```

---

## 8. Store Registration

```typescript
// app.config.ts

import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { accountsReducer } from './store/accounts/accounts.reducer';
import { AccountsEffects } from './store/accounts/accounts.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideStore({ accounts: accountsReducer }), // Đăng ký reducer
    provideEffects([AccountsEffects]),            // Đăng ký effects
    provideStoreDevtools({
      maxAge: 25,                                 // Lưu 25 state gần nhất
      logOnly: false,                             // Dev mode — bật time travel
    }),
  ]
};
```

---

## 9. Component Dùng NgRx Store

```typescript
// accounts.page.ts — Dùng NgRx Store thay vì Service trực tiếp

import { Store } from '@ngrx/store';
import * as AccountsActions from '../../../store/accounts/accounts.actions';
import * as AccountsSelectors from '../../../store/accounts/accounts.selectors';

@Component({
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './accounts.page.html'
})
export class AccountsPageNgrx implements OnInit {
  private readonly store = inject(Store);

  // Đọc state từ Store bằng Selector
  // toSignal() chuyển Observable → Signal
  readonly accounts       = toSignal(this.store.select(AccountsSelectors.selectAllAccountsList));
  readonly loading        = toSignal(this.store.select(AccountsSelectors.selectAccountsLoading));
  readonly error          = toSignal(this.store.select(AccountsSelectors.selectAccountsError));
  readonly totalBalance   = toSignal(this.store.select(AccountsSelectors.selectTotalBalance));
  readonly selectedAccount= toSignal(this.store.select(AccountsSelectors.selectSelectedAccount));

  ngOnInit(): void {
    // Dispatch action → Effect lắng nghe → gọi API → dispatch Success
    this.store.dispatch(AccountsActions.loadAccounts());
  }

  selectAccount(accountId: string): void {
    this.store.dispatch(AccountsActions.selectAccount({ accountId }));
  }

  freezeAccount(accountId: string): void {
    this.store.dispatch(AccountsActions.freezeAccount({ accountId }));
  }
}
```

---

## 10. NgRx vs Signals — Khi Nào Dùng Gì?

```
✅ Dùng Signals (như BankX):
   - App dưới 20 features
   - Team 1-3 người
   - Không cần Redux DevTools
   - Không cần time-travel debugging
   - Muốn code nhanh, ít boilerplate

✅ Dùng NgRx:
   - App enterprise 50+ features
   - Team 5+ người làm song song
   - Cần strict traceability (banking audit: AI thay đổi state gì)
   - Cần Redux DevTools để debug phức tạp
   - State updates phức tạp, nhiều components phụ thuộc nhau

🔀 Hybrid approach (hiện đại nhất):
   - Dùng NgRx Signal Store (@ngrx/signals) — kết hợp tốt nhất của 2 thế giới
   - Available từ NgRx 17+
   - Signals-based API, không cần boilerplate cổ điển
```

---

**← [Bài 12 — Animations](./12_angular_animations.md)** | **→ [Bài 14 — RxJS Nâng Cao](./14_rxjs_nang_cao.md)**
