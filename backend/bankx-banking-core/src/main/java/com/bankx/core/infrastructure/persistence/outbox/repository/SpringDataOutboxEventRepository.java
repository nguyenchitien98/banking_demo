package com.bankx.core.infrastructure.persistence.outbox.repository;

import com.bankx.core.domain.outbox.model.OutboxStatus;
import com.bankx.core.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository quản lý truy vấn bảng {@code outbox_events}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu DAO Repository Bean thao tác CSDL.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * Lấy danh sách 50 sự kiện outbox đang ở trạng thái PENDING theo thứ tự thời gian tạo tăng dần.
     *
     * @param status Trạng thái sự kiện (PENDING)
     * @return Danh sách {@link OutboxEventJpaEntity}
     */
    List<OutboxEventJpaEntity> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    /**
     * Lấy 50 sự kiện outbox mới nhất phục vụ hiển thị Monitor kiểm thử.
     *
     * @return Danh sách {@link OutboxEventJpaEntity}
     */
    List<OutboxEventJpaEntity> findTop50ByOrderByCreatedAtDesc();
}
