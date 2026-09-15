package com.bankx.core.infrastructure.persistence.ledger.repository;

import com.bankx.core.infrastructure.persistence.ledger.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository giao tiếp bảng {@code transactions}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu DAO component xử lý dữ liệu với Spring Container.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    /**
     * Tìm giao dịch theo mã tham chiếu duy nhất.
     *
     * @param transactionReference Mã tham chiếu giao dịch
     * @return {@link Optional} chứa {@link TransactionJpaEntity}
     */
    Optional<TransactionJpaEntity> findByTransactionReference(String transactionReference);
}
