import { Routes } from '@angular/router';

export const TRANSFERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/transfers/transfers.page').then((m) => m.TransfersPage),
  },
];
