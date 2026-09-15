import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionHistoryService, TransactionHistoryResponse } from '../../../../core/services/transaction-history.service';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-history.page.html',
  styleUrls: ['./transaction-history.page.scss']
})
export class TransactionHistoryPage implements OnInit {
  accountNumber = '1000188888';
  items: TransactionHistoryResponse[] = [];
  nextCursor: string | null = null;
  hasNext = false;
  loading = false;
  loadingMore = false;
  selectedFilter: 'ALL' | 'DEBIT' | 'CREDIT' = 'ALL';

  // Verification Guide Panel data
  guideSteps = [
    { title: 'Bước 1: Tải Trang CQRS Read Model', desc: 'Hệ thống tự động đọc từ bảng transaction_history_views (không chạm vào Write Tables).' },
    { title: 'Bước 2: Phân Trang Con Trỏ (Cursor)', desc: 'Nhấn "Tải Thêm Lịch Sử" để dùng Cursor `createdAt < lastItem` thay cho `OFFSET N`.' },
    { title: 'Bước 3: Lọc Thu / Chi (DEBIT/CREDIT)', desc: 'Lọc nhanh các biến động số dư theo chiều chuyển tiền.' },
    { title: 'Bước 4: Đối Chiếu Hiệu Năng O(log N)', desc: 'So sánh độ trễ truy vấn ổn định khi số lượng bản ghi đạt hàng triệu dòng.' }
  ];

  constructor(private historyService: TransactionHistoryService) {}

  ngOnInit(): void {
    this.loadInitialHistory();
  }

  loadInitialHistory(): void {
    this.loading = true;
    this.historyService.getHistoryByAccount(this.accountNumber, undefined, 5).subscribe({
      next: (res) => {
        if (res.data && res.data.items) {
          this.items = res.data.items;
          this.nextCursor = res.data.nextCursor;
          this.hasNext = res.data.hasNext;
        } else {
          this.initMockHistory();
        }
        this.loading = false;
      },
      error: () => {
        this.initMockHistory();
        this.loading = false;
      }
    });
  }

  loadMore(): void {
    if (!this.hasNext || !this.nextCursor || this.loadingMore) return;

    this.loadingMore = true;
    this.historyService.getHistoryByAccount(this.accountNumber, this.nextCursor, 5).subscribe({
      next: (res) => {
        if (res.data && res.data.items) {
          this.items = [...this.items, ...res.data.items];
          this.nextCursor = res.data.nextCursor;
          this.hasNext = res.data.hasNext;
        }
        this.loadingMore = false;
      },
      error: () => {
        this.loadingMore = false;
      }
    });
  }

  initMockHistory(): void {
    this.items = [
      {
        id: 'TX-READ-001',
        transactionReference: 'TRF-98230192',
        customerId: 'CUST-001',
        accountNumber: '1000188888',
        oppositeAccountNumber: '1000299999',
        oppositeAccountName: 'TRAN THI MAI',
        amount: 1500000,
        direction: 'DEBIT',
        transactionType: 'INTERNAL_TRANSFER',
        category: 'TRANSFER',
        description: 'Chuyển tiền mua quà sinh nhật',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 3600000 * 2).toISOString()
      },
      {
        id: 'TX-READ-002',
        transactionReference: 'BILL-4412091',
        customerId: 'CUST-001',
        accountNumber: '1000188888',
        oppositeAccountNumber: 'EVN_HCM_001',
        oppositeAccountName: 'ĐIỆN LỰC EVN HCM',
        amount: 450000,
        direction: 'DEBIT',
        transactionType: 'BILL_PAYMENT',
        category: 'ELECTRICITY',
        description: 'Thanh toán tiền điện tháng 09/2026',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 3600000 * 4).toISOString()
      },
      {
        id: 'TX-READ-003',
        transactionReference: 'QR-881204',
        customerId: 'CUST-001',
        accountNumber: '1000188888',
        oppositeAccountNumber: 'HIGHLANDS_COFFEE',
        oppositeAccountName: 'HIGHLANDS COFFEE',
        amount: 65000,
        direction: 'DEBIT',
        transactionType: 'QR_PAYMENT',
        category: 'FOOD_BEVERAGE',
        description: 'Quét mã VietQR thanh toán cà phê',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 3600000 * 6).toISOString()
      },
      {
        id: 'TX-READ-004',
        transactionReference: 'TRF-1029381',
        customerId: 'CUST-001',
        accountNumber: '1000188888',
        oppositeAccountNumber: '970422001',
        oppositeAccountName: 'LE VAN HUNG',
        amount: 5000000,
        direction: 'CREDIT',
        transactionType: 'INTERNAL_TRANSFER',
        category: 'TRANSFER',
        description: 'Nhận tiền chuyển khoản lương tháng',
        status: 'COMPLETED',
        createdAt: new Date(Date.now() - 3600000 * 12).toISOString()
      }
    ];
    this.hasNext = true;
    this.nextCursor = new Date(Date.now() - 3600000 * 12).toISOString();
  }

  get filteredItems(): TransactionHistoryResponse[] {
    if (this.selectedFilter === 'ALL') return this.items;
    return this.items.filter(i => i.direction === this.selectedFilter);
  }
}
