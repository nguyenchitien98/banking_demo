package com.bankx.core.domain.payment.qr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository cho đối tượng {@link QrPaymentJpaEntity}.
 * 
 * <p>Sử dụng annotation {@link Repository} để đánh dấu thành phần truy xuất dữ liệu
 * CSDL cho giao dịch quét mã QR VietQR.</p>
 */
@Repository
public interface SpringDataQrPaymentRepository extends JpaRepository<QrPaymentJpaEntity, String> {

    /**
     * Tìm danh sách lịch sử thanh toán QR theo tài khoản nguồn.
     * 
     * @param sourceAccountId ID tài khoản nguồn
     * @return Danh sách entity giao dịch QR
     */
    List<QrPaymentJpaEntity> findBySourceAccountIdOrderByCreatedAtDesc(String sourceAccountId);
}
