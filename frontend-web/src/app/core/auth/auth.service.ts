import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { TokenService } from './token.service';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserSummary {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
  traceId: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_URL = '/api/v1/auth';
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);

  // Signals cho State Management của Angular 22
  public currentUser = signal<UserSummary | null>(this.tokenService.getUserInfo());
  public isAuthenticated = signal<boolean>(this.tokenService.hasToken());

  /**
   * Gọi API Đăng nhập
   */
  login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.API_URL}/login`, credentials).pipe(
      tap((res) => {
        if (res.code === 0 && res.data) {
          this.tokenService.setAccessToken(res.data.accessToken);
          this.tokenService.setRefreshToken(res.data.refreshToken);
          this.tokenService.setUserInfo(res.data.user);
          this.currentUser.set(res.data.user);
          this.isAuthenticated.set(true);
        }
      })
    );
  }

  /**
   * Gọi API Refresh Token
   */
  refreshToken(): Observable<ApiResponse<LoginResponse>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http.post<ApiResponse<LoginResponse>>(`${this.API_URL}/refresh`, { refreshToken }).pipe(
      tap((res) => {
        if (res.code === 0 && res.data) {
          this.tokenService.setAccessToken(res.data.accessToken);
          this.tokenService.setRefreshToken(res.data.refreshToken);
        }
      })
    );
  }

  /**
   * Đăng xuất
   */
  logout(): void {
    this.http.post(`${this.API_URL}/logout`, {}).subscribe({
      complete: () => this.clearSession(),
      error: () => this.clearSession(),
    });
  }

  private clearSession(): void {
    this.tokenService.clearTokens();
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }
}
