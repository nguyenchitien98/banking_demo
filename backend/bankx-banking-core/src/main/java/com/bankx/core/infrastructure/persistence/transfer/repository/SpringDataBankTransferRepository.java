package com.bankx.core.infrastructure.persistence.transfer.repository;

import com.bankx.core.domain.transfer.model.TransferStatus;
import com.bankx.core.infrastructure.persistence.transfer.entity.BankTransferJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository giao tiếp bảng {@code bank_transfers}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu DAO Component quản lý lệnh chuyển tiền.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataBankTransferRepository extends JpaRepository<BankTransferJpaEntity, UUID> {

    /**
     * Tìm lệnh chuyển tiền theo mã chuyển tiền.
     *
     * @param transferCode Mã chuyển tiền duy nhất
     * @return {@link Optional} chứa {@link BankTransferJpaEntity}
     */
    Optional<BankTransferJpaEntity> findByTransferCode(String transferCode);

    /**
     * Tìm danh sách lệnh chuyển tiền liên quan tới một tài khoản (tài khoản gửi hoặc nhận).
     *
     * @param sourceAccountId ID tài khoản trích nợ
     * @param targetAccountId ID tài khoản thụ hưởng
     * @param pageable Phân trang
     * @return Danh sách {@link BankTransferJpaEntity}
     */
    List<BankTransferJpaEntity> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            UUID sourceAccountId, UUID targetAccountId, Pageable pageable);

    /**
     * Tính tổng số tiền chuyển thành công từ một tài khoản trích nợ trong ngày.
     *
     * @param sourceAccountId ID tài khoản trích nợ
     * @param status Trạng thái thành công {@link TransferStatus#COMPLETED}
     * @param startOfDay Thời điểm 00:00:00 đầu ngày
     * @return Tổng số tiền đã chuyển (BigDecimal)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransferJpaEntity t " +
           "WHERE t.sourceAccountId = :sourceAccountId AND t.status = :status AND t.createdAt >= :startOfDay")
    BigDecimal sumAmountBySourceAccountIdAndStatusAndCreatedAtGreaterThanEqual(
            @Param("sourceAccountId") UUID sourceAccountId,
            @Param("status") TransferStatus status,
            @Param("startOfDay") Instant startOfDay);
}
