package com.bankx.core.presentation.customer;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.customer.CustomerApplicationService;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.presentation.customer.dto.CustomerResponse;
import com.bankx.core.presentation.customer.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý các yêu cầu liên quan tới thông tin hồ sơ và định danh khách hàng (Customer REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Đánh dấu lớp xử lý HTTP Requests và tự động chuyển đối tượng thành định dạng JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/customers")}: Đặt tiền tố API URL cho toàn bộ các endpoint thuộc Customer Module.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerApplicationService customerService;

    public CustomerController(CustomerApplicationService customerService) {
        this.customerService = customerService;
    }

    /**
     * Endpoint lấy thông tin hồ sơ cá nhân của khách hàng đang đăng nhập.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} thông tin khách hàng
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        CustomerResponse response = customerService.getProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", response, traceId));
    }

    /**
     * Endpoint cập nhật thông tin hồ sơ cá nhân.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param request Payload {@link UpdateProfileRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} hồ sơ đã cập nhật
     */
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        CustomerResponse response = customerService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ cá nhân thành công", response, traceId));
    }
}
