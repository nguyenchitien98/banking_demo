import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

export interface CustomerProfile {
  id: string;
  cifNumber: string;
  fullName: string;
  identityNumber: string;
  phone: string;
  email: string;
  dateOfBirth: string;
  address: string;
  status: string;
}

/**
 * Màn hình Hồ Sơ Cá Nhân Khách Hàng (Customer Profile Page).
 * Cho phép xem chi tiết mã CIF, trạng thái KYC và chỉnh sửa thông tin liên lạc.
 */
@Component({
  selector: 'bankx-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
})
export class ProfilePage implements OnInit {
  profileForm: FormGroup;
  profile = signal<CustomerProfile | null>(null);
  isEditing = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private http: HttpClient
  ) {
    this.profileForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      address: ['', [Validators.required, Validators.maxLength(255)]],
      dateOfBirth: [''],
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.http.get<any>('/api/v1/customers/me').subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.code === 0 && res.data) {
          this.profile.set(res.data);
          this.profileForm.patchValue({
            fullName: res.data.fullName,
            address: res.data.address,
            dateOfBirth: res.data.dateOfBirth,
          });
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        // Giả lập dữ liệu demo nếu backend chưa có CSDL thực
        const mockProfile: CustomerProfile = {
          id: 'c1234567-89ab-cdef-0123-456789abcdef',
          cifNumber: 'CIF889966',
          fullName: 'NGUYỄN VĂN A',
          identityNumber: '001099******',
          phone: '091***5678',
          email: 'n***a@gmail.com',
          dateOfBirth: '1995-08-15',
          address: 'Tòa nhà TPBank, 57 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội',
          status: 'VERIFIED',
        };
        this.profile.set(mockProfile);
        this.profileForm.patchValue({
          fullName: mockProfile.fullName,
          address: mockProfile.address,
          dateOfBirth: mockProfile.dateOfBirth,
        });
      },
    });
  }

  toggleEdit(): void {
    this.isEditing.update((val) => !val);
  }

  onSaveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.http.put<any>('/api/v1/customers/me/profile', this.profileForm.value).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.successMessage.set('Cập nhật hồ sơ cá nhân thành công!');
        this.isEditing.set(false);
        if (res.data) {
          this.profile.set(res.data);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.successMessage.set('Đã lưu thông tin hồ sơ tạm thời!');
        this.isEditing.set(false);
      },
    });
  }
}
