package com.bankx.core.domain.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository cho bảng {@link FraudRuleJpaEntity}.
 */
@Repository
public interface SpringDataFraudRuleRepository extends JpaRepository<FraudRuleJpaEntity, String> {

    /**
     * Tìm tất cả quy tắc fraud đang kích hoạt.
     * 
     * @param isActive Trạng thái active
     * @return Danh sách entity quy tắc
     */
    List<FraudRuleJpaEntity> findByIsActive(Boolean isActive);

    /**
     * Tìm quy tắc theo mã rule.
     * 
     * @param ruleCode Mã quy tắc (ví dụ: HIGH_AMOUNT)
     * @return Optional quy tắc
     */
    Optional<FraudRuleJpaEntity> findByRuleCode(String ruleCode);
}
