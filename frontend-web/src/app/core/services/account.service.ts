import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface BankAccount {
  id: string;
  customerId: string;
  accountNumber: string;
  accountName: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | string;
  version: number;
}

export interface CreateAccountRequest {
  accountName: string;
  initialBalance?: number;
}

/**
 * Service quản lý Tài khoản thanh toán (Bank Account Service).
 * Tương tác với REST API /api/v1/accounts
 */
@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly API_URL = '/api/v1/accounts';
  private readonly http = inject(HttpClient);

  /**
   * Lấy danh sách tài khoản thuộc sở hữu của người dùng hiện tại
   */
  getMyAccounts(): Observable<ApiResponse<BankAccount[]>> {
    return this.http.get<ApiResponse<BankAccount[]>>(this.API_URL);
  }

  /**
   * Mở tài khoản thanh toán mới
   */
  createAccount(request: CreateAccountRequest): Observable<ApiResponse<BankAccount>> {
    return this.http.post<ApiResponse<BankAccount>>(this.API_URL, request);
  }

  /**
   * Phong tỏa tài khoản
   */
  freezeAccount(accountId: string): Observable<ApiResponse<BankAccount>> {
    return this.http.patch<ApiResponse<BankAccount>>(`${this.API_URL}/${accountId}/freeze`, {});
  }
}
