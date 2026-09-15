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
  },
  {
    path: 'resilience',
    loadComponent: () =>
      import('./pages/resilience/resilience.page').then(
        (m) => m.ResiliencePage
      )
  },
  {
    path: 'engineering',
    loadComponent: () =>
      import('../engineering/pages/engineering-portal/engineering-portal.page').then(
        (m) => m.EngineeringPortalPage
      )
  }
];
