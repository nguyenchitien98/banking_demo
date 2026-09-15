package com.bankx.core.domain.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository cho bảng {@link FraudAlertJpaEntity}.
 */
@Repository
public interface SpringDataFraudAlertRepository extends JpaRepository<FraudAlertJpaEntity, String> {

    /**
     * Lấy danh sách cảnh báo gian lận theo trạng thái duyệt.
     * 
     * @param status Trạng thái cảnh báo (PENDING_REVIEW...)
     * @return Danh sách entity cảnh báo
     */
    List<FraudAlertJpaEntity> findByStatusOrderByCreatedAtDesc(FraudAlertStatus status);

    /**
     * Lấy tất cả cảnh báo gian lận sắp xếp theo thời gian mới nhất.
     * 
     * @return Danh sách entity cảnh báo
     */
    List<FraudAlertJpaEntity> findAllByOrderByCreatedAtDesc();
}
