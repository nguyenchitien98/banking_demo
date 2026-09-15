import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService, BankAccount } from '../../../../core/services/account.service';
import { TransferService, TransferResult } from '../../../../core/services/transfer.service';

/**
 * Màn hình Thực hiện Chuyển tiền Nội bộ (Internal Bank Transfer Page — TPBank UI).
 *
 * Chức năng:
 * - Chọn tài khoản trích nợ (Source Account).
 * - Truy vấn số tài khoản thụ hưởng (Recipient Inquiry).
 * - Nhập số tiền giao dịch và các phím chọn nhanh.
 * - Modal xác nhận giao dịch trước khi gửi lệnh.
 * - Biên lai giao dịch thành công (Transfer Result Receipt).
 * - Panel Hướng dẫn Kiểm thử Thủ công (Verification Guide Panel) cho Sprint 07.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-transfers-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './transfers.page.html',
  styleUrl: './transfers.page.scss'
})
export class TransfersPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);

  public accounts = signal<BankAccount[]>([]);
  public sourceAccountId = '';
  public targetAccountNumber = '';
  public amount = 500000;
  public description = 'Chuyen tien noi bo BankX';

  // State
  public inquiring = false;
  public recipientName: string | null = null;
  public recipientError: string | null = null;
  public formError: string | null = null;
  public submitting = false;

  // Confirmation Modal
  public showConfirmModal = false;

  // Completed Result Receipt
  public completedTransfer: TransferResult | null = null;

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        if (res.code === 0 && res.data && res.data.length > 0) {
          this.accounts.set(res.data);
          this.sourceAccountId = res.data[0].id;
        }
      },
      error: () => {
        // Fallback account for preview
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

  onSourceAccountChange(): void {
    this.formError = null;
  }

  setAmount(val: number): void {
    this.amount = val;
  }

  onInquireRecipient(): void {
    const accNum = this.targetAccountNumber.trim();
    if (!accNum) {
      this.recipientName = null;
      this.recipientError = null;
      return;
    }

    this.inquiring = true;
    this.recipientError = null;
    this.recipientName = null;

    this.transferService.inquireRecipient(accNum).subscribe({
      next: (res) => {
        this.inquiring = false;
        if (res.code === 0 && res.data) {
          this.recipientName = res.data.accountName;
        }
      },
      error: (err) => {
        this.inquiring = false;
        this.recipientError = err?.error?.message || 'Không tìm thấy tài khoản thụ hưởng!';
      }
    });
  }

  openConfirmModal(): void {
    this.formError = null;

    if (!this.sourceAccountId) {
      this.formError = 'Vui lòng chọn tài khoản trích nợ!';
      return;
    }

    if (!this.targetAccountNumber.trim()) {
      this.formError = 'Vui lòng nhập số tài khoản người nhận!';
      return;
    }

    if (!this.recipientName) {
      this.formError = 'Vui lòng bấm "Kiểm tra" để xác thực tên người nhận!';
      return;
    }

    if (this.amount < 1000) {
      this.formError = 'Số tiền chuyển tối thiểu là 1,000 VND!';
      return;
    }

    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
  }

  submitTransfer(): void {
    this.submitting = true;

    this.transferService.createInternalTransfer({
      sourceAccountId: this.sourceAccountId,
      targetAccountNumber: this.targetAccountNumber.trim(),
      amount: this.amount,
      description: this.description
    }).subscribe({
      next: (res) => {
        this.submitting = false;
        this.showConfirmModal = false;
        if (res.code === 0 && res.data) {
          this.completedTransfer = res.data;
          this.loadAccounts(); // Refresh balance
        }
      },
      error: (err) => {
        this.submitting = false;
        this.showConfirmModal = false;
        this.formError = err?.error?.message || 'Chuyển tiền thất bại, vui lòng thử lại!';
      }
    });
  }

  getSourceAccountNumber(): string {
    const acc = this.accounts().find((a) => a.id === this.sourceAccountId);
    return acc ? `${acc.accountNumber} (${acc.accountName})` : '';
  }

  resetForm(): void {
    this.completedTransfer = null;
    this.targetAccountNumber = '';
    this.recipientName = null;
    this.recipientError = null;
    this.amount = 500000;
    this.formError = null;
  }

  printReceipt(): void {
    window.print();
  }
}
