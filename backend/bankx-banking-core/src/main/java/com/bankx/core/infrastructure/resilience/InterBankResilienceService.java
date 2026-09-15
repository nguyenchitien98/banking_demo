package com.bankx.core.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service tích hợp Resilience4j bảo vệ kết nối cổng Chuyển tiền Liên ngân hàng (InterBank Adapter).
 * 
 * <p>Annotation {@link Service} đăng ký class này là một Spring Service Bean đảm nhiệm vai trò Fault Tolerance Layer.
 * Tích hợp các mẫu thiết kế chịu lỗi tiên tiến của Resilience4j:
 * <ul>
 *   <li>{@link CircuitBreaker}: Tự động ngắt kết nối khi tỷ lệ lỗi vượt ngưỡng 50%, ngăn chặn quá tải cascading.</li>
 *   <li>{@link Retry}: Thử lại với cơ chế Exponential Backoff cho các giao dịch bị gián đoạn chập chờn.</li>
 *   <li>{@link RateLimiter}: Giới hạn số lượng request tối đa 5 req/s bảo vệ đối tác liên ngân hàng.</li>
 * </ul>
 * </p>
 */
@Service
public class InterBankResilienceService {

    private static final Logger log = LoggerFactory.getLogger(InterBankResilienceService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private boolean externalBankDown = false;
    private long externalBankLatencyMs = 0;
    private final AtomicLong fallbackExecutionCount = new AtomicLong(0);

    public InterBankResilienceService(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Thực thi giao dịch chuyển tiền liên ngân hàng được bảo vệ bởi CircuitBreaker, Retry và RateLimiter.
     *
     * @param accountNumber Số tài khoản nhận tiền
     * @param amount Số tiền chuyển
     * @return Chuỗi thông báo kết quả giao dịch
     */
    @CircuitBreaker(name = "interbankService", fallbackMethod = "fallbackInterBankTransfer")
    @Retry(name = "interbankService")
    @RateLimiter(name = "interbankService")
    public String executeInterBankTransfer(String accountNumber, double amount) {
        log.info("▶ [Resilience4j Call] Đang gọi Cổng Liên Ngân Hàng cho STK [{}] số tiền [{},000 VND]", accountNumber, amount);

        // Giả lập độ trễ rớt mạng hoặc ngắt kết nối
        if (externalBankLatencyMs > 0) {
            try {
                Thread.sleep(externalBankLatencyMs);
            } catch (InterruptedException ignored) {}
        }

        if (externalBankDown) {
            log.error("❌ [External Bank Error] Cổng Liên Ngân Hàng báo lỗi 503 Service Unavailable!");
            throw new RuntimeException("Cổng Liên Ngân Hàng báo lỗi 503 Service Unavailable");
        }

        return String.format("Chuyển thành công %,.0f VND tới tài khoản %s qua Cổng Liên Ngân Hàng", amount, accountNumber);
    }

    /**
     * Phương thức Fallback được gọi tự động khi Circuit Breaker ở trạng thái OPEN hoặc bị từ chối do Rate Limiter.
     *
     * @param accountNumber Số tài khoản nhận
     * @param amount Số tiền
     * @param throwable Ngoại lệ gây ra kích hoạt Fallback
     * @return Thông báo phản hồi Fallback an toàn (Graceful Degradation)
     */
    public String fallbackInterBankTransfer(String accountNumber, double amount, Throwable throwable) {
        long currentFallbacks = fallbackExecutionCount.incrementAndGet();
        log.warn("⚠️ [Resilience4j Fallback Triggered] Cổng Liên ngân hàng ngắt kết nối! Chuyển sang trạng thái PENDING_MANUAL_REVIEW. Lý do: [{}], Tổng số Fallback: [{}]",
                throwable.getMessage(), currentFallbacks);

        return String.format("[Fallback Safe Mode] Giao dịch %,.0f VND tới STK %s đã chuyển sang trạng thái CHỜ DUYỆT BẰNG TAY (Pending Review). Hệ thống tạm thời ngắt kết nối đối tác để bảo vệ giao dịch!",
                amount, accountNumber);
    }

    /**
     * Lấy trạng thái Circuit Breaker hiện tại (CLOSED, OPEN, HALF_OPEN) và các chỉ số đo lường.
     *
     * @return Map chứa thông số trạng thái Circuit Breaker, Fallbacks, Rate Limiter
     */
    public Map<String, Object> getResilienceStatus() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("interbankService");

        Map<String, Object> status = new HashMap<>();
        status.put("circuitState", cb.getState().name()); // CLOSED, OPEN, HALF_OPEN
        status.put("failureRatePercentage", cb.getMetrics().getFailureRate());
        status.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
        status.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
        status.put("numberOfBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
        status.put("fallbackExecutionCount", fallbackExecutionCount.get());
        status.put("externalBankDown", externalBankDown);
        status.put("externalBankLatencyMs", externalBankLatencyMs);
        return status;
    }

    /**
     * Đổi trạng thái giả lập đối tác liên ngân hàng bị DOWN hoặc SLOW.
     *
     * @param isDown True nếu đối tác rớt mạng
     * @param latencyMs Độ trễ giả lập (ms)
     */
    public void toggleExternalBankSimulation(boolean isDown, long latencyMs) {
        this.externalBankDown = isDown;
        this.externalBankLatencyMs = latencyMs;
        log.info("⚙️ [Resilience Simulation] Cập nhật Cổng Liên ngân hàng: Down=[{}], Latency=[{}ms]", isDown, latencyMs);
    }

    /**
     * Reset Circuit Breaker về trạng thái ban đầu (CLOSED).
     */
    public void resetCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("interbankService");
        cb.reset();
        fallbackExecutionCount.set(0);
        log.info("🔄 [Circuit Breaker Reset] Khôi phục trạng thái Circuit Breaker về CLOSED");
    }
}
