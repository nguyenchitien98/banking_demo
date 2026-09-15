import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

/**
 * Màn hình Xác thực Mã OTP (BankX OTP Verification Page).
 * Thiết kế giao diện chuẩn TPBank với đếm ngược 120 giây và ô nhập 6 chữ số tự động chuyển nét.
 */
@Component({
  selector: 'bankx-otp-verify',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './otp-verify.page.html',
  styleUrl: './otp-verify.page.scss',
})
export class OtpVerifyPage implements OnInit, OnDestroy {
  otpForm: FormGroup;
  countdown = signal<number>(120);
  isExpired = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  private timerInterval: any;

  // Giả lập thông tin nhận OTP (truyền từ luồng Login / Transfer)
  maskedPhone = signal<string>('091***5678');
  purpose = 'LOGIN';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.otpForm = this.fb.group({
      digit1: ['', [Validators.required, Validators.pattern('[0-9]')]],
      digit2: ['', [Validators.required, Validators.pattern('[0-9]')]],
      digit3: ['', [Validators.required, Validators.pattern('[0-9]')]],
      digit4: ['', [Validators.required, Validators.pattern('[0-9]')]],
      digit5: ['', [Validators.required, Validators.pattern('[0-9]')]],
      digit6: ['', [Validators.required, Validators.pattern('[0-9]')]],
    });
  }

  ngOnInit(): void {
    this.startCountdown();
  }

  ngOnDestroy(): void {
    this.stopCountdown();
  }

  private startCountdown(): void {
    this.stopCountdown();
    this.countdown.set(120);
    this.isExpired.set(false);

    this.timerInterval = setInterval(() => {
      this.countdown.update((val) => {
        if (val <= 1) {
          this.stopCountdown();
          this.isExpired.set(true);
          return 0;
        }
        return val - 1;
      });
    }, 1000);
  }

  private stopCountdown(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  onInputKeyUp(event: KeyboardEvent, nextIndex: number): void {
    const target = event.target as HTMLInputElement;
    if (target.value && nextIndex <= 6) {
      const nextInput = document.getElementById(`otp-digit-${nextIndex}`);
      if (nextInput) nextInput.focus();
    }
    if (event.key === 'Backspace' && nextIndex > 2 && !target.value) {
      const prevInput = document.getElementById(`otp-digit-${nextIndex - 2}`);
      if (prevInput) prevInput.focus();
    }
  }

  resendOtp(): void {
    if (!this.isExpired()) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.http.post<any>('/api/v1/auth/otp/send', { phone: '0912345678', purpose: this.purpose }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.successMessage.set('Mã OTP mới đã được gửi lại thành công');
        this.startCountdown();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể gửi lại OTP, vui lòng thử lại');
      },
    });
  }

  onVerify(): void {
    if (this.otpForm.invalid) {
      this.errorMessage.set('Vui lòng nhập đầy đủ 6 chữ số mã OTP');
      return;
    }

    const otpCode = Object.values(this.otpForm.value).join('');
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.http.post<any>('/api/v1/auth/otp/verify', {
      phone: '0912345678',
      purpose: this.purpose,
      otpCode,
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.code === 0) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage.set(res.message);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Mã OTP không chính xác hoặc đã hết hạn');
      },
    });
  }
}
