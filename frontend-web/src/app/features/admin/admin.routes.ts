import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/admin-portal/admin-portal.page').then(m => m.AdminPortalPage)
  }
];
