import { Injectable } from '@angular/core';

/**
 * Service quản lý lưu trữ và truy xuất JWT Access Token và Refresh Token phía Client.
 */
@Injectable({
  providedIn: 'root',
})
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'bankx_access_token';
  private readonly REFRESH_TOKEN_KEY = 'bankx_refresh_token';
  private readonly USER_KEY = 'bankx_user_info';

  /** Save Access Token */
  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  /** Get Access Token */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /** Save Refresh Token */
  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  /** Get Refresh Token */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /** Save User Info */
  setUserInfo(user: any): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  /** Get User Info */
  getUserInfo(): any {
    const data = localStorage.getItem(this.USER_KEY);
    return data ? JSON.parse(data) : null;
  }

  /** Clear tokens on logout */
  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  /** Check if user has token */
  hasToken(): boolean {
    return !!this.getAccessToken();
  }
}
