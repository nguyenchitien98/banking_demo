import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface AdminKpiResponse {
  totalCustomers: number;
  totalAccounts: number;
  totalSystemBalance: number;
  pendingKycCount: number;
  todayTransactionCount: number;
  frozenAccountCount: number;
  highRiskAlertCount: number;
}

export interface AdminCustomerResponse {
  id: string;
  username: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  identityNumber: string;
  kycStatus: string;
  accountStatus: string;
  createdAt: string;
}

export interface AdminAuditLogResponse {
  id: string;
  adminUsername: string;
  adminRole: string;
  actionType: string;
  targetId: string;
  details: string;
  createdAt: string;
}

export interface ReviewKycRequest {
  status: string;
  reason: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = '/api/v1/admin';

  constructor(private http: HttpClient) {}

  /**
   * Lấy danh số KPI cho Admin Dashboard.
   */
  getDashboardKpis(): Observable<ApiResponse<AdminKpiResponse>> {
    return this.http.get<ApiResponse<AdminKpiResponse>>(`${this.baseUrl}/dashboard/kpis`);
  }

  /**
   * Lấy danh sách tất cả khách hàng.
   */
  listCustomers(): Observable<ApiResponse<AdminCustomerResponse[]>> {
    return this.http.get<ApiResponse<AdminCustomerResponse[]>>(`${this.baseUrl}/customers`);
  }

  /**
   * Phê duyệt / Từ chối eKYC cho khách hàng.
   */
  reviewKyc(customerId: string, request: ReviewKycRequest): Observable<ApiResponse<AdminCustomerResponse>> {
    return this.http.post<ApiResponse<AdminCustomerResponse>>(`${this.baseUrl}/customers/${customerId}/kyc`, request);
  }

  /**
   * Khóa / Đóng băng tài khoản khẩn cấp.
   */
  freezeAccount(accountId: string, reason: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/accounts/${accountId}/freeze?reason=${encodeURIComponent(reason)}`, {});
  }

  /**
   * Lấy nhật ký kiểm toán thao tác quản trị.
   */
  getAuditLogs(): Observable<ApiResponse<AdminAuditLogResponse[]>> {
    return this.http.get<ApiResponse<AdminAuditLogResponse[]>>(`${this.baseUrl}/audit-logs`);
  }
}
