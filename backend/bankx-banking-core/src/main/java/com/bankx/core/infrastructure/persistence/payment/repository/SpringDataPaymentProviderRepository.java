package com.bankx.core.infrastructure.persistence.payment.repository;

import com.bankx.core.infrastructure.persistence.payment.entity.PaymentProviderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository cho bảng {@code payment_providers}.
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
public interface SpringDataPaymentProviderRepository extends JpaRepository<PaymentProviderJpaEntity, UUID> {

    List<PaymentProviderJpaEntity> findByStatusOrderByNameAsc(String status);

    Optional<PaymentProviderJpaEntity> findByCode(String code);
}
