import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { apiInterceptor } from './core/http/api.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';
import { loadingInterceptor } from './core/http/loading.interceptor';

/**
 * Cấu hình ứng dụng BankX Angular 22.
 *
 * Sử dụng Standalone API (không có NgModule) — cách tiêu chuẩn từ Angular 17+.
 * Mọi provider được đăng ký tại đây thay vì AppModule.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withViewTransitions()
    ),
    provideHttpClient(
      withInterceptors([
        apiInterceptor,
        loadingInterceptor,
        errorInterceptor,
      ])
    ),
  ],
};
