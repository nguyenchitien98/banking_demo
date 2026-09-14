/**
 * Model chuẩn cho mọi API response từ BankX Backend.
 * Phải khớp với Java record: ApiResponse<T>
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
  traceId: string | null;
}

/**
 * Model cho API error response từ BankX Backend.
 */
export interface ApiErrorResponse {
  success: false;
  errorCode: string;
  message: string;
  details: Record<string, string> | null;
  timestamp: string;
  traceId: string | null;
}

/**
 * Wrapper cho paginated responses.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
