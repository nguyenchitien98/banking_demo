import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface OutboxEvent {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  payload: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  retryCount: number;
  errorMessage?: string;
  createdAt: string;
  processedAt?: string;
}

export interface ChaosStatusResponse {
  kafkaDisabled: boolean;
  statusMessage?: string;
}

/**
 * Service quản lý và giám sát Transactional Outbox Events & Chaos Simulation (Outbox Service).
 */
@Injectable({
  providedIn: 'root',
})
export class OutboxService {
  private readonly API_URL = '/api/v1/outbox';
  private readonly http = inject(HttpClient);

  /**
   * Lấy danh sách sự kiện Outbox gần đây
   */
  getRecentEvents(): Observable<ApiResponse<OutboxEvent[]>> {
    return this.http.get<ApiResponse<OutboxEvent[]>>(`${this.API_URL}/events`);
  }

  /**
   * Kích hoạt thử lại thủ công một event outbox bị lỗi
   */
  retryEvent(id: string): Observable<ApiResponse<OutboxEvent>> {
    return this.http.post<ApiResponse<OutboxEvent>>(`${this.API_URL}/events/${id}/retry`, {});
  }

  /**
   * Bật/tắt kịch bản Chaos Simulation ngắt kết nối Kafka (Kafka DOWN vs Kafka UP)
   */
  toggleChaosKafka(): Observable<ApiResponse<ChaosStatusResponse>> {
    return this.http.post<ApiResponse<ChaosStatusResponse>>(`${this.API_URL}/chaos/toggle-kafka`, {});
  }

  /**
   * Lấy trạng thái Chaos Simulator
   */
  getChaosStatus(): Observable<ApiResponse<ChaosStatusResponse>> {
    return this.http.get<ApiResponse<ChaosStatusResponse>>(`${this.API_URL}/chaos/status`);
  }
}
