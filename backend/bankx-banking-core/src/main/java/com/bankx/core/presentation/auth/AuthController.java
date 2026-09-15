package com.bankx.core.presentation.auth;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.auth.AuthApplicationService;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.presentation.auth.dto.LoginRequest;
import com.bankx.core.presentation.auth.dto.LoginResponse;
import com.bankx.core.presentation.auth.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý các yêu cầu xác thực người dùng (Authentication REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Đánh dấu lớp xử lý HTTP Request, chuyển đổi kết quả trả về thành định dạng JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/auth")}: Đặt tiền tố API URL cho toàn bộ các endpoint thuộc Auth Module.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint Đăng nhập hệ thống BankX.
     *
     * @param request Payload {@link LoginRequest} chứa username và password
     * @param traceId Header {@code X-Trace-Id} truyền từ API Gateway
     * @return {@link ResponseEntity} chứa {@link ApiResponse} với JWT Access Token & Refresh Token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response, traceId));
    }

    /**
     * Endpoint cấp lại Access Token mới qua Refresh Token (Refresh Token Rotation).
     *
     * @param request Payload {@link RefreshTokenRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} với cặp token mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response, traceId));
    }

    /**
     * Endpoint Đăng xuất hệ thống (Logout).
     *
     * @param currentUser Người dùng hiện tại trích xuất từ Spring Security Authentication Principal
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa thông báo đăng xuất thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        if (currentUser != null) {
            authService.logout(currentUser.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", "SUCCESS", traceId));
    }
}
