package com.bankx.core.presentation.tracing;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.infrastructure.tracing.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller quản lý vết vết phân tán OpenTelemetry & Jaeger Distributed Tracing.
 * 
 * <p>Annotation {@link RestController} đánh dấu class này tiếp nhận request HTTP dạng JSON.
 * Annotation {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/tracing}.
 * Annotation {@link RequiredArgsConstructor} tự động tiêm dependency qua constructor.
 * Annotation {@link Slf4j} hỗ trợ ghi nhật ký log với `traceId` và `spanId` đính kèm.</p>
 */
@RestController
@RequestMapping("/api/v1/tracing")
@RequiredArgsConstructor
@Slf4j
public class TracingController {

    private final TraceService traceService;

    /**
     * API Lấy ngữ cảnh Tracing hiện tại của HTTP Request (TraceID, SpanID, W3C traceparent Header).
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa Map các thuộc tính vết vết
     */
    @GetMapping("/current")
    public ApiResponse<Map<String, String>> getCurrentTraceContext() {
        log.info("REST request: Lấy ngữ cảnh Distributed Tracing W3C hiện tại");
        Map<String, String> context = new HashMap<>();
        context.put("traceId", traceService.getCurrentTraceId());
        context.put("spanId", traceService.getCurrentSpanId());
        context.put("traceparent", traceService.getW3cTraceParent());
        context.put("serviceName", "bankx-banking-core");
        context.put("mdcCorrelation", "Enriched via SLF4J MDC Pattern");
        return ApiResponse.success("Lấy ngữ cảnh Tracing thành công", context);
    }

    /**
     * API Giả lập tạo chuỗi Spans phân tán (Span Tree Waterfall) cho luồng giao dịch chuyển tiền end-to-end.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa cây biểu đồ {@link TraceService.SpanNode}
     */
    @PostMapping("/simulate-span-tree")
    public ApiResponse<TraceService.SpanNode> simulateDistributedTrace() {
        String traceId = traceService.getCurrentTraceId();
        log.info("REST request: Giả lập Distributed Trace Tree cho TraceId [{}]", traceId);

        // Chạy qua các Custom Spans nghiệp vụ
        traceService.executeInSpan("transfer.validate_rules", () -> {
            log.info("Xác thực quy tắc tài khoản & số dư");
            return true;
        });

        traceService.executeInSpan("fraud.evaluate_risk_score", () -> {
            log.info("Đánh giá điểm rủi ro giao dịch (Risk Score: 15/100)");
            return "ALLOW";
        });

        traceService.executeInSpan("transfer.debit_and_credit", () -> {
            log.info("Bút toán nợ có và cập nhật số dư");
            return "SUCCESS";
        });

        TraceService.SpanNode waterfall = traceService.generateTraceWaterfall(traceId);
        return ApiResponse.success("Tạo giả lập Distributed Trace thành công", waterfall);
    }
}
