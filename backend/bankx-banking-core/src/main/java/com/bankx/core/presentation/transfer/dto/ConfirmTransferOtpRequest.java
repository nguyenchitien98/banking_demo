package com.bankx.core.presentation.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Phản hồi / Yêu cầu DTO xác nhận mã OTP cho giao dịch chuyển tiền.
 *
 * @param otpCode Mã OTP 6 chữ số
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record ConfirmTransferOtpRequest(
        @NotBlank(message = "Mã OTP không được để trống")
        @Size(min = 6, max = 6, message = "Mã OTP phải đúng 6 chữ số")
        String otpCode
) {}
