import { Routes } from '@angular/router';
import { PaymentsPage } from './pages/payments/payments.page';
import { QrPaymentsPage } from './pages/qr-payments/qr-payments.page';

export const PAYMENTS_ROUTES: Routes = [
  { path: '', component: PaymentsPage },
  { path: 'qr', component: QrPaymentsPage }
];
