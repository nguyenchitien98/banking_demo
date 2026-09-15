package com.bankx.core.application.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object (DTO) chứa chi tiết của một Saga Instance kèm lịch sử chuyển trạng thái State Machine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstanceResponse {
    private String sagaId;
    private String transferCode;
    private String currentState;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SagaAuditStepResponse> steps;
}
