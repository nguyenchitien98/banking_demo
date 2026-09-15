import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface EngineeringHealthResponse {
  systemStatus: 'HEALTHY' | 'DEGRADED' | 'CRITICAL' | string;
  serviceHealthGrid: { [key: string]: string };
  currentTps: number;
  errorRatePercentage: number;
  p99LatencyMs: number;
  kafkaConsumerLag: number;
  redisHitRatePercentage: number;
  dbPoolActive: number;
  dbPoolMax: number;
  circuitBreakerState: string;
  dbDelayActive: boolean;
  kafkaDownSimulated: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EngineeringService {
  private readonly baseUrl = '/api/v1/engineering';

  constructor(private http: HttpClient) {}

  /**
   * Lấy dữ liệu Telemetry sức khỏe toàn bộ hệ thống (System Health Grid).
   */
  getHealthSummary(): Observable<ApiResponse<EngineeringHealthResponse>> {
    return this.http.get<ApiResponse<EngineeringHealthResponse>>(`${this.baseUrl}/health-summary`);
  }

  /**
   * Chaos Action 1: Giả lập độ trễ Database.
   */
  delayDb(delayMs: number = 2000): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/chaos/delay-db?delayMs=${delayMs}`, {});
  }

  /**
   * Chaos Action 2: Giả lập ngắt kết nối Kafka Consumer/Producer.
   */
  toggleKafka(isDown: boolean): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/chaos/toggle-kafka?isDown=${isDown}`, {});
  }

  /**
   * Chaos Action 3: Tải cao dồn dập 100 requests chuyển tiền đồng thời (Flood Transfer).
   */
  floodTransfer(count: number = 100): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/chaos/flood-transfer?count=${count}`, {});
  }
}
