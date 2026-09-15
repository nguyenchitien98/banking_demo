package com.bankx.core.controller;

import com.bankx.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller kiểm tra sức khỏe dịch vụ (Health Check) và tích hợp API Envelope.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Đánh dấu lớp xử lý các HTTP Request, kết hợp giữa {@code @Controller}
 *       và {@code @ResponseBody}. Mọi giá trị trả về từ các method sẽ được Spring tự động serialize thành JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/health")}: Cấu hình tiền tố đường dẫn URI cho tất cả các endpoint trong controller này.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * Endpoint kiểm tra phản hồi từ Banking Core service kèm traceId phân vết.
     *
     * @param traceId Mã định danh trace ID truyền từ Header {@code X-Trace-Id} do API Gateway gán
     * @return {@link ResponseEntity} chứa {@link ApiResponse} phản hồi trạng thái hoạt động của hệ thống
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> checkHealth(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        ApiResponse<String> response = ApiResponse.success(
                "BankX Banking Core Service đang hoạt động bình thường",
                "UP",
                traceId
        );
        return ResponseEntity.ok(response);
    }
}
