package com.bankx.core.application.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) nhận yêu cầu khởi chạy luồng Saga Orchestration Chuyển tiền phân tán.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionRequest {
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String description;

    /** Tham số hỗ trợ giả lập lỗi để kiểm thử Compensating Transactions ("NONE", "CREDIT_FAILED", "LEDGER_FAILED") */
    private String forceFailureStep;
}
