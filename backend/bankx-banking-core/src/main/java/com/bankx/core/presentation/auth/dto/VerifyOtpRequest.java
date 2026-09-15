package com.bankx.core.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO yêu cầu xác thực mã OTP.
 *
 * @param phone Số điện thoại nhận OTP
 * @param purpose Mục đích yêu cầu OTP
 * @param otpCode Mã OTP 6 chữ số do người dùng nhập
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record VerifyOtpRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng Việt Nam")
        String phone,

        @NotBlank(message = "Mục đích yêu cầu OTP không được để trống")
        String purpose,

        @NotBlank(message = "Mã OTP không được để trống")
        @Size(min = 6, max = 6, message = "Mã OTP phải bao gồm chính xác 6 chữ số")
        String otpCode
) {}
