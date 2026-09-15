import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface PaymentProvider {
  id: string;
  code: string;
  name: string;
  category: string;
  logoUrl?: string;
  status: string;
}

export interface BillInquiry {
  customerBillCode: string;
  customerName: string;
  providerCode: string;
  providerName: string;
  amount: number;
  fee: number;
  period: string;
  description: string;
}

export interface BillPayment {
  id: string;
  paymentCode: string;
  sourceAccountId: string;
  providerCode: string;
  customerBillCode: string;
  customerName: string;
  amount: number;
  fee: number;
  period: string;
  status: string;
  transactionId: string;
  createdAt: string;
}

export interface PayBillRequest {
  sourceAccountId: string;
  providerCode: string;
  customerBillCode: string;
  amount: number;
  idempotencyKey?: string;
}

/**
 * Service quản lý Thanh toán Hóa đơn Dịch vụ & Strategy Pattern (Payment Service).
 */
@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private readonly API_URL = '/api/v1/payments';
  private readonly http = inject(HttpClient);

  /**
   * Sinh mã Idempotency Key ngẫu nhiên chuẩn UUID v4
   */
  generateIdempotencyKey(): string {
    return crypto.randomUUID();
  }

  /**
   * Lấy danh sách các nhà cung cấp dịch vụ thanh toán
   */
  getProviders(): Observable<ApiResponse<PaymentProvider[]>> {
    return this.http.get<ApiResponse<PaymentProvider[]>>(`${this.API_URL}/providers`);
  }

  /**
   * Tra cứu thông tin hóa đơn và nợ cước
   */
  inquireBill(providerCode: string, billNumber: string): Observable<ApiResponse<BillInquiry>> {
    return this.http.get<ApiResponse<BillInquiry>>(`${this.API_URL}/bills?providerCode=${providerCode}&billNumber=${billNumber}`);
  }

  /**
   * Thực hiện thanh toán hóa đơn với Idempotency Key
   */
  payBill(request: PayBillRequest): Observable<ApiResponse<BillPayment>> {
    let headers = new HttpHeaders();
    if (request.idempotencyKey) {
      headers = headers.set('X-Idempotency-Key', request.idempotencyKey);
    }

    const payload = {
      sourceAccountId: request.sourceAccountId,
      providerCode: request.providerCode,
      customerBillCode: request.customerBillCode,
      amount: request.amount
    };

    return this.http.post<ApiResponse<BillPayment>>(`${this.API_URL}/bills`, payload, { headers });
  }

  /**
   * Lấy lịch sử thanh toán hóa đơn
   */
  getHistory(accountId: string): Observable<ApiResponse<BillPayment[]>> {
    return this.http.get<ApiResponse<BillPayment[]>>(`${this.API_URL}/history?accountId=${accountId}`);
  }
}
