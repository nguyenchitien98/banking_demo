import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layouts/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(
            (m) => m.DASHBOARD_ROUTES
          ),
      },
      {
        path: 'accounts',
        loadChildren: () =>
          import('./features/accounts/accounts.routes').then(
            (m) => m.ACCOUNTS_ROUTES
          ),
      },
      {
        path: 'transfers',
        loadChildren: () =>
          import('./features/transfers/transfers.routes').then(
            (m) => m.TRANSFERS_ROUTES
          ),
      },
      {
        path: 'payments',
        loadChildren: () =>
          import('./features/payments/payments.routes').then(
            (m) => m.PAYMENTS_ROUTES
          ),
      },
      {
        path: 'cards',
        loadChildren: () =>
          import('./features/cards/cards.routes').then((m) => m.CARDS_ROUTES),
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('./features/notifications/notifications.routes').then(
            (m) => m.NOTIFICATIONS_ROUTES
          ),
      },
      {
        path: 'fraud',
        loadChildren: () =>
          import('./features/fraud/fraud.routes').then(
            (m) => m.FRAUD_ROUTES
          ),
      },
      {
        path: 'beneficiaries',
        loadChildren: () =>
          import('./features/beneficiaries/beneficiaries.routes').then(
            (m) => m.BENEFICIARIES_ROUTES
          ),
      },
      {
        path: 'admin',
        loadChildren: () =>
          import('./features/admin/admin.routes').then(
            (m) => m.ADMIN_ROUTES
          ),
      },
      {
        path: 'monitoring',
        loadChildren: () =>
          import('./features/monitoring/monitoring.routes').then(
            (m) => m.MONITORING_ROUTES
          ),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
