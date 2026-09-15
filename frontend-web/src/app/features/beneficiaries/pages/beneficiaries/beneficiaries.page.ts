import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BeneficiaryService, Beneficiary, AddBeneficiaryRequest } from '../../../../core/services/beneficiary.service';

@Component({
  selector: 'app-beneficiaries-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './beneficiaries.page.html',
  styleUrls: ['./beneficiaries.page.scss']
})
export class BeneficiariesPage implements OnInit {
  beneficiaries: Beneficiary[] = [];
  filteredBeneficiaries: Beneficiary[] = [];
  searchTerm = '';

  loading = false;
  errorMessage = '';
  successMessage = '';

  // Add Beneficiary Modal Form
  showAddModal = false;
  addAccountNumber = '';
  addBankBin = '970400';
  addBankName = 'BankX Digital Bank';
  addAccountHolderName = '';
  addNickname = '';
  isLookingUp = false;

  // Edit Nickname Modal
  showEditModal = false;
  editingBeneficiary: Beneficiary | null = null;
  editNickname = '';

  // Bank BIN options for select
  banks = [
    { bin: '970400', name: 'BankX Digital Bank' },
    { bin: '970423', name: 'TPBank (Ngân Hàng Tiên Phong)' },
    { bin: '970436', name: 'Vietcombank' },
    { bin: '970418', name: 'BIDV' },
    { bin: '970415', name: 'VietinBank' },
    { bin: '970422', name: 'MBBank' }
  ];

  constructor(
    private beneficiaryService: BeneficiaryService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBeneficiaries();
  }

  loadBeneficiaries(): void {
    this.loading = true;
    this.beneficiaryService.getBeneficiaries().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.beneficiaries = res.data;
          this.filterBeneficiaries();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Không thể tải danh bạ thụ hưởng';
      }
    });
  }

  filterBeneficiaries(): void {
    if (!this.searchTerm || !this.searchTerm.trim()) {
      this.filteredBeneficiaries = [...this.beneficiaries];
      return;
    }
    const term = this.searchTerm.toLowerCase().trim();
    this.filteredBeneficiaries = this.beneficiaries.filter(b => 
      b.accountNumber.toLowerCase().includes(term) ||
      b.accountHolderName.toLowerCase().includes(term) ||
      (b.nickname && b.nickname.toLowerCase().includes(term)) ||
      b.bankName.toLowerCase().includes(term)
    );
  }

  // Account Lookup on Blur/Input
  onAccountInput(): void {
    if (!this.addAccountNumber || this.addAccountNumber.trim().length < 6) return;

    this.isLookingUp = true;
    this.beneficiaryService.lookupAccount(this.addAccountNumber.trim(), this.addBankBin).subscribe({
      next: (res) => {
        this.isLookingUp = false;
        if (res.data) {
          this.addAccountHolderName = res.data.accountHolderName;
          this.addBankName = res.data.bankName;
        }
      },
      error: () => {
        this.isLookingUp = false;
      }
    });
  }

  onBankChange(): void {
    const selected = this.banks.find(b => b.bin === this.addBankBin);
    if (selected) {
      this.addBankName = selected.name;
    }
    this.onAccountInput();
  }

  // Add Beneficiary
  saveBeneficiary(): void {
    if (!this.addAccountNumber || !this.addAccountHolderName) {
      this.errorMessage = 'Vui lòng nhập đầy đủ thông tin số tài khoản và tên người nhận';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const req: AddBeneficiaryRequest = {
      customerId: 'CUST-001',
      accountNumber: this.addAccountNumber.trim(),
      bankBin: this.addBankBin,
      bankName: this.addBankName,
      accountHolderName: this.addAccountHolderName,
      nickname: this.addNickname.trim() || undefined
    };

    this.beneficiaryService.addBeneficiary(req).subscribe({
      next: (res) => {
        this.loading = false;
        this.showAddModal = false;
        if (res.data) {
          this.successMessage = `Đã lưu thành công [${res.data.accountHolderName}] vào danh bạ!`;
          this.resetAddForm();
          this.loadBeneficiaries();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Lỗi thêm người thụ hưởng';
      }
    });
  }

  resetAddForm(): void {
    this.addAccountNumber = '';
    this.addBankBin = '970400';
    this.addBankName = 'BankX Digital Bank';
    this.addAccountHolderName = '';
    this.addNickname = '';
  }

  // Edit Nickname
  openEditModal(b: Beneficiary): void {
    this.editingBeneficiary = b;
    this.editNickname = b.nickname || '';
    this.showEditModal = true;
  }

  saveNickname(): void {
    if (!this.editingBeneficiary || !this.editNickname.trim()) return;

    this.loading = true;
    this.beneficiaryService.updateNickname(this.editingBeneficiary.id, this.editNickname.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.showEditModal = false;
        if (res.data) {
          this.successMessage = 'Đã cập nhật biệt danh thành công!';
          this.loadBeneficiaries();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Cập nhật biệt danh thất bại';
      }
    });
  }

  // Delete Beneficiary
  deleteBeneficiary(b: Beneficiary): void {
    if (!confirm(`Bạn có chắc chắn muốn xóa [${b.nickname || b.accountHolderName}] khỏi danh bạ?`)) return;

    this.loading = true;
    this.beneficiaryService.deleteBeneficiary(b.id).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Đã xóa người thụ hưởng khỏi danh bạ';
        this.loadBeneficiaries();
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Xóa người thụ hưởng thất bại';
      }
    });
  }

  // Quick Transfer to Beneficiary
  quickTransfer(b: Beneficiary): void {
    this.router.navigate(['/transfers'], {
      queryParams: {
        targetAccount: b.accountNumber,
        targetName: b.accountHolderName
      }
    });
  }
}
