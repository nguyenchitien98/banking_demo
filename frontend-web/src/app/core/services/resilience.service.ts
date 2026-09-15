import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface ResilienceStatus {
  circuitState: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | string;
  failureRatePercentage: number;
  numberOfFailedCalls: number;
  numberOfSuccessfulCalls: number;
  numberOfBufferedCalls: number;
  fallbackExecutionCount: number;
  externalBankDown: boolean;
  externalBankLatencyMs: number;
}

@Injectable({
  providedIn: 'root'
})
export class ResilienceService {
  private readonly baseUrl = '/api/v1/resilience';

  constructor(private http: HttpClient) {}

  /**
   * Gọi API Chuyển tiền liên ngân hàng được bảo vệ bởi Circuit Breaker.
   */
  executeTransfer(accountNumber: string = '970422001999', amount: number = 500000): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/interbank/transfer?accountNumber=${accountNumber}&amount=${amount}`, {});
  }

  /**
   * Lấy trạng thái Circuit Breaker Resilience4j.
   */
  getStatus(): Observable<ApiResponse<ResilienceStatus>> {
    return this.http.get<ApiResponse<ResilienceStatus>>(`${this.baseUrl}/status`);
  }

  /**
   * Đổi giả lập đối tác liên ngân hàng bị DOWN hoặc SLOW.
   */
  toggleSimulation(isDown: boolean, latencyMs: number = 0): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/simulate/toggle-external-bank?isDown=${isDown}&latencyMs=${latencyMs}`, {});
  }

  /**
   * Reset Circuit Breaker về CLOSED.
   */
  resetCircuitBreaker(): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/reset`, {});
  }
}
