import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../auth/token.service';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const accessToken = tokenService.getAccessToken();
  if (accessToken) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }));
  }
  return next(req);
};
