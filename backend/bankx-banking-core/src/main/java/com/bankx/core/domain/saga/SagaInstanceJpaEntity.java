package com.bankx.core.domain.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity biểu diễn Saga Instance {@code saga_instances}.
 * 
 * <p>Annotation {@link Entity} chỉ định JPA Entity quản lý trạng thái của Saga State Machine.
 * Annotation {@link Table} ánh xạ vào bảng {@code saga_instances} trong cơ sở dữ liệu.
 * Các annotation Lombok {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * hỗ trợ tự động hóa getter/setter và builder pattern.</p>
 */
@Entity
@Table(name = "saga_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstanceJpaEntity {

    @Id
    @Column(name = "saga_id", length = 64, nullable = false)
    private String sagaId;

    @Column(name = "transfer_code", length = 64, nullable = false, unique = true)
    private String transferCode;

    @Column(name = "current_state", length = 64, nullable = false)
    private String currentState;

    @Column(name = "source_account_number", length = 64, nullable = false)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", length = 64, nullable = false)
    private String targetAccountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
