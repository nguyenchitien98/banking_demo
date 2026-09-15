package com.bankx.core.presentation.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload DTO yêu cầu Thanh toán Hóa đơn dịch vụ.
 *
 * @param sourceAccountId ID tài khoản trích nợ
 * @param providerCode Mã nhà cung cấp dịch vụ (ví dụ: "EVN_HN")
 * @param customerBillCode Mã khách hàng hóa đơn
 * @param amount Số tiền thanh toán
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record PayBillRequest(
        @NotNull(message = "Tài khoản trích nợ không được để trống")
        UUID sourceAccountId,

        @NotBlank(message = "Mã nhà cung cấp không được để trống")
        String providerCode,

        @NotBlank(message = "Mã khách hàng không được để trống")
        String customerBillCode,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1000", message = "Số tiền thanh toán tối thiểu là 1,000 VND")
        BigDecimal amount
) {}
