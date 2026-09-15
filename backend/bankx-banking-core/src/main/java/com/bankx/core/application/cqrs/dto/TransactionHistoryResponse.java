package com.bankx.core.application.cqrs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object (DTO) chứa chi tiết của một bản ghi lịch sử giao dịch thuộc CQRS Read Model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {
    private String id;
    private String transactionReference;
    private String customerId;
    private String accountNumber;
    private String oppositeAccountNumber;
    private String oppositeAccountName;
    private BigDecimal amount;
    private String direction;
    private String transactionType;
    private String category;
    private String description;
    private String status;
    private Instant createdAt;
}
