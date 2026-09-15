import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface LedgerEntry {
  id: string;
  transactionId: string;
  accountId: string;
  entryType: 'DEBIT' | 'CREDIT';
  amount: number;
  currency: string;
  balanceAfter: number;
  createdAt: string;
}

export interface TransactionDetail {
  id: string;
  transactionReference: string;
  transactionType: string;
  status: string;
  amount: number;
  currency: string;
  description: string;
  createdAt: string;
  entries: LedgerEntry[];
}

export interface RecordDoubleEntryRequest {
  debitAccountId: string;
  creditAccountId: string;
  amount: number;
  description?: string;
  type?: string;
}

/**
 * Service quản lý Bút toán Ghi sổ kép & Lịch sử Giao dịch (Ledger Service).
 */
@Injectable({
  providedIn: 'root',
})
export class LedgerService {
  private readonly API_URL = '/api/v1';
  private readonly http = inject(HttpClient);

  /**
   * Lấy danh sách bút toán ghi sổ của một tài khoản
   */
  getAccountTransactions(accountId: string, limit: number = 20): Observable<ApiResponse<LedgerEntry[]>> {
    return this.http.get<ApiResponse<LedgerEntry[]>>(`${this.API_URL}/accounts/${accountId}/transactions?limit=${limit}`);
  }

  /**
   * Lấy chi tiết giao dịch kèm danh sách bút toán ghi sổ kép
   */
  getTransactionDetail(transactionId: string): Observable<ApiResponse<TransactionDetail>> {
    return this.http.get<ApiResponse<TransactionDetail>>(`${this.API_URL}/transactions/${transactionId}`);
  }

  /**
   * Thử nghiệm hạch toán bút toán ghi sổ kép
   */
  recordDoubleEntry(request: RecordDoubleEntryRequest): Observable<ApiResponse<TransactionDetail>> {
    return this.http.post<ApiResponse<TransactionDetail>>(`${this.API_URL}/ledger/record`, request);
  }
}
