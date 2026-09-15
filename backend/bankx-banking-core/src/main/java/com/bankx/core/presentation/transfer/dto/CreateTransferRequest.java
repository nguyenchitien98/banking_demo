package com.bankx.core.presentation.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Yêu cầu DTO thực hiện giao dịch chuyển tiền nội bộ.
 *
 * @param sourceAccountId ID tài khoản trích nợ
 * @param targetAccountNumber Số tài khoản thụ hưởng
 * @param amount Số tiền chuyển (tối thiểu 1,000 VND)
 * @param description Nội dung chuyển tiền
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record CreateTransferRequest(
        @NotNull(message = "Tài khoản trích nợ không được để trống")
        UUID sourceAccountId,

        @NotBlank(message = "Số tài khoản thụ hưởng không được để trống")
        String targetAccountNumber,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1000.00", message = "Số tiền chuyển tối thiểu là 1,000 VND")
        BigDecimal amount,

        String description
) {}
