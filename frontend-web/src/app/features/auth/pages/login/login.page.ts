import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';

/**
 * Màn hình Đăng Nhập Khách Hàng (BankX Login Page).
 * Thiết kế giao diện cao cấp màu tím gradient theo phong cách TPBank.
 */
@Component({
  selector: 'bankx-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  loginForm: FormGroup;
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal<boolean>(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(4)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [true],
    });
  }

  toggleShowPassword(): void {
    this.showPassword.update((val) => !val);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.code === 0) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage.set(res.message || 'Đăng nhập không thành công');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        const errorRes = err.error;
        this.errorMessage.set(errorRes?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác');
      },
    });
  }
}
