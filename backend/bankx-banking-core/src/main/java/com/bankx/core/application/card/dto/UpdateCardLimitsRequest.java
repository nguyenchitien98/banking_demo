package com.bankx.core.application.card.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật hạn mức thanh toán của Thẻ.
 * 
 * @param spendingLimit Hạn mức 1 giao dịch
 * @param dailyLimit Hạn mức thanh toán ngày
 */
public record UpdateCardLimitsRequest(
        @NotNull(message = "Hạn mức 1 lần không được để trống")
        @DecimalMin(value = "1000000.00", message = "Hạn mức tối thiểu từ 1,000,000 VND")
        BigDecimal spendingLimit,

        @NotNull(message = "Hạn mức theo ngày không được để trống")
        @DecimalMin(value = "1000000.00", message = "Hạn mức ngày tối thiểu từ 1,000,000 VND")
        BigDecimal dailyLimit
) {
}
