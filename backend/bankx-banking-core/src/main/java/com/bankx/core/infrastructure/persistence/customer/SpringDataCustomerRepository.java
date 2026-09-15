package com.bankx.core.infrastructure.persistence.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository cho thực thể {@link CustomerJpaEntity}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Khai báo Spring Data Repository Component.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    /**
     * Tìm CustomerJpaEntity theo userId.
     */
    Optional<CustomerJpaEntity> findByUserId(UUID userId);

    /**
     * Tìm CustomerJpaEntity theo cifNumber.
     */
    Optional<CustomerJpaEntity> findByCifNumber(String cifNumber);
}
