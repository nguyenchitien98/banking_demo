package com.bankx.core.domain.fraud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA Entity biểu diễn Quy tắc Phát hiện Gian lận {@code fraud_rules}.
 */
@Entity
@Table(name = "fraud_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "rule_code", length = 64, nullable = false, unique = true)
    private String ruleCode;

    @Column(name = "rule_name", length = 128, nullable = false)
    private String ruleName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "weight_score", nullable = false)
    private Integer weightScore;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
