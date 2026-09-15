import { Routes } from '@angular/router';

export const TRANSFERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/transfers/transfers.page').then((m) => m.TransfersPage),
  },
  {
    path: 'saga',
    loadComponent: () =>
      import('./pages/saga-orchestration/saga-orchestration.page').then((m) => m.SagaOrchestrationPage),
  },
];
