import { Routes } from '@angular/router';

export const MONITORING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/monitoring-dashboard/monitoring-dashboard.page').then(
        (m) => m.MonitoringDashboardPage
      )
  },
  {
    path: 'tracing',
    loadComponent: () =>
      import('./pages/tracing/tracing.page').then(
        (m) => m.TracingPage
      )
  }
];
