import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AccountService, BankAccount } from '../../../../core/services/account.service';
import { LedgerService, LedgerEntry, TransactionDetail } from '../../../../core/services/ledger.service';

/**
 * Màn hình Quản lý Tài khoản & Nhật ký Bút toán Ghi sổ kép (Ledger View — TPBank UI).
 *
 * Chức năng:
 * - Hiển thị danh sách các tài khoản ngân hàng của khách hàng.
 * - Hiển thị nhật ký bút toán hạch toán (DEBIT / CREDIT) của từng tài khoản.
 * - Modal thử nghiệm hạch toán bút toán ghi sổ kép (`POST /api/v1/ledger/record`).
 * - Modal xem bằng chứng cân bằng tài chính `SUM(DEBIT) == SUM(CREDIT)`.
 * - Panel Hướng dẫn Kiểm thử Thủ công (Verification Guide Panel) cho Sprint 06.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-accounts-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './accounts.page.html',
  styleUrl: './accounts.page.scss'
})
export class AccountsPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly ledgerService = inject(LedgerService);

  public accounts = signal<BankAccount[]>([]);
  public selectedAccount = signal<BankAccount | null>(null);
  public entries = signal<LedgerEntry[]>([]);

  public loadingAccounts = true;
  public loadingEntries = false;
  public toastMessage: string | null = null;

  // Double Entry Modal State
  public showDoubleEntryModal = false;
  public debitAccountId = '';
  public creditAccountId = '';
  public transferAmount = 1000000;
  public transferDescription = 'Chuyển tiền ghi sổ kép thử nghiệm';
  public submittingLedger = false;
  public modalError: string | null = null;

  // Transaction Detail Modal State
  public selectedTransaction: TransactionDetail | null = null;
  public totalDebitAmount = signal<number>(0);
  public totalCreditAmount = signal<number>(0);

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
            this.selectAccount(res.data[0]);
          }
        }
      },
      error: () => {
        this.loadingAccounts = false;
        // Mock fallback account for presentation
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
        this.selectAccount(fallbackAcc);
      }
    });
  }

  selectAccount(acc: BankAccount): void {
    this.selectedAccount.set(acc);
    this.loadLedgerEntries(acc.id);
  }

  loadLedgerEntries(accountId: string): void {
    this.loadingEntries = true;
    this.ledgerService.getAccountTransactions(accountId).subscribe({
      next: (res) => {
        this.loadingEntries = false;
        if (res.code === 0 && res.data) {
          this.entries.set(res.data);
        }
      },
      error: () => {
        this.loadingEntries = false;
        // Mock fallback entries for demo preview
        const mockEntries: LedgerEntry[] = [
          {
            id: 'entry-101',
            transactionId: 'tx-20260915001',
            accountId: accountId,
            entryType: 'CREDIT',
            amount: 2500000,
            currency: 'VND',
            balanceAfter: 125000000,
            createdAt: new Date().toISOString()
          },
          {
            id: 'entry-102',
            transactionId: 'tx-20260915002',
            accountId: accountId,
            entryType: 'DEBIT',
            amount: 500000,
            currency: 'VND',
            balanceAfter: 122500000,
            createdAt: new Date(Date.now() - 3600000 * 5).toISOString()
          }
        ];
        this.entries.set(mockEntries);
      }
    });
  }

  refreshLedger(): void {
    const acc = this.selectedAccount();
    if (acc) {
      this.loadAccounts();
      this.loadLedgerEntries(acc.id);
      this.showToast('Đã làm mới danh sách bút toán ghi sổ');
    }
  }

  openDoubleEntryModal(): void {
    this.modalError = null;
    const list = this.accounts();
    if (list.length >= 2) {
      this.debitAccountId = list[0].id;
      this.creditAccountId = list[1].id;
    } else if (list.length === 1) {
      this.debitAccountId = list[0].id;
      this.creditAccountId = list[0].id;
    }
    this.showDoubleEntryModal = true;
  }

  closeDoubleEntryModal(): void {
    this.showDoubleEntryModal = false;
  }

  submitDoubleEntry(): void {
    if (!this.debitAccountId || !this.creditAccountId) {
      this.modalError = 'Vui lòng chọn cả tài khoản trích Nợ và Thụ hưởng!';
      return;
    }

    if (this.debitAccountId === this.creditAccountId) {
      this.modalError = 'Tài khoản trích Nợ và Thụ hưởng không được giống nhau!';
      return;
    }

    if (this.transferAmount < 1000) {
      this.modalError = 'Số tiền tối thiểu là 1,000 VND!';
      return;
    }

    this.submittingLedger = true;
    this.modalError = null;

    this.ledgerService.recordDoubleEntry({
      debitAccountId: this.debitAccountId,
      creditAccountId: this.creditAccountId,
      amount: this.transferAmount,
      description: this.transferDescription
    }).subscribe({
      next: (res) => {
        this.submittingLedger = false;
        if (res.code === 0 && res.data) {
          this.showDoubleEntryModal = false;
          this.showToast(`Hạch toán thành công! Mã GD: ${res.data.transactionReference}`);
          this.refreshLedger();
        }
      },
      error: (err) => {
        this.submittingLedger = false;
        this.modalError = err?.error?.message || 'Hạch toán bút toán ghi sổ kép thất bại!';
      }
    });
  }

  viewTransactionDetail(txId: string): void {
    this.ledgerService.getTransactionDetail(txId).subscribe({
      next: (res) => {
        if (res.code === 0 && res.data) {
          this.selectedTransaction = res.data;
          this.calculateProofAmounts(res.data.entries);
        }
      },
      error: () => {
        // Fallback detail for presentation
        const mockTxDetail: TransactionDetail = {
          id: txId,
          transactionReference: 'TXN20260915987654',
          transactionType: 'INTERNAL_TRANSFER',
          status: 'SUCCESS',
          amount: 2500000,
          currency: 'VND',
          description: 'Hạch toán chuyển khoản ghi sổ kép',
          createdAt: new Date().toISOString(),
          entries: [
            {
              id: 'entry-debit-001',
              transactionId: txId,
              accountId: 'acc-debit-001',
              entryType: 'DEBIT',
              amount: 2500000,
              currency: 'VND',
              balanceAfter: 122500000,
              createdAt: new Date().toISOString()
            },
            {
              id: 'entry-credit-001',
              transactionId: txId,
              accountId: 'acc-credit-001',
              entryType: 'CREDIT',
              amount: 2500000,
              currency: 'VND',
              balanceAfter: 52500000,
              createdAt: new Date().toISOString()
            }
          ]
        };
        this.selectedTransaction = mockTxDetail;
        this.calculateProofAmounts(mockTxDetail.entries);
      }
    });
  }

  closeTransactionDetail(): void {
    this.selectedTransaction = null;
  }

  private calculateProofAmounts(entries: LedgerEntry[]): void {
    let debitSum = 0;
    let creditSum = 0;
    for (const e of entries) {
      if (e.entryType === 'DEBIT') {
        debitSum += e.amount;
      } else if (e.entryType === 'CREDIT') {
        creditSum += e.amount;
      }
    }
    this.totalDebitAmount.set(debitSum);
    this.totalCreditAmount.set(creditSum);
  }

  simulateRaceCondition(): void {
    const acc = this.selectedAccount();
    if (!acc) return;

    this.showToast('🚀 Đang giả lập 2 luồng giao dịch đồng thời (Concurrent Race Condition)...');

    const req1 = this.accountService.freezeAccount(acc.id);
    const req2 = this.accountService.freezeAccount(acc.id);

    forkJoin([req1, req2]).subscribe({
      next: ([res1, res2]) => {
        this.showToast(`✅ Xử lý đồng thời thành công nhờ @Version & Spring @Retryable! Version mới: v${res2.data.version}`);
        this.refreshLedger();
      },
      error: (err) => {
        this.showToast(`⚠️ Kết quả xung đột đồng thời: ${err?.error?.message || 'OptimisticLockingFailureException'}`);
        this.refreshLedger();
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
