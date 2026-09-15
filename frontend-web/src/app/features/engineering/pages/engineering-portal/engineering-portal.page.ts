import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngineeringService, EngineeringHealthResponse } from '../../../../core/services/engineering.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-engineering-portal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './engineering-portal.page.html',
  styleUrls: ['./engineering-portal.page.scss']
})
export class EngineeringPortalPage implements OnInit, OnDestroy {
  healthData: EngineeringHealthResponse | null = null;
  loading: boolean = false;
  actionMessage: string = '';
  actionError: string = '';
  autoRefreshSub: Subscription | null = null;

  constructor(private engineeringService: EngineeringService) {}

  ngOnInit(): void {
    this.fetchHealthSummary();
    // Auto-refresh telemetry every 3 seconds for live dashboard feel
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.fetchHealthSummary(true);
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  fetchHealthSummary(silent: boolean = false): void {
    if (!silent) this.loading = true;
    this.engineeringService.getHealthSummary().subscribe({
      next: (res) => {
        if (res.data) {
          this.healthData = res.data;
        }
        this.loading = false;
      },
      error: (err) => {
        this.actionError = 'Không thể tải dữ liệu telemetry: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  triggerDelayDb(): void {
    const nextDelay = this.healthData?.dbDelayActive ? 0 : 2000;
    this.loading = true;
    this.actionMessage = '';
    this.actionError = '';

    this.engineeringService.delayDb(nextDelay).subscribe({
      next: (res) => {
        this.actionMessage = res.message || 'Đã cập nhật giả lập độ trễ Database!';
        this.fetchHealthSummary();
      },
      error: (err) => {
        this.actionError = 'Lỗi kích hoạt Chaos DB: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  triggerToggleKafka(): void {
    const nextState = !this.healthData?.kafkaDownSimulated;
    this.loading = true;
    this.actionMessage = '';
    this.actionError = '';

    this.engineeringService.toggleKafka(nextState).subscribe({
      next: (res) => {
        this.actionMessage = res.message || 'Đã thay đổi trạng thái Kafka!';
        this.fetchHealthSummary();
      },
      error: (err) => {
        this.actionError = 'Lỗi kích hoạt Chaos Kafka: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  triggerFloodTransfer(): void {
    this.loading = true;
    this.actionMessage = '';
    this.actionError = '';

    this.engineeringService.floodTransfer(100).subscribe({
      next: (res) => {
        this.actionMessage = res.message || 'Đã gửi 100 requests giao dịch dồn dập!';
        this.fetchHealthSummary();
      },
      error: (err) => {
        this.actionError = 'Lỗi kích hoạt Flood Transfer: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  getServiceKeys(): string[] {
    return this.healthData?.serviceHealthGrid ? Object.keys(this.healthData.serviceHealthGrid) : [];
  }
}
