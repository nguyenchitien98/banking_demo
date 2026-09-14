import { Injectable, signal, computed } from '@angular/core';

/**
 * Service quản lý trạng thái loading toàn cục.
 *
 * Sử dụng Angular Signals thay vì BehaviorSubject vì:
 * - Fine-grained reactivity: Chỉ re-render component khi giá trị thực sự thay đổi
 * - Đơn giản hơn: Không cần unsubscribe
 * - Hỗ trợ computed(): isLoading tự động tính từ activeRequests
 *
 * Pattern: Counter-based (tăng khi có request mới, giảm khi request xong)
 * → Đúng khi nhiều requests đồng thời (không tắt spinner sớm)
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  // Số lượng HTTP requests đang active
  private readonly activeRequests = signal(0);

  // Computed: true nếu có ít nhất 1 request đang chạy
  readonly isLoading = computed(() => this.activeRequests() > 0);

  /** Tăng counter khi có request mới bắt đầu */
  show(): void {
    this.activeRequests.update((count) => count + 1);
  }

  /** Giảm counter khi request kết thúc (thành công hoặc lỗi) */
  hide(): void {
    this.activeRequests.update((count) => Math.max(0, count - 1));
  }
}
