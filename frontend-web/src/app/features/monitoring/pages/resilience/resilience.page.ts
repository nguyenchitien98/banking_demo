import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ResilienceService, ResilienceStatus } from '../../../../core/services/resilience.service';

@Component({
  selector: 'app-resilience',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resilience.page.html',
  styleUrls: ['./resilience.page.scss']
})
export class ResiliencePage implements OnInit {
  loading = false;
  executing = false;
  successMessage = '';
  errorMessage = '';

  status: ResilienceStatus = {
    circuitState: 'CLOSED',
    failureRatePercentage: 0,
    numberOfFailedCalls: 0,
    numberOfSuccessfulCalls: 15,
    numberOfBufferedCalls: 15,
    fallbackExecutionCount: 0,
    externalBankDown: false,
    externalBankLatencyMs: 0
  };

  lastExecutionResult = '';

  // Verification Guide Panel
  guideSteps = [
    { title: 'Bước 1: Trạng Thái Ban Đầu (CLOSED 🟢)', desc: 'Xác minh Circuit Breaker ở trạng thái CLOSED, cho phép gọi thẳng Cổng Liên ngân hàng.' },
    { title: 'Bước 2: Kích Hoạt Giả Lập Lỗi (Partner DOWN)', desc: 'Nhấn "Bật Cổng Liên Ngân Hàng DOWN". Gửi 5 giao dịch liên tiếp để nâng Failure Rate > 50%.' },
    { title: 'Bước 3: Tự Động Chuyển Trạng Thái OPEN 🔴', desc: 'Circuit Breaker tự động chuyển sang OPEN. Các request tiếp theo sẽ bị Reject ngay lập tức và chuyển sang Fallback Safe Mode.' },
    { title: 'Bước 4: Reset & Tự Động Khôi Phục (HALF_OPEN 🟡)', desc: 'Sau 10s (hoặc nhấn Reset), Circuit Breaker chuyển HALF_OPEN thử nghiệm 2 requests và quay về CLOSED khi thành công.' }
  ];

  constructor(private resilienceService: ResilienceService) {}

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus(): void {
    this.loading = true;
    this.resilienceService.getStatus().subscribe({
      next: (res) => {
        if (res.data) {
          this.status = res.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  sendSingleTransfer(): void {
    this.executing = true;
    this.resilienceService.executeTransfer('970422001999', 500000).subscribe({
      next: (res) => {
        this.lastExecutionResult = res.data || res.message || 'Chuyển tiền thành công';
        this.executing = false;
        this.loadStatus();
      },
      error: (err) => {
        this.lastExecutionResult = err?.error?.message || 'Giao dịch bị từ chối do Circuit Breaker / Rate Limiter';
        this.executing = false;
        this.loadStatus();
      }
    });
  }

  toggleExternalBank(isDown: boolean): void {
    this.resilienceService.toggleSimulation(isDown, 0).subscribe({
      next: (res) => {
        this.showSuccess(res.message || 'Cập nhật giả lập đối tác thành công!');
        this.loadStatus();
      },
      error: () => {
        this.status.externalBankDown = isDown;
        this.showSuccess(`[Mô phỏng] Đã chuyển Cổng Liên ngân hàng: ${isDown ? 'DOWN' : 'UP'}`);
      }
    });
  }

  resetCircuit(): void {
    this.resilienceService.resetCircuitBreaker().subscribe({
      next: (res) => {
        this.showSuccess(res.message || 'Khôi phục Circuit Breaker về CLOSED!');
        this.loadStatus();
      },
      error: () => {
        this.status.circuitState = 'CLOSED';
        this.status.fallbackExecutionCount = 0;
        this.status.failureRatePercentage = 0;
        this.showSuccess('[Mô phỏng] Khôi phục Circuit Breaker về CLOSED');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }
}
