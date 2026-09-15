import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface EvaluateTransactionRequest {
  sourceAccountId: string;
  targetAccountNumber?: string;
  amount: number;
  isNewDevice: boolean;
  isNewBeneficiary: boolean;
  velocityLastMinute: number;
  customHour?: number;
}

export interface FraudEvaluationResult {
  riskScore: number;
  riskAction: 'ALLOW' | 'OTP_REQUIRED' | 'BLOCK';
  triggeredRules: string[];
  summaryDetail: string;
}

export interface FraudAlert {
  id: string;
  transactionId: string;
  sourceAccountId: string;
  targetAccountNumber: string;
  amount: number;
  riskScore: number;
  riskAction: 'ALLOW' | 'OTP_REQUIRED' | 'BLOCK' | string;
  triggeredRules: string;
  status: 'PENDING_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE' | string;
  reviewerNotes?: string;
  createdAt: string;
}

export interface FraudRule {
  id: string;
  ruleCode: string;
  ruleName: string;
  description: string;
  weightScore: number;
  isActive: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class FraudService {
  private readonly baseUrl = '/api/v1/fraud';

  constructor(private http: HttpClient) {}

  /**
   * Đánh giá rủi ro giao dịch real-time.
   */
  evaluateTransaction(request: EvaluateTransactionRequest): Observable<ApiResponse<FraudEvaluationResult>> {
    return this.http.post<ApiResponse<FraudEvaluationResult>>(`${this.baseUrl}/evaluate`, request);
  }

  /**
   * Lấy danh sách Cảnh báo Gian lận.
   */
  getAlerts(): Observable<ApiResponse<FraudAlert[]>> {
    return this.http.get<ApiResponse<FraudAlert[]>>(`${this.baseUrl}/alerts`);
  }

  /**
   * Duyệt / Xử lý Cảnh báo Gian lận.
   */
  reviewAlert(id: string, newStatus: 'RESOLVED' | 'FALSE_POSITIVE', reviewerNotes?: string): Observable<ApiResponse<FraudAlert>> {
    return this.http.patch<ApiResponse<FraudAlert>>(`${this.baseUrl}/alerts/${id}/review`, { newStatus, reviewerNotes });
  }

  /**
   * Lấy danh sách Quy tắc Gian lận.
   */
  getRules(): Observable<ApiResponse<FraudRule[]>> {
    return this.http.get<ApiResponse<FraudRule[]>>(`${this.baseUrl}/rules`);
  }

  /**
   * Bật / Tắt Quy tắc Gian lận.
   */
  toggleRule(id: string, isActive: boolean): Observable<ApiResponse<FraudRule>> {
    return this.http.patch<ApiResponse<FraudRule>>(`${this.baseUrl}/rules/${id}/toggle?isActive=${isActive}`, {});
  }
}
