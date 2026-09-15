package com.bankx.core.presentation.outbox;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.outbox.OutboxService;
import com.bankx.core.infrastructure.outbox.OutboxPollingService;
import com.bankx.core.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller quản lý và giám sát Transactional Outbox Events & Chaos Simulation.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Tự động hóa quá trình serialize phản hồi JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/outbox")}: Tiền tố URL điều khiển Outbox & Chaos Simulator.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/outbox")
public class OutboxController {

    private final OutboxService outboxService;

    public OutboxController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    /**
     * Endpoint lấy danh sách 50 sự kiện Outbox mới nhất cho bảng giám sát.
     *
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link OutboxEventJpaEntity}
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<OutboxEventJpaEntity>>> getRecentEvents(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<OutboxEventJpaEntity> events = outboxService.getRecentEvents();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Outbox Events thành công", events, traceId));
    }

    /**
     * Endpoint kích hoạt thử lại thủ công một sự kiện outbox bị FAILED.
     *
     * @param id ID sự kiện outbox
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link OutboxEventJpaEntity}
     */
    @PostMapping("/events/{id}/retry")
    public ResponseEntity<ApiResponse<OutboxEventJpaEntity>> retryFailedEvent(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        OutboxEventJpaEntity updated = outboxService.retryFailedEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt thử lại sự kiện Outbox", updated, traceId));
    }

    /**
     * Endpoint bật/tắt kịch bản Chaos Simulation ngắt kết nối Kafka (Kafka DOWN vs Kafka UP).
     *
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} trạng thái Kafka mock hiện tại
     */
    @PostMapping("/chaos/toggle-kafka")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleKafkaConnection(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        OutboxPollingService.kafkaMockDisabled = !OutboxPollingService.kafkaMockDisabled;
        boolean isDisabled = OutboxPollingService.kafkaMockDisabled;
        String msg = isDisabled
                ? "⚠️ ĐÃ KÍCH HOẠT CHAOS: Kafka bị ngắt kết nối (Simulated Kafka DOWN)! Các Outbox Event sẽ tạm thời PENDING / FAILED."
                : "✅ ĐÃ PHỤC HỒI KẾT NỐI: Kafka kết nối trở lại bình thường (Simulated Kafka UP)! Poller sẽ đẩy các event PENDING sang Kafka.";

        Map<String, Object> data = Map.of("kafkaDisabled", isDisabled, "statusMessage", msg);
        return ResponseEntity.ok(ApiResponse.success(msg, data, traceId));
    }

    /**
     * Endpoint lấy trạng thái Chaos Simulator hiện tại.
     *
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa boolean {@code kafkaDisabled}
     */
    @GetMapping("/chaos/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChaosStatus(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        Map<String, Object> data = Map.of("kafkaDisabled", OutboxPollingService.kafkaMockDisabled);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái Chaos Simulation", data, traceId));
    }
}
