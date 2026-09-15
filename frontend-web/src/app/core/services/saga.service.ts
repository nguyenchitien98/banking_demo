import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface SagaExecutionRequest {
  sourceAccountNumber: string;
  targetAccountNumber: string;
  amount: number;
  description: string;
  forceFailureStep?: 'NONE' | 'CREDIT_FAILED' | 'LEDGER_FAILED';
}

export interface SagaAuditStepResponse {
  id: string;
  sagaId: string;
  stepName: string;
  stateBefore: string;
  stateAfter: string;
  compensating: boolean;
  details: string;
  createdAt: string;
}

export interface SagaInstanceResponse {
  sagaId: string;
  transferCode: string;
  currentState: string;
  sourceAccountNumber: string;
  targetAccountNumber: string;
  amount: number;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
  steps: SagaAuditStepResponse[];
}

@Injectable({
  providedIn: 'root'
})
export class SagaService {
  private readonly baseUrl = '/api/v1/sagas';

  constructor(private http: HttpClient) {}

  /**
   * Khởi chạy Saga Chuyển tiền phân tán.
   */
  executeSaga(request: SagaExecutionRequest): Observable<ApiResponse<SagaInstanceResponse>> {
    return this.http.post<ApiResponse<SagaInstanceResponse>>(`${this.baseUrl}/execute`, request);
  }

  /**
   * Lấy chi tiết Saga Instance.
   */
  getSagaDetails(sagaId: string): Observable<ApiResponse<SagaInstanceResponse>> {
    return this.http.get<ApiResponse<SagaInstanceResponse>>(`${this.baseUrl}/${sagaId}`);
  }

  /**
   * Lấy danh sách các Saga Instances.
   */
  listSagas(): Observable<ApiResponse<SagaInstanceResponse[]>> {
    return this.http.get<ApiResponse<SagaInstanceResponse[]>>(`${this.baseUrl}`);
  }
}
