package com.bankx.core.infrastructure.persistence.ledger.repository;

import com.bankx.core.infrastructure.persistence.ledger.entity.LedgerEntryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository giao tiếp bảng {@code ledger_entries}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu Spring Repository Bean quản lý bút toán ghi sổ.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    /**
     * Tìm kiếm các bút toán ghi sổ theo ID tài khoản ngân hàng.
     *
     * @param accountId ID tài khoản
     * @param pageable Phân trang
     * @return Danh sách {@link LedgerEntryJpaEntity}
     */
    List<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
