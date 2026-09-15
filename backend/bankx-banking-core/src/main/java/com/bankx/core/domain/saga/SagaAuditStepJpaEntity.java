package com.bankx.core.domain.saga;

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
 * JPA Entity biểu diễn Nhật ký bước chuyển trạng thái Saga {@code saga_audit_steps}.
 * 
 * <p>Lưu vết chi tiết từng bước chuyển dịch trạng thái trong State Machine, kể cả các bước đền bù (Compensating Transactions).</p>
 */
@Entity
@Table(name = "saga_audit_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaAuditStepJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "saga_id", length = 64, nullable = false)
    private String sagaId;

    @Column(name = "step_name", length = 64, nullable = false)
    private String stepName;

    @Column(name = "state_before", length = 64, nullable = false)
    private String stateBefore;

    @Column(name = "state_after", length = 64, nullable = false)
    private String stateAfter;

    @Column(name = "is_compensating", nullable = false)
    private boolean compensating;

    @Column(name = "details", length = 255)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
