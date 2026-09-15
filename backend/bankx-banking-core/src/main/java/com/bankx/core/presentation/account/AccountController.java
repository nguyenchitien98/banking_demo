package com.bankx.core.presentation.account;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.account.AccountApplicationService;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.presentation.account.dto.BankAccountResponse;
import com.bankx.core.presentation.account.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller xử lý các dịch vụ quản lý tài khoản thanh toán (Bank Account REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Đánh dấu Controller và tự động serialize phản hồi sang định dạng JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/accounts")}: Đặt tiền tố API URL cho các endpoints quản lý tài khoản.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    /**
     * Endpoint lấy danh sách tài khoản thuộc về khách hàng đang đăng nhập.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link BankAccountResponse}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<BankAccountResponse> response = accountService.getMyAccounts(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản thành công", response, traceId));
    }

    /**
     * Endpoint mở tài khoản thanh toán mới.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param request Payload {@link CreateAccountRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link BankAccountResponse}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountResponse>> createAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BankAccountResponse response = accountService.createAccount(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Mở tài khoản mới thành công", response, traceId));
    }

    /**
     * Endpoint phong tỏa tài khoản (FREEZE).
     *
     * @param id ID tài khoản cần phong tỏa
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link BankAccountResponse}
     */
    @PatchMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<BankAccountResponse>> freezeAccount(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BankAccountResponse response = accountService.freezeAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Phong tỏa tài khoản thành công", response, traceId));
    }
}
