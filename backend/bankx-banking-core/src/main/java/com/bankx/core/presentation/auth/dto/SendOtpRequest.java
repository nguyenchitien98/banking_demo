package com.bankx.core.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO yêu cầu tạo và gửi mã xác thực OTP.
 *
 * @param phone Số điện thoại nhận OTP
 * @param purpose Mục đích tạo OTP (ví dụ: LOGIN, TRANSFER, PROFILE_UPDATE)
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record SendOtpRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng Việt Nam")
        String phone,

        @NotBlank(message = "Mục đích yêu cầu OTP không được để trống")
        String purpose
) {}
