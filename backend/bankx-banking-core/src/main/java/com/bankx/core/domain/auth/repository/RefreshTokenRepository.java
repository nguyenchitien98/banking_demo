package com.bankx.core.domain.auth.repository;

import com.bankx.core.domain.auth.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Port Interface quản lý tương tác lưu trữ cho {@link RefreshToken}.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface RefreshTokenRepository {

    /**
     * Tìm kiếm RefreshToken theo chuỗi token.
     *
     * @param token Chuỗi token
     * @return {@link Optional} chứa {@link RefreshToken} nếu tìm thấy
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Thu hồi tất cả RefreshToken còn hiệu lực của một người dùng.
     *
     * @param userId ID người dùng
     */
    void revokeAllUserTokens(UUID userId);

    /**
     * Lưu thông tin RefreshToken vào cơ sở dữ liệu / Redis.
     *
     * @param refreshToken Đối tượng {@link RefreshToken}
     * @return Đối tượng {@link RefreshToken} đã lưu
     */
    RefreshToken save(RefreshToken refreshToken);
}
