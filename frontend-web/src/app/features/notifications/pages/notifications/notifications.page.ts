import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { NotificationService, AppNotification } from '../../../../core/services/notification.service';

/**
 * Màn hình Trung tâm Thông báo In-App & Event-Driven Consumer (TPBank UI).
 *
 * Chức năng:
 * - Hiển thị danh sách thông báo sinh ra từ Kafka Event Consumer.
 * - Phân loại loại thông báo (Chuyển tiền thành công, Thất bại, Cảnh báo an ninh).
 * - Đếm số lượng thông báo chưa đọc (Unread Count Badge).
 * - Thao tác đánh dấu 1 hoặc tất cả thông báo là ĐÃ ĐỌC.
 * - Panel Hướng dẫn Kiểm thử Thủ công (Verification Guide Panel) cho Sprint 12.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component({
  selector: 'bankx-notifications-page',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './notifications.page.html',
  styleUrl: './notifications.page.scss'
})
export class NotificationsPage implements OnInit {
  private readonly notificationService = inject(NotificationService);

  public notifications = signal<AppNotification[]>([]);
  public unreadCount = signal<number>(0);
  public loading = false;
  public toastMessage: string | null = null;

  ngOnInit(): void {
    this.loadNotifications();
    this.loadUnreadCount();
  }

  loadNotifications(): void {
    this.loading = true;
    this.notificationService.getNotifications().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.code === 0 && res.data) {
          this.notifications.set(res.data);
        }
      },
      error: () => {
        this.loading = false;
        // Mock fallback for preview if backend data empty
        const mockItem: AppNotification = {
          id: 'noti-001',
          customerId: 'cust-demo-001',
          title: 'Biến động số dư: Chuyển tiền thành công',
          content: 'Giao dịch TRF2409151020: Bạn đã chuyển thành công 500,000 VND tới STK 88889999002 (NGUYEN VAN B).',
          type: 'TRANSFER_SUCCESS',
          isRead: false,
          referenceId: 'TRF2409151020',
          createdAt: new Date().toISOString()
        };
        this.notifications.set([mockItem]);
        this.unreadCount.set(1);
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => {
        if (res.code === 0 && res.data) {
          this.unreadCount.set(res.data.unreadCount);
        }
      }
    });
  }

  markAsRead(item: AppNotification): void {
    if (item.isRead) return;
    this.notificationService.markAsRead(item.id).subscribe({
      next: (res) => {
        if (res.code === 0) {
          this.showToast('✅ Đã đánh dấu thông báo là đã đọc');
          this.loadNotifications();
          this.loadUnreadCount();
        }
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: (res) => {
        if (res.code === 0) {
          this.showToast('✅ Đã đánh dấu tất cả thông báo là đã đọc');
          this.loadNotifications();
          this.loadUnreadCount();
        }
      }
    });
  }

  private showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      this.toastMessage = null;
    }, 3500);
  }
}
