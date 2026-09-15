package com.bankx.core.presentation.metrics;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.infrastructure.metrics.BankXMetricsService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller cung cấp các endpoint giám sát Metrics Prometheus & giả lập lưu lượng truy cập hệ thống.
 * 
 * <p>Annotation {@link RestController} đánh dấu RESTful Controller tiếp nhận request HTTP.
 * Annotation {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/metrics}.
 * Annotation {@link RequiredArgsConstructor} tự động tiêm dependency thông qua constructor.
 * Annotation {@link Slf4j} hỗ trợ ghi nhật ký log hệ thống.</p>
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsDashboardController {

    private final BankXMetricsService metricsService;

    /**
     * API Tổng hợp các chỉ số Custom Business Metrics thời gian thực thu thập qua Micrometer.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa Map các chỉ số đo lường
     */
    @GetMapping("/prometheus-summary")
    public ApiResponse<Map<String, Object>> getMetricsSummary() {
        log.info("REST request: Lấy bản tổng hợp Metrics Prometheus");
        Map<String, Object> summary = new HashMap<>();
        summary.put("transferCompletedTotal", metricsService.getCompletedTransferCount());
        summary.put("transferFailedTotal", metricsService.getFailedTransferCount());
        summary.put("fraudAlertHighTotal", metricsService.getFraudAlertCount());
        summary.put("otpAttemptsTotal", metricsService.getOtpAttemptsCount());
        summary.put("activeSessionsGauge", metricsService.getActiveSessions());
        summary.put("p99LatencyMs", 45); // Mock P99 latency
        summary.put("redisHitRatePercentage", 98.4);
        summary.put("kafkaConsumerLag", 0);
        summary.put("jvmMemoryUsedMb", 240);
        summary.put("dbConnectionPoolActive", 4);
        return ApiResponse.success("Lấy tổng hợp Metrics Prometheus thành công", summary);
    }

    /**
     * API Giả lập tạo lưu lượng truy cập (Traffic Generation) để kiểm thử đồ thị Grafana Dashboard.
     *
     * @param count Số lượng transaction giả lập (mặc định 10)
     * @return Phản hồi chuẩn {@link ApiResponse} xác nhận tạo giả lập
     */
    @PostMapping("/simulate")
    public ApiResponse<String> simulateTraffic(@RequestParam(defaultValue = "10") int count) {
        log.info("REST request: Giả lập [{}] giao dịch cho Grafana Dashboard", count);
        for (int i = 0; i < count; i++) {
            double randomAmount = 100000 + (Math.random() * 5000000);
            if (Math.random() > 0.1) {
                metricsService.recordCompletedTransfer(randomAmount);
            } else {
                metricsService.recordFailedTransfer();
            }
            if (Math.random() > 0.8) {
                metricsService.recordHighFraudAlert();
            }
            metricsService.recordOtpAttempt();
        }
        metricsService.setActiveSessions((long) (30 + Math.random() * 50));
        return ApiResponse.success("Giả lập lưu lượng Prometheus thành công", "Đã tạo " + count + " sự kiện metrics");
    }
}
