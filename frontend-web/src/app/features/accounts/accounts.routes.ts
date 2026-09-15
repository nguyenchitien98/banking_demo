import { Routes } from '@angular/router';

export const ACCOUNTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/accounts/accounts.page').then((m) => m.AccountsPage),
  },
  {
    path: 'history',
    loadComponent: () =>
      import('./pages/transaction-history/transaction-history.page').then((m) => m.TransactionHistoryPage),
  },
];
