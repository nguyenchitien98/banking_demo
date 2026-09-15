package com.bankx.core.application.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) chứa chi tiết của một bước chuyển trạng thái Saga Audit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaAuditStepResponse {
    private String id;
    private String sagaId;
    private String stepName;
    private String stateBefore;
    private String stateAfter;
    private boolean compensating;
    private String details;
    private Instant createdAt;
}
