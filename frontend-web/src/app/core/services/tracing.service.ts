import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface TraceContext {
  traceId: string;
  spanId: string;
  traceparent: string;
  serviceName: string;
  mdcCorrelation: string;
}

export interface SpanNode {
  spanName: string;
  serviceName: string;
  spanId: string;
  durationMs: number;
  status: string;
  children?: SpanNode[];
}

@Injectable({
  providedIn: 'root'
})
export class TracingService {
  private readonly baseUrl = '/api/v1/tracing';

  constructor(private http: HttpClient) {}

  /**
   * Lấy ngữ cảnh Trace context hiện tại.
   */
  getCurrentTraceContext(): Observable<ApiResponse<TraceContext>> {
    return this.http.get<ApiResponse<TraceContext>>(`${this.baseUrl}/current`);
  }

  /**
   * Giả lập tạo cây vết vết phân tán Distributed Trace Waterfall.
   */
  simulateDistributedTrace(): Observable<ApiResponse<SpanNode>> {
    return this.http.post<ApiResponse<SpanNode>>(`${this.baseUrl}/simulate-span-tree`, {});
  }
}
