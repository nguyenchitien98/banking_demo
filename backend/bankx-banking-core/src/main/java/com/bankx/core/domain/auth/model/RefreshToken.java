package com.bankx.core.domain.auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Thể hiện Domain của Refresh Token.
 *
 * <p>Quản lý vòng đời Refresh Token, kiểm tra thời gian hết hạn và cơ chế vô hiệu hóa (Revocation).</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String token;
    private final Instant expiryDate;
    private boolean revoked;
    private final Instant createdAt;

    /**
     * Khởi tạo đối tượng RefreshToken.
     *
     * @param id ID duy nhất
     * @param userId ID người dùng sở hữu token
     * @param token Chuỗi token ngẫu nhiên UUID/secure random
     * @param expiryDate Thời điểm hết hạn token
     * @param revoked Trạng thái thu hồi (trái với active)
     * @param createdAt Thời điểm tạo
     */
    public RefreshToken(UUID id, UUID userId, String token, Instant expiryDate, boolean revoked, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId;
        this.token = token;
        this.expiryDate = expiryDate;
        this.revoked = revoked;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * Kiểm tra xem token có hợp lệ không (chưa bị revoked và chưa hết hạn).
     *
     * @return {@code true} nếu token còn hiệu lực
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiryDate);
    }

    /**
     * Thu hồi (vô hiệu hóa) token này.
     */
    public void revoke() {
        this.revoked = true;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiryDate() { return expiryDate; }
    public boolean isRevoked() { return revoked; }
    public Instant getCreatedAt() { return createdAt; }
}
