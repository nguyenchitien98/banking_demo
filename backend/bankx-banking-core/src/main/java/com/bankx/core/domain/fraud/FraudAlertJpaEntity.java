package com.bankx.core.domain.fraud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity biểu diễn Cảnh báo Gian lận {@code fraud_alerts}.
 */
@Entity
@Table(name = "fraud_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "source_account_id", length = 64, nullable = false)
    private String sourceAccountId;

    @Column(name = "target_account_number", length = 64)
    private String targetAccountNumber;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_action", length = 32, nullable = false)
    private RiskAction riskAction;

    @Column(name = "triggered_rules", columnDefinition = "TEXT", nullable = false)
    private String triggeredRules;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private FraudAlertStatus status;

    @Column(name = "reviewer_notes", length = 255)
    private String reviewerNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
