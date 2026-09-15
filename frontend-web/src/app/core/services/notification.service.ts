import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../auth/auth.service';

export interface AppNotification {
  id: string;
  customerId: string;
  title: string;
  content: string;
  type: string;
  isRead: boolean;
  referenceId?: string;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

/**
 * Service quản lý Thông báo In-App người dùng (Notification Service).
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly API_URL = '/api/v1/notifications';
  private readonly http = inject(HttpClient);

  /**
   * Lấy danh sách tất cả thông báo
   */
  getNotifications(): Observable<ApiResponse<AppNotification[]>> {
    return this.http.get<ApiResponse<AppNotification[]>>(this.API_URL);
  }

  /**
   * Đếm số lượng thông báo chưa đọc
   */
  getUnreadCount(): Observable<ApiResponse<UnreadCountResponse>> {
    return this.http.get<ApiResponse<UnreadCountResponse>>(`${this.API_URL}/unread-count`);
  }

  /**
   * Đánh dấu 1 thông báo là đã đọc
   */
  markAsRead(id: string): Observable<ApiResponse<AppNotification>> {
    return this.http.patch<ApiResponse<AppNotification>>(`${this.API_URL}/${id}/read`, {});
  }

  /**
   * Đánh dấu tất cả thông báo là đã đọc
   */
  markAllAsRead(): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.API_URL}/read-all`, {});
  }
}
