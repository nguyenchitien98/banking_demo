package com.bankx.core.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Lớp tiện ích chịu trách nhiệm sinh, giải mã và xác thực JWT Token (Access Token & Refresh Token).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký JwtTokenProvider làm Spring Bean để inject tự động
 *       vào {@link SecurityConfig}, {@link JwtAuthenticationFilter} và {@code AuthApplicationService}.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${bankx.jwt.secret:BankXSuperSecretKeyForJWTAuth2026BankingGradeVeryLongKeyAndSecure1234567890!}") String secret,
            @Value("${bankx.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs, // 15 phút
            @Value("${bankx.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs // 7 ngày
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Sinh JWT Access Token (hạn 15 phút) chứa thông tin username và các vai trò (roles).
     *
     * @param userId ID người dùng
     * @param username Tên đăng nhập
     * @param roles Danh sách các vai trò (ROLE_CUSTOMER, ROLE_ADMIN...)
     * @return Chuỗi JWT Access Token ký HMAC-SHA256
     */
    public String generateAccessToken(UUID userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Trích xuất username từ JWT Access Token.
     *
     * @param token Chuỗi JWT token
     * @return Username của chủ sở hữu token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Trích xuất userId từ JWT Access Token.
     *
     * @param token Chuỗi JWT token
     * @return {@link UUID} người dùng
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.get("userId", String.class));
    }

    /**
     * Xác thực tính hợp lệ của JWT Access Token.
     *
     * @param token Chuỗi token cần kiểm tra
     * @return {@code true} nếu token hợp lệ và chưa hết hạn
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT Token không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
