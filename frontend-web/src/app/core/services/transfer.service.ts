import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface RecipientInquiry {
  accountId: string;
  accountNumber: string;
  accountName: string;
  status: string;
}

export interface TransferResult {
  id: string;
  transferCode: string;
  sourceAccountId: string;
  targetAccountId: string;
  targetAccountNumber: string;
  targetAccountName: string;
  amount: number;
  currency: string;
  fee: number;
  description: string;
  transferType: string;
  status: string;
  transactionId: string;
  createdAt: string;
}

export interface CreateTransferRequest {
  sourceAccountId: string;
  targetAccountNumber: string;
  amount: number;
  description?: string;
}

/**
 * Service xử lý các giao dịch Chuyển tiền (Transfer Service).
 */
@Injectable({
  providedIn: 'root',
})
export class TransferService {
  private readonly API_URL = '/api/v1/transfers';
  private readonly http = inject(HttpClient);

  /**
   * Truy vấn thông tin người thụ hưởng theo số tài khoản
   */
  inquireRecipient(accountNumber: string): Observable<ApiResponse<RecipientInquiry>> {
    return this.http.get<ApiResponse<RecipientInquiry>>(`${this.API_URL}/recipient-inquiry?accountNumber=${accountNumber}`);
  }

  /**
   * Thực hiện giao dịch chuyển tiền nội bộ
   */
  createInternalTransfer(request: CreateTransferRequest): Observable<ApiResponse<TransferResult>> {
    return this.http.post<ApiResponse<TransferResult>>(`${this.API_URL}/internal`, request);
  }

  /**
   * Lấy chi tiết lệnh chuyển tiền
   */
  getTransferDetail(id: string): Observable<ApiResponse<TransferResult>> {
    return this.http.get<ApiResponse<TransferResult>>(`${this.API_URL}/${id}`);
  }
}
