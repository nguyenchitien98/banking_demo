package com.bankx.core.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository cho thực thể {@link BankAccountJpaEntity}.
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
public interface SpringDataBankAccountRepository extends JpaRepository<BankAccountJpaEntity, UUID> {

    /**
     * Tìm danh sách BankAccountJpaEntity theo customerId.
     */
    List<BankAccountJpaEntity> findByCustomerId(UUID customerId);

    /**
     * Tìm BankAccountJpaEntity theo accountNumber.
     */
    Optional<BankAccountJpaEntity> findByAccountNumber(String accountNumber);
}
