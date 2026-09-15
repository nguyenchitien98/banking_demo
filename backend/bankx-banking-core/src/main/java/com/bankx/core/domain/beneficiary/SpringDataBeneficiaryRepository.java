package com.bankx.core.domain.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository cho bảng {@link BeneficiaryJpaEntity}.
 */
@Repository
public interface SpringDataBeneficiaryRepository extends JpaRepository<BeneficiaryJpaEntity, String> {

    /**
     * Lấy danh sách người thụ hưởng của khách hàng sắp xếp theo tần suất giao dịch giảm dần.
     * 
     * @param customerId ID khách hàng
     * @return Danh sách entity thụ hưởng
     */
    List<BeneficiaryJpaEntity> findByCustomerIdOrderByTransferCountDescLastTransferAtDesc(String customerId);

    /**
     * Tìm bản ghi người thụ hưởng trùng khớp thông tin tài khoản và ngân hàng.
     * 
     * @param customerId ID khách hàng
     * @param accountNumber Số tài khoản nhận
     * @param bankBin Mã BIN ngân hàng
     * @return Optional entity thụ hưởng
     */
    Optional<BeneficiaryJpaEntity> findByCustomerIdAndAccountNumberAndBankBin(String customerId, String accountNumber, String bankBin);
}
