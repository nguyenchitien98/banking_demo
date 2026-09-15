import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Layout chính của BankX Web App (Banking Customer Portal).
 *
 * Chứa:
 * - Top header với TPBank Branding, thông tin tài khoản người dùng, nút Đăng xuất.
 * - Sidebar Navigation chính (Trang chủ, Tài khoản, Chuyển tiền, Thanh toán, Thẻ, Hồ sơ).
 * - Router Outlet hiển thị nội dung trang.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  public readonly currentUser = this.authService.currentUser;
  public sidebarOpen = false;

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  getUserInitials(): string {
    const username = this.currentUser()?.username || 'U';
    return username.substring(0, 2).toUpperCase();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
