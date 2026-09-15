package com.bankx.core.infrastructure.persistence.transfer.repository;

import com.bankx.core.infrastructure.persistence.transfer.entity.TransferLimitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository giao tiếp bảng {@code transfer_limits}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu Spring Repository quản lý hạn mức chuyển tiền.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataTransferLimitRepository extends JpaRepository<TransferLimitJpaEntity, UUID> {

    /**
     * Tìm hạn mức giao dịch theo ID khách hàng.
     *
     * @param customerId ID khách hàng
     * @return {@link Optional} chứa {@link TransferLimitJpaEntity}
     */
    Optional<TransferLimitJpaEntity> findByCustomerId(UUID customerId);
}
