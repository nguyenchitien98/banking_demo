package com.bankx.core.presentation.auth;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.auth.OtpApplicationService;
import com.bankx.core.presentation.auth.dto.SendOtpRequest;
import com.bankx.core.presentation.auth.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý các dịch vụ xác thực mã OTP (OTP REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Đánh dấu lớp xử lý HTTP Request và tự động serialize phản hồi sang JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/auth/otp")}: Đặt tiền tố URL cho các endpoint thuộc dịch vụ OTP.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth/otp")
public class OtpController {

    private final OtpApplicationService otpService;

    public OtpController(OtpApplicationService otpService) {
        this.otpService = otpService;
    }

    /**
     * Endpoint Yêu cầu khởi tạo và gửi mã OTP qua SMS.
     *
     * @param request Payload {@link SendOtpRequest} chứa số điện thoại và mục đích
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} thông báo kết quả
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        String message = otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(message, "SENT", traceId));
    }

    /**
     * Endpoint Xác thực mã OTP do người dùng nhập.
     *
     * @param request Payload {@link VerifyOtpRequest} chứa OTP 6 chữ số
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} kết quả xác thực
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        boolean isValid = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công", isValid, traceId));
    }
}
