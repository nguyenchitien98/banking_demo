import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface MetricsSummary {
  transferCompletedTotal: number;
  transferFailedTotal: number;
  fraudAlertHighTotal: number;
  otpAttemptsTotal: number;
  activeSessionsGauge: number;
  p99LatencyMs: number;
  redisHitRatePercentage: number;
  kafkaConsumerLag: number;
  jvmMemoryUsedMb: number;
  dbConnectionPoolActive: number;
}

@Injectable({
  providedIn: 'root'
})
export class MonitoringService {
  private readonly baseUrl = '/api/v1/metrics';

  constructor(private http: HttpClient) {}

  /**
   * Lấy dữ liệu tổng hợp Prometheus metrics.
   */
  getMetricsSummary(): Observable<ApiResponse<MetricsSummary>> {
    return this.http.get<ApiResponse<MetricsSummary>>(`${this.baseUrl}/prometheus-summary`);
  }

  /**
   * Giả lập gửi traffic tạo metrics cho Grafana.
   */
  simulateTraffic(count: number = 10): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/simulate?count=${count}`, {});
  }
}
