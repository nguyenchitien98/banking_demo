import { Routes } from '@angular/router';

export const MONITORING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/monitoring-dashboard/monitoring-dashboard.page').then(
        (m) => m.MonitoringDashboardPage
      )
  }
];
