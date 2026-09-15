package com.bankx.core.presentation.resilience;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.infrastructure.resilience.InterBankResilienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller quản lý mẫu thiết kế chịu lỗi Resilience4j Circuit Breaker & Rate Limiting.
 * 
 * <p>Annotation {@link RestController} khai báo RESTful Controller tiếp nhận HTTP Requests.
 * Annotation {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/resilience}.
 * Annotation {@link RequiredArgsConstructor} tự động tiêm dependency qua constructor.
 * Annotation {@link Slf4j} hỗ trợ ghi log hệ thống.</p>
 */
@RestController
@RequestMapping("/api/v1/resilience")
@RequiredArgsConstructor
@Slf4j
public class ResilienceController {

    private final InterBankResilienceService resilienceService;

    /**
     * API Thực hiện giao dịch chuyển tiền liên ngân hàng được bảo vệ bởi Circuit Breaker & Rate Limiter.
     *
     * @param accountNumber Số tài khoản nhận tiền
     * @param amount Số tiền chuyển
     * @return Phản hồi chuẩn {@link ApiResponse} chứa kết quả hoặc phản hồi Fallback
     */
    @PostMapping("/interbank/transfer")
    public ApiResponse<String> executeInterBankTransfer(
            @RequestParam(defaultValue = "970422001999") String accountNumber,
            @RequestParam(defaultValue = "500000") double amount) {
        log.info("REST request: Gọi Cổng Liên ngân hàng chuyển tiền tới [{}]", accountNumber);
        String result = resilienceService.executeInterBankTransfer(accountNumber, amount);
        return ApiResponse.success("Kết quả xử lý Cổng Liên ngân hàng", result);
    }

    /**
     * API Tra cứu trạng thái hiện tại của Circuit Breaker (CLOSED, OPEN, HALF_OPEN) và các chỉ số đo lường.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa Map trạng thái
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getResilienceStatus() {
        log.info("REST request: Truy vấn trạng thái Circuit Breaker Resilience4j");
        Map<String, Object> status = resilienceService.getResilienceStatus();
        return ApiResponse.success("Lấy trạng thái Circuit Breaker thành công", status);
    }

    /**
     * API Bật / Tắt trạng thái giả lập đối tác liên ngân hàng rớt mạng hoặc chập chờn.
     *
     * @param isDown True nếu giả lập đối tác bị DOWN
     * @param latencyMs Độ trễ giả lập (ms)
     * @return Phản hồi chuẩn {@link ApiResponse} xác nhận cài đặt
     */
    @PostMapping("/simulate/toggle-external-bank")
    public ApiResponse<String> toggleSimulation(
            @RequestParam(defaultValue = "true") boolean isDown,
            @RequestParam(defaultValue = "0") long latencyMs) {
        log.info("REST request: Cài đặt giả lập Cổng Liên ngân hàng isDown=[{}], latency=[{}ms]", isDown, latencyMs);
        resilienceService.toggleExternalBankSimulation(isDown, latencyMs);
        return ApiResponse.success("Cập nhật giả lập đối tác liên ngân hàng thành công", "Down=" + isDown + ", Latency=" + latencyMs + "ms");
    }

    /**
     * API Khôi phục (Reset) Circuit Breaker về trạng thái ban đầu CLOSED.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} xác nhận reset
     */
    @PostMapping("/reset")
    public ApiResponse<String> resetCircuitBreaker() {
        log.info("REST request: Khôi phục trạng thái Circuit Breaker về CLOSED");
        resilienceService.resetCircuitBreaker();
        return ApiResponse.success("Reset Circuit Breaker thành công", "Trạng thái đã khôi phục về CLOSED");
    }
}
