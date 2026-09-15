package com.bankx.core.presentation.auth.dto;

/**
 * Phản hồi xác thực thành công chứa JWT Access Token và Refresh Token.
 *
 * @param accessToken JWT Access Token (TTL 15 phút)
 * @param refreshToken Refresh Token (TTL 7 ngày)
 * @param tokenType Loại Token ("Bearer")
 * @param expiresIn Thời gian hết hạn của Access Token (giây)
 * @param user Thông tin người dùng tóm tắt
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryDto user
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresInMs, UserSummaryDto user) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresInMs / 1000,
                user
        );
    }
}
