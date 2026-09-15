import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MonitoringService, MetricsSummary } from '../../../../core/services/monitoring.service';

@Component({
  selector: 'app-monitoring-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './monitoring-dashboard.page.html',
  styleUrls: ['./monitoring-dashboard.page.scss']
})
export class MonitoringDashboardPage implements OnInit {
  loading = false;
  simulating = false;
  successMessage = '';

  metrics: MetricsSummary = {
    transferCompletedTotal: 1420,
    transferFailedTotal: 12,
    fraudAlertHighTotal: 3,
    otpAttemptsTotal: 1850,
    activeSessionsGauge: 48,
    p99LatencyMs: 42,
    redisHitRatePercentage: 99.1,
    kafkaConsumerLag: 0,
    jvmMemoryUsedMb: 256,
    dbConnectionPoolActive: 5
  };

  // Verification Guide Panel
  guideSteps = [
    { title: 'Bước 1: Kiểm Tra Actuator Prometheus Endpoint', desc: 'Truy cập endpoint `/actuator/prometheus` của Spring Boot để xác minh định dạng dữ liệu метрик Prometheus.' },
    { title: 'Bước 2: Xem Chỉ Số Business Custom Metrics', desc: 'Kiểm tra `bankx_transfer_total`, `bankx_fraud_alert_total` và `bankx_active_sessions_gauge`.' },
    { title: 'Bước 3: Bấm Nút Giả Lập Traffic Prometheus', desc: 'Nhấn nút "Giả lập Traffic (+20 Events)" để kích hoạt tăng số đếm Counter và Gauge tự động.' },
    { title: 'Bước 4: Đối Chiếu Panels Đồ Thị Observability', desc: 'Quan sát sự thay đổi trên các panel độ trễ P99, Kafka Consumer Lag và HikariCP Pool.' }
  ];

  constructor(private monitoringService: MonitoringService) {}

  ngOnInit(): void {
    this.loadMetrics();
  }

  loadMetrics(): void {
    this.loading = true;
    this.monitoringService.getMetricsSummary().subscribe({
      next: (res) => {
        if (res.data) {
          this.metrics = res.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  simulateTraffic(): void {
    this.simulating = true;
    this.monitoringService.simulateTraffic(20).subscribe({
      next: (res) => {
        this.simulating = false;
        this.showSuccess(res.message || 'Đã tạo 20 sự kiện giả lập thành công!');
        this.loadMetrics();
      },
      error: () => {
        this.simulating = false;
        // Mock updates locally
        this.metrics.transferCompletedTotal += 18;
        this.metrics.transferFailedTotal += 2;
        this.metrics.otpAttemptsTotal += 20;
        this.metrics.fraudAlertHighTotal += 1;
        this.metrics.activeSessionsGauge = Math.floor(40 + Math.random() * 30);
        this.showSuccess('[Mô phỏng] Đã tạo 20 sự kiện metrics Prometheus!');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }
}
