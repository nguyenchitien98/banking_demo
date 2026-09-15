import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardService, BankCard, CreateVirtualCardRequest } from '../../../../core/services/card.service';
import { AccountService, BankAccount } from '../../../../core/services/account.service';

@Component({
  selector: 'app-cards-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cards.page.html',
  styleUrls: ['./cards.page.scss']
})
export class CardsPage implements OnInit {
  cards: BankCard[] = [];
  selectedCard: BankCard | null = null;
  accounts: BankAccount[] = [];

  loading = false;
  errorMessage = '';
  successMessage = '';

  // New Virtual Card Form Modal
  showIssueModal = false;
  newCardAccount = '';
  newCardBrand: 'VISA' | 'MASTERCARD' | 'NAPAS' = 'VISA';
  newCardLimit = 50000000;

  // Limit Edit Modal
  showLimitModal = false;
  editSpendingLimit = 50000000;
  editDailyLimit = 100000000;

  // Block Modal
  showBlockModal = false;

  constructor(
    private cardService: CardService,
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    this.loadCards();
    this.loadAccounts();
  }

  loadCards(): void {
    this.loading = true;
    this.cardService.getCards().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.cards = res.data;
          if (res.data.length > 0 && !this.selectedCard) {
            this.selectedCard = res.data[0];
          } else if (this.selectedCard) {
            this.selectedCard = res.data.find(c => c.id === this.selectedCard?.id) || res.data[0];
          }
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Không thể tải danh sách thẻ ngân hàng';
      }
    });
  }

  loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.accounts = res.data;
          this.newCardAccount = res.data[0].accountNumber;
        }
      }
    });
  }

  selectCard(card: BankCard): void {
    this.selectedCard = card;
  }

  // FSM State Transitions
  toggleFreeze(): void {
    if (!this.selectedCard) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const isFrozen = this.selectedCard.status === 'FROZEN';
    const action$ = isFrozen 
      ? this.cardService.unfreezeCard(this.selectedCard.id) 
      : this.cardService.freezeCard(this.selectedCard.id);

    action$.subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.successMessage = isFrozen 
            ? 'Mở khóa thẻ thành công! Thẻ đã hoạt động trở lại.' 
            : 'Tạm khóa thẻ thành công! Mọi giao dịch bằng thẻ đã tạm dừng.';
          this.loadCards();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Thao tác đổi trạng thái thẻ thất bại';
      }
    });
  }

  confirmBlockCard(): void {
    if (!this.selectedCard) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.cardService.blockCard(this.selectedCard.id).subscribe({
      next: (res) => {
        this.loading = false;
        this.showBlockModal = false;
        if (res.data) {
          this.successMessage = 'Thẻ đã bị khóa vĩnh viễn (BLOCKED). Không thể khôi phục lại thẻ này.';
          this.loadCards();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.showBlockModal = false;
        this.errorMessage = err.error?.message || 'Lỗi khi báo mất / khóa vĩnh viễn thẻ';
      }
    });
  }

  // Issue Virtual Card
  issueVirtualCard(): void {
    if (!this.newCardAccount) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const req: CreateVirtualCardRequest = {
      customerId: 'CUST-001',
      accountNumber: this.newCardAccount,
      cardBrand: this.newCardBrand,
      spendingLimit: this.newCardLimit
    };

    this.cardService.createVirtualCard(req).subscribe({
      next: (res) => {
        this.loading = false;
        this.showIssueModal = false;
        if (res.data) {
          this.successMessage = `Phát hành Thẻ Ảo ${res.data.cardBrand} thành công! Số thẻ che mờ: ${res.data.maskedPan}`;
          this.loadCards();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Lỗi phát hành thẻ ảo mới';
      }
    });
  }

  // Edit Limits
  openLimitModal(): void {
    if (this.selectedCard) {
      this.editSpendingLimit = this.selectedCard.spendingLimit;
      this.editDailyLimit = this.selectedCard.dailyLimit;
      this.showLimitModal = true;
    }
  }

  saveLimits(): void {
    if (!this.selectedCard) return;

    this.loading = true;
    this.cardService.updateLimits(this.selectedCard.id, {
      spendingLimit: this.editSpendingLimit,
      dailyLimit: this.editDailyLimit
    }).subscribe({
      next: (res) => {
        this.loading = false;
        this.showLimitModal = false;
        if (res.data) {
          this.successMessage = 'Cập nhật hạn mức giao dịch online thành công!';
          this.loadCards();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Cập nhật hạn mức thất bại';
      }
    });
  }
}
