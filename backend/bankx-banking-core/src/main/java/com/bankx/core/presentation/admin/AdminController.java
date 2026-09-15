package com.bankx.core.presentation.admin;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.admin.AdminApplicationService;
import com.bankx.core.application.admin.dto.AdminAuditLogResponse;
import com.bankx.core.application.admin.dto.AdminCustomerResponse;
import com.bankx.core.application.admin.dto.AdminKpiResponse;
import com.bankx.core.application.admin.dto.ReviewKycRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller quản lý cổng Admin Portal (Admin REST API).
 * 
 * <p>Annotation {@link RestController} đánh dấu class này đảm nhiệm vai trò tiếp nhận và phản hồi HTTP Requests dưới dạng JSON.
 * Annotation {@link RequestMapping} chỉ định tiền tố URL {@code /api/v1/admin} cho toàn bộ các API quản trị.
 * Annotation {@link RequiredArgsConstructor} tự động tiêm các dependency thông qua constructor.
 * Annotation {@link Validated} kích hoạt kiểm tra hợp lệ các tham số truyền vào.
 * Annotation {@link Slf4j} tự động khởi tạo Logger ghi nhận nhật ký hệ thống.</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminController {

    private final AdminApplicationService adminApplicationService;

    /**
     * API Lấy các chỉ số KPI tổng quan toàn hệ thống cho Admin Dashboard.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa đối tượng {@link AdminKpiResponse}
     */
    @GetMapping("/dashboard/kpis")
    public ApiResponse<AdminKpiResponse> getDashboardKpis() {
        log.info("REST request: Lấy thông số KPI Dashboard Admin");
        AdminKpiResponse kpis = adminApplicationService.getDashboardKpis();
        return ApiResponse.success("Lấy thông số KPI Dashboard thành công", kpis);
    }

    /**
     * API Lấy danh sách tất cả khách hàng trong hệ thống.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách {@link AdminCustomerResponse}
     */
    @GetMapping("/customers")
    public ApiResponse<List<AdminCustomerResponse>> listCustomers() {
        log.info("REST request: Lấy danh sách khách hàng cho Admin");
        List<AdminCustomerResponse> list = adminApplicationService.listCustomers();
        return ApiResponse.success("Lấy danh sách khách hàng thành công", list);
    }

    /**
     * API Phê duyệt hoặc từ chối trạng thái eKYC của khách hàng.
     *
     * @param customerId ID của khách hàng
     * @param request DTO phê duyệt/từ chối
     * @param adminUsername Tên quản trị viên thực hiện (mặc định 'admin_sys')
     * @return Phản hồi chuẩn {@link ApiResponse} chứa dữ liệu sau khi duyệt
     */
    @PostMapping("/customers/{customerId}/kyc")
    public ApiResponse<AdminCustomerResponse> reviewKyc(
            @PathVariable String customerId,
            @RequestBody ReviewKycRequest request,
            @RequestParam(required = false, defaultValue = "admin_sys") String adminUsername) {
        log.info("REST request: Admin [{}] phê duyệt eKYC khách hàng [{}] -> trạng thái [{}]", adminUsername, customerId, request.getStatus());
        AdminCustomerResponse response = adminApplicationService.reviewKyc(customerId, request, adminUsername);
        return ApiResponse.success("Duyệt hồ sơ eKYC thành công", response);
    }

    /**
     * API Đóng băng / Khóa tài khoản thanh toán khẩn cấp.
     *
     * @param accountId ID tài khoản hoặc số tài khoản
     * @param reason Lý do đóng băng
     * @param adminUsername Tên quản trị viên thực hiện
     * @return Phản hồi chuẩn {@link ApiResponse} xác nhận khóa tài khoản
     */
    @PostMapping("/accounts/{accountId}/freeze")
    public ApiResponse<Void> freezeAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "Phát hiện dấu hiệu rủi ro cao từ Fraud Engine") String reason,
            @RequestParam(required = false, defaultValue = "admin_sys") String adminUsername) {
        log.info("REST request: Admin [{}] khóa khẩn cấp tài khoản [{}] với lý do: [{}]", adminUsername, accountId, reason);
        adminApplicationService.freezeAccount(accountId, reason, adminUsername);
        return ApiResponse.success("Khóa tài khoản khẩn cấp thành công", null);
    }

    /**
     * API Tra cứu danh sách Nhật ký Kiểm toán (Audit Logs) hành động của Quản trị viên.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách {@link AdminAuditLogResponse}
     */
    @GetMapping("/audit-logs")
    public ApiResponse<List<AdminAuditLogResponse>> getAuditLogs() {
        log.info("REST request: Truy vấn danh sách Admin Audit Logs");
        List<AdminAuditLogResponse> logs = adminApplicationService.getAuditLogs();
        return ApiResponse.success("Truy vấn nhật ký kiểm toán thành công", logs);
    }
}
