package com.bankx.core.application.auth;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.common.util.MaskingUtils;
import com.bankx.core.domain.auth.model.RefreshToken;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.domain.auth.model.UserRole;
import com.bankx.core.domain.auth.repository.RefreshTokenRepository;
import com.bankx.core.domain.auth.repository.UserRepository;
import com.bankx.core.infrastructure.security.JwtTokenProvider;
import com.bankx.core.presentation.auth.dto.LoginRequest;
import com.bankx.core.presentation.auth.dto.LoginResponse;
import com.bankx.core.presentation.auth.dto.RefreshTokenRequest;
import com.bankx.core.presentation.auth.dto.UserSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application Service điều phối các Use Cases nghiệp vụ xác thực (Authentication Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu lớp này là một Spring Service Bean chứa các luồng xử lý nghiệp vụ ứng dụng.</li>
 *   <li>{@code @Transactional}: Đảm bảo tính nguyên tố (Atomic) của các thao tác cập nhật CSDL (như xoay Refresh Token, cập nhật số lần sai mật khẩu...).</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthApplicationService(UserRepository userRepository,
                                   RefreshTokenRepository refreshTokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Xử lý Use Case Đăng nhập người dùng (Login).
     *
     * @param request {@link LoginRequest} chứa username và password
     * @return {@link LoginResponse} chứa Access Token và Refresh Token mới
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Xử lý yêu cầu đăng nhập cho username: {}", MaskingUtils.maskEmail(request.username()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BankingException(ErrorCode.INVALID_CREDENTIALS));

        // 1. Kiểm tra xem tài khoản có đang bị khóa không
        if (user.isAccountLocked()) {
            log.warn("Đăng nhập thất bại: Tài khoản [{}] bị khóa cho tới {}", user.getUsername(), user.getLockedUntil());
            throw new BankingException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 2. Kiểm tra mật khẩu băm
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            log.warn("Đăng nhập thất bại: Sai mật khẩu cho user [{}]. Số lần sai: {}", user.getUsername(), user.getFailedLoginAttempts());
            throw new BankingException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Đăng nhập thành công -> Reset số lần sai
        user.resetFailedLogin();
        userRepository.save(user);

        // 4. Sinh JWT Access Token & Refresh Token
        List<String> rolesList = user.getRoles().stream().map(UserRole::name).toList();
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), rolesList);
        
        // Thu hồi toàn bộ Refresh Token cũ (Refresh Token Rotation)
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        // Tạo Refresh Token mới
        String refreshTokenString = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        RefreshToken newRefreshToken = new RefreshToken(
                UUID.randomUUID(), user.getId(), refreshTokenString, expiryDate, false, Instant.now()
        );
        refreshTokenRepository.save(newRefreshToken);

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                MaskingUtils.maskEmail(user.getEmail()),
                user.getRoles().stream().map(UserRole::name).collect(Collectors.toSet())
        );

        log.info("Đăng nhập thành công cho user: [{}]", user.getUsername());
        return LoginResponse.of(accessToken, refreshTokenString, tokenProvider.getAccessTokenExpirationMs(), userSummary);
    }

    /**
     * Xử lý Use Case Cấp lại Access Token mới bằng Refresh Token (Refresh Token Rotation).
     *
     * @param request {@link RefreshTokenRequest}
     * @return {@link LoginResponse} mới
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken tokenDomain = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BankingException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!tokenDomain.isValid()) {
            throw new BankingException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(tokenDomain.getUserId())
                .orElseThrow(() -> new BankingException(ErrorCode.USER_NOT_FOUND));

        if (user.isAccountLocked()) {
            throw new BankingException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Vô hiệu hóa token vừa dùng (Rotation)
        tokenDomain.revoke();
        refreshTokenRepository.save(tokenDomain);

        // Sinh token mới
        List<String> rolesList = user.getRoles().stream().map(UserRole::name).toList();
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), rolesList);
        String newRefreshTokenString = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        
        RefreshToken newRefreshToken = new RefreshToken(
                UUID.randomUUID(), user.getId(), newRefreshTokenString, expiryDate, false, Instant.now()
        );
        refreshTokenRepository.save(newRefreshToken);

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                MaskingUtils.maskEmail(user.getEmail()),
                user.getRoles().stream().map(UserRole::name).collect(Collectors.toSet())
        );

        return LoginResponse.of(newAccessToken, newRefreshTokenString, tokenProvider.getAccessTokenExpirationMs(), userSummary);
    }

    /**
     * Xử lý Use Case Đăng xuất (Logout) -> Thu hồi Refresh Token.
     *
     * @param userId ID người dùng đăng xuất
     */
    @Transactional
    public void logout(UUID userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllUserTokens(userId);
            log.info("Đã thu hồi tất cả Refresh Token khi logout cho userId: {}", userId);
        }
    }
}
