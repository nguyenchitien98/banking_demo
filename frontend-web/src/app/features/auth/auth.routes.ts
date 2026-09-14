import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'otp-verify',
    loadComponent: () =>
      import('./pages/otp-verify/otp-verify.page').then((m) => m.OtpVerifyPage),
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
