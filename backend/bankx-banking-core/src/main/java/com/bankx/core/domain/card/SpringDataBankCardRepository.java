package com.bankx.core.domain.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository quản lý truy vấn bảng {@link BankCardJpaEntity}.
 */
@Repository
public interface SpringDataBankCardRepository extends JpaRepository<BankCardJpaEntity, String> {

    /**
     * Lấy danh sách thẻ theo mã khách hàng.
     * 
     * @param customerId ID khách hàng
     * @return Danh sách entity thẻ ngân hàng
     */
    List<BankCardJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /**
     * Tìm thẻ theo PAN token.
     * 
     * @param panToken Token mã hóa thẻ
     * @return Optional chứa thông tin thẻ nếu tồn tại
     */
    Optional<BankCardJpaEntity> findByPanToken(String panToken);
}
