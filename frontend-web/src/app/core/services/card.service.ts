import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface BankCard {
  id: string;
  customerId: string;
  accountNumber: string;
  cardHolderName: string;
  maskedPan: string;
  panToken: string;
  mockCvv: string;
  cardType: 'VIRTUAL_DEBIT' | 'PHYSICAL_DEBIT' | 'VIRTUAL_CREDIT' | string;
  cardBrand: 'VISA' | 'MASTERCARD' | 'NAPAS' | string;
  expiryMonth: string;
  expiryYear: string;
  spendingLimit: number;
  dailyLimit: number;
  status: 'ACTIVE' | 'FROZEN' | 'BLOCKED' | 'EXPIRED' | string;
  createdAt: string;
}

export interface CreateVirtualCardRequest {
  customerId: string;
  accountNumber: string;
  cardBrand: 'VISA' | 'MASTERCARD' | 'NAPAS';
  spendingLimit: number;
}

export interface UpdateCardLimitsRequest {
  spendingLimit: number;
  dailyLimit: number;
}

@Injectable({
  providedIn: 'root'
})
export class CardService {
  private readonly baseUrl = '/api/v1/cards';

  constructor(private http: HttpClient) {}

  /**
   * Lấy danh sách thẻ của người dùng.
   */
  getCards(customerId: string = 'CUST-001'): Observable<ApiResponse<BankCard[]>> {
    return this.http.get<ApiResponse<BankCard[]>>(`${this.baseUrl}?customerId=${customerId}`);
  }

  /**
   * Lấy thông tin chi tiết một thẻ.
   */
  getCardById(id: string): Observable<ApiResponse<BankCard>> {
    return this.http.get<ApiResponse<BankCard>>(`${this.baseUrl}/${id}`);
  }

  /**
   * Phát hành Thẻ Ảo mới.
   */
  createVirtualCard(request: CreateVirtualCardRequest): Observable<ApiResponse<BankCard>> {
    return this.http.post<ApiResponse<BankCard>>(`${this.baseUrl}/virtual`, request);
  }

  /**
   * Tạm khóa thẻ (ACTIVE -> FROZEN).
   */
  freezeCard(id: string): Observable<ApiResponse<BankCard>> {
    return this.http.patch<ApiResponse<BankCard>>(`${this.baseUrl}/${id}/freeze`, {});
  }

  /**
   * Mở khóa thẻ tạm thời (FROZEN -> ACTIVE).
   */
  unfreezeCard(id: string): Observable<ApiResponse<BankCard>> {
    return this.http.patch<ApiResponse<BankCard>>(`${this.baseUrl}/${id}/unfreeze`, {});
  }

  /**
   * Khóa vĩnh viễn / Báo mất thẻ (ACTIVE/FROZEN -> BLOCKED).
   */
  blockCard(id: string): Observable<ApiResponse<BankCard>> {
    return this.http.patch<ApiResponse<BankCard>>(`${this.baseUrl}/${id}/block`, {});
  }

  /**
   * Cập nhật hạn mức giao dịch online.
   */
  updateLimits(id: string, request: UpdateCardLimitsRequest): Observable<ApiResponse<BankCard>> {
    return this.http.patch<ApiResponse<BankCard>>(`${this.baseUrl}/${id}/limits`, request);
  }
}
