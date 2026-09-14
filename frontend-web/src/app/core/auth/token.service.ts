import { Injectable } from '@angular/core';

/** Key lưu trong sessionStorage — Không dùng localStorage để tự xóa khi đóng tab */
const ACCESS_TOKEN_KEY = 'bankx_access_token';

/**
 * Service quản lý JWT tokens của BankX.
 *
 * Bảo mật:
 * - Access token: Lưu sessionStorage (tự xóa khi đóng tab/browser)
 * - Refresh token: Backend lưu trong HttpOnly Cookie (không accessible từ JS)
 * - KHÔNG lưu sensitive data trong localStorage
 *
 * Lý do dùng sessionStorage thay vì localStorage:
 * Nếu dùng localStorage, token tồn tại vĩnh viễn → Rủi ro nếu XSS.
 * SessionStorage tự xóa khi đóng tab → Bảo mật hơn với tradeoff phải
 * login lại khi mở tab mới (acceptable cho banking app).
 */
@Injectable({ providedIn: 'root' })
export class TokenService {

  /**
   * Lưu access token vào sessionStorage.
   *
   * @param token JWT access token nhận từ /auth/login hoặc /auth/refresh
   */
  saveAccessToken(token: string): void {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
  }

  /**
   * Lấy access token hiện tại.
   *
   * @returns Access token hoặc null nếu chưa đăng nhập
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  /**
   * Xóa toàn bộ tokens (dùng khi logout hoặc 401).
   */
  clearTokens(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  }

  /**
   * Kiểm tra user đã đăng nhập chưa.
   *
   * @returns true nếu có access token
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}
