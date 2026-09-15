package com.bankx.core.infrastructure.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Service quản lý vết vết phân tán (Distributed Tracing) sử dụng OpenTelemetry & Micrometer Tracing.
 * 
 * <p>Annotation {@link Service} đăng ký class này là một Spring Service Bean đảm nhiệm khởi tạo, khởi tạo các Spans thủ công
 * cho các tác vụ nghiệp vụ quan trọng (`transfer.validate`, `transfer.debit`, `transfer.credit`, `transfer.ledger`, `fraud.evaluate`)
 * và đính kèm `traceId`, `spanId` vào MDC (Mapped Diagnostic Context) của SLF4J.</p>
 * 
 * <p><b>Chuẩn Tracing W3C Header {@code traceparent}:</b>
 * Định dạng chuẩn: {@code 00-{traceId}-{spanId}-01}. Giúp truyền ngữ cảnh vết vết qua HTTP Header và Kafka Headers
 * giữa các microservices bất đồng bộ.</p>
 */
@Service
public class TraceService {

    private static final Logger log = LoggerFactory.getLogger(TraceService.class);

    private final Tracer tracer;

    public TraceService(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Lấy TraceId hiện tại từ Micrometer Tracer hoặc MDC.
     *
     * @return Mã TraceId (Chuỗi Hex 32 ký tự)
     */
    public String getCurrentTraceId() {
        if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
            return tracer.currentSpan().context().traceId();
        }
        String mdcTraceId = MDC.get("traceId");
        return mdcTraceId != null ? mdcTraceId : UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Lấy SpanId hiện tại từ Micrometer Tracer hoặc MDC.
     *
     * @return Mã SpanId (Chuỗi Hex 16 ký tự)
     */
    public String getCurrentSpanId() {
        if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
            return tracer.currentSpan().context().spanId();
        }
        String mdcSpanId = MDC.get("spanId");
        return mdcSpanId != null ? mdcSpanId : UUID.randomUUID().toString().substring(0, 16);
    }

    /**
     * Sinh chuỗi W3C {@code traceparent} Header.
     *
     * @return Chuỗi W3C traceparent (vd: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01)
     */
    public String getW3cTraceParent() {
        String traceId = getCurrentTraceId();
        String spanId = getCurrentSpanId();
        return String.format("00-%s-%s-01", traceId, spanId);
    }

    /**
     * Thực thi một khối nghiệp vụ bên trong một Custom Span riêng biệt và tự động đóng Span sau khi xong.
     *
     * @param spanName Tên đại diện cho tác vụ nghiệp vụ (vd: transfer.debit)
     * @param supplier Khối lệnh cần thực thi
     * @param <T> Kiểu dữ liệu kết quả trả về
     * @return Kết quả từ khối lệnh
     */
    public <T> T executeInSpan(String spanName, Supplier<T> supplier) {
        Span newSpan = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan)) {
            MDC.put("traceId", newSpan.context().traceId());
            MDC.put("spanId", newSpan.context().spanId());
            log.info("▶ [SPAN START] Operations [{}] trong TraceId [{}]", spanName, newSpan.context().traceId());
            return supplier.get();
        } catch (Exception ex) {
            newSpan.error(ex);
            log.error("❌ [SPAN ERROR] Operations [{}] gặp lỗi: {}", spanName, ex.getMessage());
            throw ex;
        } finally {
            newSpan.end();
            log.info("⏹ [SPAN END] Completed [{}]", spanName);
        }
    }

    /**
     * DTO đại diện cho một Span trong cây biểu đồ Thác nước (Waterfall Tree) vết vết.
     */
    public record SpanNode(
            String spanName,
            String serviceName,
            String spanId,
            long durationMs,
            String status,
            List<SpanNode> children
    ) {}

    /**
     * Mô phỏng cây vết vết phân tán (Distributed Trace Tree) từ API Gateway -> Service Core -> Outbox -> Kafka.
     *
     * @param traceId Mã Trace ID duy nhất
     * @return {@link SpanNode} gốc đại diện cho toàn bộ luồng giao dịch
     */
    public SpanNode generateTraceWaterfall(String traceId) {
        SpanNode childNotification = new SpanNode("notification.send_push", "bankx-notification-service", "span-006", 12, "COMPLETED", new ArrayList<>());
        SpanNode childKafka = new SpanNode("kafka.consume_event", "bankx-notification-service", "span-005", 28, "COMPLETED", List.of(childNotification));
        SpanNode childLedger = new SpanNode("transfer.ledger_double_entry", "bankx-banking-core", "span-004", 35, "COMPLETED", List.of(childKafka));
        SpanNode childDebit = new SpanNode("transfer.debit_account", "bankx-banking-core", "span-003", 22, "COMPLETED", new ArrayList<>());
        SpanNode childFraud = new SpanNode("fraud.evaluate_risk", "bankx-banking-core", "span-002", 18, "COMPLETED", new ArrayList<>());
        
        List<SpanNode> coreChildren = List.of(childFraud, childDebit, childLedger);
        return new SpanNode("HTTP POST /api/v1/transfers/internal", "bankx-api-gateway", "span-001", 125, "COMPLETED", coreChildren);
    }
}
