import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService, BankAccount } from '../../../../core/services/account.service';
import { PaymentService, PaymentProvider, BillInquiry, BillPayment } from '../../../../core/services/payment.service';

/**
 * Màn hình Thanh toán Hóa đơn Dịch vụ & Strategy Pattern (Bill Payment Page — TPBank UI).
 *
 * Chức năng:
 * - Chọn danh mục dịch vụ (Điện lực, Nước sinh hoạt, Viễn thông, Thử nghiệm).
 * - Tra cứu nợ cước thông qua Strategy Pattern tương ứng với từng nhà cung cấp.
 * - Chọn tài khoản trích nợ, xác nhận thanh toán với Idempotency Key.
 * - Hạch toán sổ kép Double-Entry & Ghi bản tin Transactional Outbox Event.
 * - Bảng Hướng dẫn Kiểm thử Thủ công (Verification Guide Panel) cho Sprint 13.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-payments-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './payments.page.html',
  styleUrl: './payments.page.scss'
})
export class PaymentsPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly paymentService = inject(PaymentService);

  public accounts = signal<BankAccount[]>([]);
  public providers = signal<PaymentProvider[]>([]);
  public history = signal<BillPayment[]>([]);

  public selectedCategory = 'ALL';
  public selectedProviderCode = 'EVN_HN';
  public customerBillCode = 'PE0100234567';
  public sourceAccountId = '';
  public idempotencyKey = '';

  // State
  public inquiring = false;
  public submitting = false;
  public inquiryResult: BillInquiry | null = null;
  public inquiryError: string | null = null;
  public formError: string | null = null;
  public toastMessage: string | null = null;

  // Modal & Receipt
  public showConfirmModal = false;
  public completedPayment: BillPayment | null = null;

  ngOnInit(): void {
    this.idempotencyKey = this.paymentService.generateIdempotencyKey();
    this.loadAccounts();
    this.loadProviders();
  }

  loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        if (res.code === 0 && res.data && res.data.length > 0) {
          this.accounts.set(res.data);
          this.sourceAccountId = res.data[0].id;
          this.loadHistory(res.data[0].id);
        }
      },
      error: () => {
        const fallbackAcc: BankAccount = {
          id: 'acc-demo-001',
          customerId: 'cust-demo-001',
          accountNumber: '88889999001',
          accountName: 'Tài khoản Thanh toán TPBank',
          balance: 125000000,
          currency: 'VND',
          status: 'ACTIVE',
          version: 1
        };
        this.accounts.set([fallbackAcc]);
        this.sourceAccountId = fallbackAcc.id;
      }
    });
  }

  loadProviders(): void {
    this.paymentService.getProviders().subscribe({
      next: (res) => {
        if (res.code === 0 && res.data && res.data.length > 0) {
          this.providers.set(res.data);
        }
      },
      error: () => {
        const fallbackProviders: PaymentProvider[] = [
          { id: '1', code: 'EVN_HN', name: 'Điện lực Hà Nội (EVN HN)', category: 'ELECTRICITY', status: 'ACTIVE' },
          { id: '2', code: 'WATER_HCM', name: 'Nước sinh hoạt DNP Hồ Chí Minh', category: 'WATER', status: 'ACTIVE' },
          { id: '3', code: 'VIETTEL_TEL', name: 'Viettel Telecom (Internet/Thoại)', category: 'TELECOM', status: 'ACTIVE' },
          { id: '4', code: 'MOCK_PROVIDER', name: 'Nhà cung cấp Thử nghiệm BankX', category: 'OTHER', status: 'ACTIVE' }
        ];
        this.providers.set(fallbackProviders);
      }
    });
  }

  loadHistory(accId: string): void {
    this.paymentService.getHistory(accId).subscribe({
      next: (res) => {
        if (res.code === 0 && res.data) {
          this.history.set(res.data);
        }
      }
    });
  }

  filteredProviders(): PaymentProvider[] {
    if (this.selectedCategory === 'ALL') {
      return this.providers();
    }
    return this.providers().filter(p => p.category === this.selectedCategory);
  }

  setCategory(cat: string): void {
    this.selectedCategory = cat;
    const filtered = this.filteredProviders();
    if (filtered.length > 0) {
      this.selectedProviderCode = filtered[0].code;
    }
    this.inquiryResult = null;
    this.inquiryError = null;
  }

  selectProvider(code: string): void {
    this.selectedProviderCode = code;
    this.inquiryResult = null;
    this.inquiryError = null;
  }

  inquireBill(): void {
    if (!this.customerBillCode.trim()) {
      this.inquiryError = 'Vui lòng nhập Mã khách hàng / Mã danh bạ!';
      return;
    }

    this.inquiring = true;
    this.inquiryError = null;
    this.inquiryResult = null;

    this.paymentService.inquireBill(this.selectedProviderCode, this.customerBillCode.trim()).subscribe({
      next: (res) => {
        this.inquiring = false;
        if (res.code === 0 && res.data) {
          this.inquiryResult = res.data;
        }
      },
      error: (err) => {
        this.inquiring = false;
        this.inquiryError = err?.error?.message || 'Không tìm thấy thông tin nợ cước cho mã khách hàng này!';
      }
    });
  }

  openConfirmModal(): void {
    if (!this.inquiryResult) {
      this.formError = 'Vui lòng bấm "Tra cứu nợ cước" trước khi thanh toán!';
      return;
    }
    this.formError = null;
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
  }

  submitPayBill(): void {
    if (!this.inquiryResult) return;

    this.submitting = true;
    this.paymentService.payBill({
      sourceAccountId: this.sourceAccountId,
      providerCode: this.inquiryResult.providerCode,
      customerBillCode: this.inquiryResult.customerBillCode,
      amount: this.inquiryResult.amount,
      idempotencyKey: this.idempotencyKey
    }).subscribe({
      next: (res) => {
        this.submitting = false;
        this.showConfirmModal = false;
        if (res.code === 0 && res.data) {
          this.completedPayment = res.data;
          this.showToast('✅ Thanh toán hóa đơn dịch vụ thành công!');
          this.loadAccounts();
          this.loadHistory(this.sourceAccountId);
        }
      },
      error: (err) => {
        this.submitting = false;
        this.showConfirmModal = false;
        this.formError = err?.error?.message || 'Thanh toán thất bại, vui lòng kiểm tra số dư!';
      }
    });
  }

  getSourceAccountNumber(): string {
    const acc = this.accounts().find(a => a.id === this.sourceAccountId);
    return acc ? `${acc.accountNumber} (${acc.accountName})` : '';
  }

  resetForm(): void {
    this.completedPayment = null;
    this.inquiryResult = null;
    this.formError = null;
    this.inquiryError = null;
    this.idempotencyKey = this.paymentService.generateIdempotencyKey();
  }

  private showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      this.toastMessage = null;
    }, 4000);
  }
}
