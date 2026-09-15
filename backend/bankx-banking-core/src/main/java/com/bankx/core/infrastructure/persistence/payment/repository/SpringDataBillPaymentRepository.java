package com.bankx.core.infrastructure.persistence.payment.repository;

import com.bankx.core.infrastructure.persistence.payment.entity.BillPaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository cho bảng {@code bill_payments}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Khai báo DAO Repository Bean.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataBillPaymentRepository extends JpaRepository<BillPaymentJpaEntity, UUID> {

    List<BillPaymentJpaEntity> findBySourceAccountIdOrderByCreatedAtDesc(UUID sourceAccountId);
}
