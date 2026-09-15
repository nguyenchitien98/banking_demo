package com.bankx.core.presentation.engineering;

import com.bankx.core.application.engineering.EngineeringPortalService;
import com.bankx.core.application.engineering.dto.EngineeringHealthResponse;
import com.bankx.common.dto.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý các Endpoint Engineering Portal và kích hoạt Chaos Engineering.
 * 
 * <p>Annotation {@link RestController} đánh dấu class này là một Spring REST API component phục vụ các phản hồi JSON.
 * Annotation {@link RequestMapping} định tuyến các request HTTP với đường dẫn cơ sở '/api/v1/engineering'.</p>
 */
@RestController
@RequestMapping("/api/v1/engineering")
public class EngineeringController {

    private final EngineeringPortalService engineeringPortalService;

    public EngineeringController(EngineeringPortalService engineeringPortalService) {
        this.engineeringPortalService = engineeringPortalService;
    }

    /**
     * Lấy dữ liệu Telemetry tổng hợp sức khỏe toàn bộ hệ thống ngân hàng (Health Summary Grid).
     *
     * @return {@link ResponseEntity} chứa {@link ApiResponse} bọc dữ liệu {@link EngineeringHealthResponse}
     */
    @GetMapping("/health-summary")
    public ResponseEntity<ApiResponse<EngineeringHealthResponse>> getHealthSummary() {
        EngineeringHealthResponse healthResponse = engineeringPortalService.getHealthSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sức khỏe hệ thống thành công", healthResponse));
    }

    /**
     * Kích hoạt thử nghiệm Chaos Engineering: Giả lập độ trễ Database.
     *
     * @param delayMs Số miligiây trễ (ms)
     * @return {@link ResponseEntity} chứa {@link ApiResponse} phản hồi kết quả kích hoạt
     */
    @PostMapping("/chaos/delay-db")
    public ResponseEntity<ApiResponse<Void>> delayDb(@RequestParam(defaultValue = "2000") long delayMs) {
        engineeringPortalService.delayDb(delayMs);
        return ResponseEntity.ok(ApiResponse.success("Đã cài đặt giả lập độ trễ DB: " + delayMs + "ms", null));
    }

    /**
     * Kích hoạt thử nghiệm Chaos Engineering: Giả lập ngắt kết nối Kafka Consumer/Producer.
     *
     * @param isDown True để simulated Kafka down, False để khôi phục
     * @return {@link ResponseEntity} chứa {@link ApiResponse} phản hồi kết quả kích hoạt
     */
    @PostMapping("/chaos/toggle-kafka")
    public ResponseEntity<ApiResponse<Void>> toggleKafka(@RequestParam boolean isDown) {
        engineeringPortalService.toggleKafka(isDown);
        String msg = isDown ? "Đã giả lập ngắt kết nối Kafka (Down)" : "Đã khôi phục kết nối Kafka (Up)";
        return ResponseEntity.ok(ApiResponse.success(msg, null));
    }

    /**
     * Kích hoạt thử nghiệm Chaos Engineering: Tải cao dồn dập 100 giao dịch đồng thời (Flood Transfer).
     *
     * @param count Số lượng giao dịch giả lập dồn dập (mặc định 100)
     * @return {@link ResponseEntity} chứa {@link ApiResponse} phản hồi kết quả kích hoạt
     */
    @PostMapping("/chaos/flood-transfer")
    public ResponseEntity<ApiResponse<Void>> floodTransfer(@RequestParam(defaultValue = "100") int count) {
        engineeringPortalService.floodTransfer(count);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt Flood Transfer với " + count + " requests đồng thời", null));
    }
}
