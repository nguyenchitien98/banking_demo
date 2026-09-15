package com.bankx.core.application.notification;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import com.bankx.core.infrastructure.persistence.notification.repository.SpringDataNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service ứng dụng quản lý Thông báo In-App người dùng (Notification Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Class là Spring Service Bean chứa các luồng nghiệp vụ thông báo.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class NotificationApplicationService {

    private final SpringDataNotificationRepository notificationRepository;

    public NotificationApplicationService(SpringDataNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Lấy tất cả thông báo của một khách hàng xếp theo thời gian mới nhất.
     *
     * @param customerId ID khách hàng
     * @return Danh sách {@link NotificationJpaEntity}
     */
    @Transactional(readOnly = true)
    public List<NotificationJpaEntity> getCustomerNotifications(UUID customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Đếm số lượng thông báo chưa đọc của khách hàng.
     *
     * @param customerId ID khách hàng
     * @return Số lượng thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }

    /**
     * Đánh dấu một thông báo là ĐÃ ĐỌC.
     *
     * @param customerId ID khách hàng
     * @param notificationId ID thông báo
     * @return {@link NotificationJpaEntity} đã cập nhật
     */
    @Transactional
    public NotificationJpaEntity markAsRead(UUID customerId, UUID notificationId) {
        NotificationJpaEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông báo id: " + notificationId));

        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    /**
     * Đánh dấu TẤT CẢ thông báo của khách hàng là ĐÃ ĐỌC.
     *
     * @param customerId ID khách hàng
     */
    @Transactional
    public void markAllAsRead(UUID customerId) {
        List<NotificationJpaEntity> notifications = notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        for (NotificationJpaEntity n : notifications) {
            if (!n.isRead()) {
                n.markAsRead();
                notificationRepository.save(n);
            }
        }
    }
}
