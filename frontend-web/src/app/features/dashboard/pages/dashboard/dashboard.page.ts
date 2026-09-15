import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccountService, BankAccount } from '../../../../core/services/account.service';
import { AuthService } from '../../../../core/auth/auth.service';

export interface MockTransaction {
  id: string;
  description: string;
  timestamp: string;
  counterparty: string;
  amount: number;
  type: 'IN' | 'OUT';
}

/**
 * Màn hình Tổng quan Tài khoản (Main Dashboard Page - TPBank UI).
 *
 * Chức năng:
 * - Thống kê số dư tài khoản thanh toán BankAccount Aggregate.
 * - Cho phép Ẩn/Hiện số dư (Hide/Show balance).
 * - Sao chép số tài khoản (Copy to clipboard).
 * - Mở tài khoản thanh toán mới qua REST API `POST /api/v1/accounts`.
 * - Phong tỏa tài khoản (Freeze) phục vụ kiểm thử Optimistic Lock &#64;Version.
 * - Khu vực Dịch vụ nhanh & Lịch sử giao dịch gần đây.
 * - Panel Hướng dẫn Kiểm thử Thủ công (Verification Guide Panel) cho Sprint 05.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DecimalPipe],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss'
})
export class DashboardPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly authService = inject(AuthService);

  public readonly currentUser = this.authService.currentUser;

  public accounts = signal<BankAccount[]>([]);
  public primaryAccount = signal<BankAccount | null>(null);

  public loadingAccounts = true;
  public showBalance = true;
  public toastMessage: string | null = null;

  // Modal State
  public showCreateAccountModal = false;
  public newAccountName = 'Tài khoản Tiết kiệm Linh hoạt';
  public newAccountInitialBalance = 5000000;
  public submittingAccount = false;
  public modalError: string | null = null;

  // Mock Transactions for UI preview
  public mockTransactions: MockTransaction[] = [
    {
      id: 'tx-101',
      description: 'Nhận tiền từ NGUYEN VAN A',
      timestamp: 'Hôm nay, 14:32',
      counterparty: '0982***321',
      amount: 2500000,
      type: 'IN'
    },
    {
      id: 'tx-102',
      description: 'Thanh toán Điện lực EVN',
      timestamp: 'Hôm qua, 09:15',
      counterparty: 'EVN HANOI',
      amount: 485000,
      type: 'OUT'
    },
    {
      id: 'tx-103',
      description: 'Chuyển tiền mua sắmShopee',
      timestamp: '12/09/2026',
      counterparty: 'SHOPEEPAY',
      amount: 1250000,
      type: 'OUT'
    },
    {
      id: 'tx-104',
      description: 'Thưởng hiệu suất Sprint 04',
      timestamp: '10/09/2026',
      counterparty: 'BANKX CORP',
      amount: 10000000,
      type: 'IN'
    }
  ];

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loadingAccounts = true;
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        this.loadingAccounts = false;
        if (res.code === 0 && res.data) {
          this.accounts.set(res.data);
          if (res.data.length > 0) {
            this.primaryAccount.set(res.data[0]);
          }
        }
      },
      error: () => {
        this.loadingAccounts = false;
        // Mock fallback account if backend empty/offline for initial view
        const fallbackAccount: BankAccount = {
          id: 'acc-demo-001',
          customerId: 'cust-demo-001',
          accountNumber: '88889999001',
          accountName: 'Tài khoản Thanh toán TPBank',
          balance: 125000000,
          currency: 'VND',
          status: 'ACTIVE',
          version: 1
        };
        this.accounts.set([fallbackAccount]);
        this.primaryAccount.set(fallbackAccount);
      }
    });
  }

  refreshAccounts(): void {
    this.loadAccounts();
    this.showToast('Đã tải lại số dư tài khoản từ Redis Balance Cache 30s');
  }

  selectPrimaryAccount(acc: BankAccount): void {
    this.primaryAccount.set(acc);
  }

  toggleBalanceVisibility(): void {
    this.showBalance = !this.showBalance;
  }

  copyAccountNumber(accNum?: string): void {
    if (!accNum) return;
    navigator.clipboard.writeText(accNum);
    this.showToast(`Đã sao chép số tài khoản: ${accNum}`);
  }

  toggleFreezeCurrentAccount(): void {
    const acc = this.primaryAccount();
    if (!acc) return;

    this.accountService.freezeAccount(acc.id).subscribe({
      next: (res) => {
        if (res.code === 0 && res.data) {
          this.primaryAccount.set(res.data);
          this.loadAccounts();
          this.showToast(`Tài khoản ${acc.accountNumber} đã chuyển trạng thái ${res.data.status} (version ${res.data.version})`);
        }
      },
      error: (err) => {
        this.showToast(`Lỗi phong tỏa tài khoản: ${err?.error?.message || 'Có lỗi xảy ra'}`);
      }
    });
  }

  openCreateAccountModal(): void {
    this.modalError = null;
    this.showCreateAccountModal = true;
  }

  closeCreateAccountModal(): void {
    this.showCreateAccountModal = false;
  }

  submitCreateAccount(): void {
    if (!this.newAccountName.trim()) {
      this.modalError = 'Vui lòng nhập tên tài khoản!';
      return;
    }

    this.submittingAccount = true;
    this.modalError = null;

    this.accountService.createAccount({
      accountName: this.newAccountName,
      initialBalance: this.newAccountInitialBalance
    }).subscribe({
      next: (res) => {
        this.submittingAccount = false;
        if (res.code === 0 && res.data) {
          this.showCreateAccountModal = false;
          this.showToast(`Mở tài khoản "${res.data.accountName}" thành công! STK: ${res.data.accountNumber}`);
          this.loadAccounts();
        }
      },
      error: (err) => {
        this.submittingAccount = false;
        this.modalError = err?.error?.message || 'Khôi phục tạo tài khoản thất bại!';
      }
    });
  }

  private showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      this.toastMessage = null;
    }, 4000);
  }
}
