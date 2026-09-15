import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ParseQrResponse {
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountHolderName: string;
  amount: number | null;
  description: string;
  isDynamic: boolean;
  crcValid: boolean;
  rawPayload: string;
}

export interface GenerateQrResponse {
  qrPayload: string;
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountHolderName: string;
  amount: number | null;
  description: string;
}

export interface PayQrRequest {
  sourceAccountId: string;
  targetAccountNumber: string;
  targetBankBin: string;
  targetAccountName: string;
  amount: number;
  description: string;
  qrPayload: string;
}

export interface QrPaymentResult {
  paymentId: string;
  sourceAccountId: string;
  targetAccountNumber: string;
  targetBankBin: string;
  targetAccountName: string;
  amount: number;
  description: string;
  status: string;
  timestamp: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
  traceId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class QrPaymentService {
  private readonly baseUrl = '/api/v1/payments/qr';

  constructor(private http: HttpClient) {}

  /**
   * Phân tích mã QR VietQR EMVCo.
   */
  parseQr(qrData: string): Observable<ApiResponse<ParseQrResponse>> {
    return this.http.post<ApiResponse<ParseQrResponse>>(`${this.baseUrl}/parse`, { qrData });
  }

  /**
   * Sinh mã VietQR động/tĩnh.
   */
  generateQr(params: { accountNumber: string; bankBin?: string; amount?: number; description?: string }): Observable<ApiResponse<GenerateQrResponse>> {
    return this.http.post<ApiResponse<GenerateQrResponse>>(`${this.baseUrl}/generate`, params);
  }

  /**
   * Lấy mã QR cá nhân nhận tiền.
   */
  getMyQr(accountNumber: string, amount?: number, description?: string): Observable<ApiResponse<GenerateQrResponse>> {
    let queryParams = `accountNumber=${encodeURIComponent(accountNumber)}`;
    if (amount) queryParams += `&amount=${amount}`;
    if (description) queryParams += `&description=${encodeURIComponent(description)}`;
    return this.http.get<ApiResponse<GenerateQrResponse>>(`${this.baseUrl}/my-qr?${queryParams}`);
  }

  /**
   * Thực thi thanh toán qua VietQR (Kèm Idempotency Header).
   */
  payQr(payload: PayQrRequest, idempotencyKey: string): Observable<ApiResponse<QrPaymentResult>> {
    const headers = new HttpHeaders({
      'X-Idempotency-Key': idempotencyKey
    });
    return this.http.post<ApiResponse<QrPaymentResult>>(`${this.baseUrl}/pay`, payload, { headers });
  }
}
