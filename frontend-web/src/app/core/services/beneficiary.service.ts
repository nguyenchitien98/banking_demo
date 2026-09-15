import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface Beneficiary {
  id: string;
  customerId: string;
  accountNumber: string;
  bankBin: string;
  bankName: string;
  accountHolderName: string;
  nickname?: string;
  transferCount: number;
  lastTransferAt: string;
  createdAt: string;
}

export interface AddBeneficiaryRequest {
  customerId: string;
  accountNumber: string;
  bankBin?: string;
  bankName?: string;
  accountHolderName: string;
  nickname?: string;
}

export interface AccountLookupResponse {
  accountNumber: string;
  accountHolderName: string;
  bankBin: string;
  bankName: string;
}

@Injectable({
  providedIn: 'root'
})
export class BeneficiaryService {
  private readonly baseUrl = '/api/v1/beneficiaries';

  constructor(private http: HttpClient) {}

  /**
   * Lấy danh sách người thụ hưởng thường xuyên.
   */
  getBeneficiaries(customerId: string = 'CUST-001'): Observable<ApiResponse<Beneficiary[]>> {
    return this.http.get<ApiResponse<Beneficiary[]>>(`${this.baseUrl}?customerId=${customerId}`);
  }

  /**
   * Thêm thủ công người thụ hưởng mới.
   */
  addBeneficiary(request: AddBeneficiaryRequest): Observable<ApiResponse<Beneficiary>> {
    return this.http.post<ApiResponse<Beneficiary>>(this.baseUrl, request);
  }

  /**
   * Cập nhật biệt danh (nickname).
   */
  updateNickname(id: string, nickname: string): Observable<ApiResponse<Beneficiary>> {
    return this.http.put<ApiResponse<Beneficiary>>(`${this.baseUrl}/${id}`, { nickname });
  }

  /**
   * Xóa người thụ hưởng khỏi danh bạ.
   */
  deleteBeneficiary(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  /**
   * Tra cứu tên tài khoản nhận tiền.
   */
  lookupAccount(accountNumber: string, bankBin: string = '970400'): Observable<ApiResponse<AccountLookupResponse>> {
    return this.http.get<ApiResponse<AccountLookupResponse>>(`${this.baseUrl}/lookup?accountNumber=${accountNumber}&bankBin=${bankBin}`);
  }
}
