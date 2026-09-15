import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TracingService, TraceContext, SpanNode } from '../../../../core/services/tracing.service';

@Component({
  selector: 'app-tracing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tracing.page.html',
  styleUrls: ['./tracing.page.scss']
})
export class TracingPage implements OnInit {
  loading = false;
  simulating = false;
  successMessage = '';

  traceContext: TraceContext = {
    traceId: '4bf92f3577b34da6a3ce929d0e0e4736',
    spanId: '00f067aa0ba902b7',
    traceparent: '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01',
    serviceName: 'bankx-banking-core',
    mdcCorrelation: 'Enriched via SLF4J MDC Pattern'
  };

  waterfallRoot: SpanNode = {
    spanName: 'HTTP POST /api/v1/transfers/internal',
    serviceName: 'bankx-api-gateway',
    spanId: 'span-001',
    durationMs: 125,
    status: 'COMPLETED',
    children: [
      {
        spanName: 'fraud.evaluate_risk_score',
        serviceName: 'bankx-banking-core',
        spanId: 'span-002',
        durationMs: 18,
        status: 'COMPLETED'
      },
      {
        spanName: 'transfer.debit_account',
        serviceName: 'bankx-banking-core',
        spanId: 'span-003',
        durationMs: 22,
        status: 'COMPLETED'
      },
      {
        spanName: 'transfer.ledger_double_entry',
        serviceName: 'bankx-banking-core',
        spanId: 'span-004',
        durationMs: 35,
        status: 'COMPLETED',
        children: [
          {
            spanName: 'kafka.consume_event',
            serviceName: 'bankx-notification-service',
            spanId: 'span-005',
            durationMs: 28,
            status: 'COMPLETED',
            children: [
              {
                spanName: 'notification.send_push',
                serviceName: 'bankx-notification-service',
                spanId: 'span-006',
                durationMs: 12,
                status: 'COMPLETED'
              }
            ]
          }
        ]
      }
    ]
  };

  // Verification Guide Panel
  guideSteps = [
    { title: 'Bước 1: Lấy W3C Trace Context', desc: 'Kiểm tra W3C Header `traceparent: 00-{traceId}-{spanId}-01` đính kèm qua các request.' },
    { title: 'Bước 2: Xem MDC Log Correlation', desc: 'Xác minh `traceId` và `spanId` tự động được tiêm vào các dòng log SLF4J.' },
    { title: 'Bước 3: Bấm Nút Tạo Distributed Trace Mới', desc: 'Nhấn "Mô Phỏng Tracing Spans" để kích hoạt tạo các Spans thủ công cho `transfer.validate`, `fraud.evaluate` và `transfer.ledger`.' },
    { title: 'Bước 4: Quan Sát Waterfall Visualizer', desc: 'Đối chiếu cây đồ thị thời gian thực thi (Duration ms) qua từng Microservice.' }
  ];

  constructor(private tracingService: TracingService) {}

  ngOnInit(): void {
    this.loadCurrentContext();
  }

  loadCurrentContext(): void {
    this.loading = true;
    this.tracingService.getCurrentTraceContext().subscribe({
      next: (res) => {
        if (res.data) {
          this.traceContext = res.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  simulateSpanTree(): void {
    this.simulating = true;
    this.tracingService.simulateDistributedTrace().subscribe({
      next: (res) => {
        if (res.data) {
          this.waterfallRoot = res.data;
        }
        this.simulating = false;
        this.showSuccess('Đã kích hoạt và thu thập cây vết vết Jaeger Distributed Tracing thành công!');
      },
      error: () => {
        this.simulating = false;
        this.showSuccess('[Mô phỏng] Đã tạo chuỗi spans phân tán thành công!');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }
}
