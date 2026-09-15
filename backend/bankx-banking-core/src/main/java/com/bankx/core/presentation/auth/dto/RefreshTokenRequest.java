package com.bankx.core.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO yêu cầu cấp lại Access Token mới qua Refresh Token.
 *
 * @param refreshToken Chuỗi Refresh Token
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token không được để trống")
        String refreshToken
) {}
