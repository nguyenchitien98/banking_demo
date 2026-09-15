package com.bankx.core.application.fraud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO yêu cầu đánh giá điểm rủi ro cho một giao dịch.
 * 
 * @param sourceAccountId ID tài khoản nguồn
 * @param targetAccountNumber Số tài khoản thụ hưởng
 * @param amount Số tiền giao dịch
 * @param isNewDevice True nếu từ thiết bị mới
 * @param isNewBeneficiary True nếu chuyển tiền thụ hưởng mới
 * @param velocityLastMinute Số lượng giao dịch trong 60 giây qua
 * @param customHour Giờ giả lập trong ngày (0-23, tùy chọn)
 */
public record EvaluateTransactionRequest(
        @NotBlank(message = "ID tài khoản nguồn không được để trống")
        String sourceAccountId,

        String targetAccountNumber,

        @NotNull(message = "Số tiền giao dịch không được để trống")
        @DecimalMin(value = "1000.00", message = "Số tiền tối thiểu 1,000 VND")
        BigDecimal amount,

        boolean isNewDevice,

        boolean isNewBeneficiary,

        int velocityLastMinute,

        Integer customHour
) {
}
