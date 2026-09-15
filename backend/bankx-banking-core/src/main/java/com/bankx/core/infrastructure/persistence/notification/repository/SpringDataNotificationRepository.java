package com.bankx.core.infrastructure.persistence.notification.repository;

import com.bankx.core.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository cho bảng {@code notifications}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đăng ký DAO Repository Bean quản lý CSDL Thông báo.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    /**
     * Lấy danh sách thông báo của khách hàng xếp theo thời gian mới nhất.
     *
     * @param customerId ID khách hàng
     * @return Danh sách {@link NotificationJpaEntity}
     */
    List<NotificationJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /**
     * Đếm số lượng thông báo chưa đọc của khách hàng.
     *
     * @param customerId ID khách hàng
     * @return Số lượng thông báo chưa đọc
     */
    long countByCustomerIdAndIsReadFalse(UUID customerId);
}
