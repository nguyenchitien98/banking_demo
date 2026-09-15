import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface TransactionHistoryResponse {
  id: string;
  transactionReference: string;
  customerId: string;
  accountNumber: string;
  oppositeAccountNumber: string;
  oppositeAccountName: string;
  amount: number;
  direction: 'DEBIT' | 'CREDIT';
  transactionType: string;
  category: string;
  description: string;
  status: string;
  createdAt: string;
}

export interface CursorPageResponse<T> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
  pageSize: number;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionHistoryService {
  private readonly baseUrl = '/api/v1/cqrs/history';

  constructor(private http: HttpClient) {}

  /**
   * Truy vấn Lịch sử Giao dịch theo số tài khoản bằng phân trang con trỏ (Cursor Pagination).
   */
  getHistoryByAccount(accountNumber: string, cursor?: string, size: number = 10): Observable<ApiResponse<CursorPageResponse<TransactionHistoryResponse>>> {
    let url = `${this.baseUrl}/accounts/${accountNumber}?size=${size}`;
    if (cursor) {
      url += `&cursor=${encodeURIComponent(cursor)}`;
    }
    return this.http.get<ApiResponse<CursorPageResponse<TransactionHistoryResponse>>>(url);
  }

  /**
   * Truy vấn Lịch sử Giao dịch theo Khách hàng.
   */
  getHistoryByCustomer(customerId: string = 'CUST-001', limit: number = 20): Observable<ApiResponse<TransactionHistoryResponse[]>> {
    return this.http.get<ApiResponse<TransactionHistoryResponse[]>>(`${this.baseUrl}/customers/${customerId}?limit=${limit}`);
  }
}
