import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QrPaymentService, ParseQrResponse, GenerateQrResponse, QrPaymentResult } from '../../../../core/services/qr-payment.service';
import { AccountService, BankAccount } from '../../../../core/services/account.service';

@Component({
  selector: 'app-qr-payments-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qr-payments.page.html',
  styleUrls: ['./qr-payments.page.scss']
})
export class QrPaymentsPage implements OnInit {
  activeTab: 'scan' | 'myqr' = 'scan';
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Accounts
  accounts: BankAccount[] = [];
  selectedSourceAccountId = '';

  // Scan / Parse State
  rawQrInput = '';
  parsedQr: ParseQrResponse | null = null;
  customAmount: number | null = null;
  customDescription = '';

  // My QR Generator State
  selectedReceiveAccount = '';
  genAmount: number | null = null;
  genDescription = '';
  generatedQr: GenerateQrResponse | null = null;
  qrPayloadCopied = false;

  // Payment Confirmation Modal
  showConfirmModal = false;
  paymentResult: QrPaymentResult | null = null;

  // Sample VietQR Payloads for Testing
  sampleQrs = [
    {
      name: '⚡ QR 1: TPBank 50.000đ (Chuyển tiền cà phê)',
      payload: '00020101021238540010A00000072701280006970423011410888899990208QRIBFTTA53037045405500005802VN62190815Chuyen cafe BankX63047A3B'
    },
    {
      name: '💧 QR 2: Vietcombank 250.000đ (Dynamic QR)',
      payload: '00020101021238540010A00000072701280006970436011499887766550208QRIBFTTA530370454062500005802VN62180814Thanh toan QR6304C10D'
    },
    {
      name: '🏷️ QR 3: BankX Tĩnh (Không kèm số tiền)',
      payload: '00020101021138540010A00000072701280006970400011410009988770208QRIBFTTA53037045802VN62180814Ung ho quy sike63042E9F'
    }
  ];

  constructor(
    private qrService: QrPaymentService,
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    this.loadUserAccounts();
  }

  loadUserAccounts(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (res) => {
        if (res.data) {
          this.accounts = res.data;
          if (res.data.length > 0) {
            this.selectedSourceAccountId = res.data[0].id;
            this.selectedReceiveAccount = res.data[0].accountNumber;
            this.generateMyQrCode();
          }
        }
      },
      error: (err: any) => {
        console.error('Lỗi lấy danh sách tài khoản:', err);
      }
    });
  }

  // Parse Action
  parseQrString(qrString: string): void {
    if (!qrString || !qrString.trim()) {
      this.parsedQr = null;
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.qrService.parseQr(qrString.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.parsedQr = res.data;
          this.customAmount = res.data.amount;
          this.customDescription = res.data.description || 'Chuyen tien qua VietQR';
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Chuỗi mã QR không đúng định dạng VietQR EMVCo';
        this.parsedQr = null;
      }
    });
  }

  useSampleQr(payload: string): void {
    this.rawQrInput = payload;
    this.parseQrString(payload);
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      // Simulate reading QR image from file
      this.useSampleQr(this.sampleQrs[0].payload);
    }
  }

  // Generate My QR
  generateMyQrCode(): void {
    if (!this.selectedReceiveAccount) return;
    this.qrService.getMyQr(this.selectedReceiveAccount, this.genAmount || undefined, this.genDescription || undefined).subscribe({
      next: (res) => {
        if (res.data) {
          this.generatedQr = res.data;
        }
      },
      error: (err: any) => {
        console.error('Lỗi sinh mã QR:', err);
      }
    });
  }

  copyQrPayload(): void {
    if (this.generatedQr?.qrPayload) {
      navigator.clipboard.writeText(this.generatedQr.qrPayload);
      this.qrPayloadCopied = true;
      setTimeout(() => this.qrPayloadCopied = false, 2500);
    }
  }

  // Payment Flow
  openPaymentConfirm(): void {
    if (!this.parsedQr) return;
    const finalAmount = this.parsedQr.amount || this.customAmount;
    if (!finalAmount || finalAmount < 1000) {
      this.errorMessage = 'Vui lòng nhập số tiền thanh toán hợp lệ (tối thiểu 1,000 VND)';
      return;
    }
    this.errorMessage = '';
    this.showConfirmModal = true;
  }

  confirmPayment(): void {
    if (!this.parsedQr) return;
    const finalAmount = this.parsedQr.amount || this.customAmount || 0;
    
    this.loading = true;
    const idempotencyKey = 'QR-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);

    const payload = {
      sourceAccountId: this.selectedSourceAccountId,
      targetAccountNumber: this.parsedQr.accountNumber,
      targetBankBin: this.parsedQr.bankBin,
      targetAccountName: this.parsedQr.accountHolderName || 'NGUYEN CHITIEN',
      amount: finalAmount,
      description: this.customDescription || 'Thanh toan VietQR',
      qrPayload: this.parsedQr.rawPayload
    };

    this.qrService.payQr(payload, idempotencyKey).subscribe({
      next: (res) => {
        this.loading = false;
        this.showConfirmModal = false;
        if (res.data) {
          this.paymentResult = res.data;
          this.successMessage = `Thanh toán VietQR thành công! Mã GD: ${res.data.paymentId}`;
          this.loadUserAccounts(); // Refresh balance
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Thanh toán QR thất bại. Vui lòng kiểm tra lại số dư';
      }
    });
  }

  getSelectedAccountBalance(): number {
    const acc = this.accounts.find(a => a.id === this.selectedSourceAccountId);
    return acc ? acc.balance : 0;
  }
}
