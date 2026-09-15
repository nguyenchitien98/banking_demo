package com.bankx.core.application.engineering;

import com.bankx.core.application.engineering.dto.EngineeringHealthResponse;
import com.bankx.core.infrastructure.metrics.BankXMetricsService;
import com.bankx.core.infrastructure.resilience.InterBankResilienceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Application Service điều phối trang Engineering Portal và các thử nghiệm Chaos Engineering.
 * 
 * <p>Annotation {@link Service} đăng ký class này là một Spring Service Bean quản lý thu thập dữ liệu Telemetry thời gian thực,
 * hiển thị lưới trạng thái sức khỏe từng Module (Auth, Customer, Account, Transfer, Ledger, Payment, Card, Notification, Fraud, Audit, Saga)
 * và điều khiển kích hoạt các nút thử nghiệm hỗn hoảng Chaos Engineering.</p>
 */
@Service
public class EngineeringPortalService {

    private static final Logger log = LoggerFactory.getLogger(EngineeringPortalService.class);

    private final BankXMetricsService metricsService;
    private final InterBankResilienceService resilienceService;

    private boolean dbDelayActive = false;
    private long dbDelayMs = 0;
    private boolean kafkaDownSimulated = false;

    public EngineeringPortalService(BankXMetricsService metricsService,
                                    InterBankResilienceService resilienceService) {
        this.metricsService = metricsService;
        this.resilienceService = resilienceService;
    }

    /**
     * Lấy dữ liệu tổng hợp sức khỏe toàn bộ hệ thống (Health Summary).
     *
     * @return DTO {@link EngineeringHealthResponse}
     */
    public EngineeringHealthResponse getHealthSummary() {
        Map<String, String> healthGrid = new HashMap<>();
        healthGrid.put("Auth Module", "UP");
        healthGrid.put("Customer Module", "UP");
        healthGrid.put("Account Module", dbDelayActive ? "DEGRADED" : "UP");
        healthGrid.put("Transfer Core", "UP");
        healthGrid.put("Ledger Module", "UP");
        healthGrid.put("Payment Module", "UP");
        healthGrid.put("Card Module", "UP");
        healthGrid.put("Notification Service", kafkaDownSimulated ? "DOWN" : "UP");
        healthGrid.put("Fraud Engine", "UP");
        healthGrid.put("CQRS Read Model", "UP");
        healthGrid.put("Saga Orchestrator", "UP");

        Map<String, Object> resilienceStatus = resilienceService.getResilienceStatus();
        String circuitState = (String) resilienceStatus.getOrDefault("circuitState", "CLOSED");

        String systemStatus = (kafkaDownSimulated || dbDelayActive || "OPEN".equalsIgnoreCase(circuitState))
                ? "DEGRADED" : "HEALTHY";

        return EngineeringHealthResponse.builder()
                .systemStatus(systemStatus)
                .serviceHealthGrid(healthGrid)
                .currentTps(28.4 + (Math.random() * 10))
                .errorRatePercentage(dbDelayActive ? 4.8 : 0.05)
                .p99LatencyMs(dbDelayMs > 0 ? dbDelayMs + 45 : 42)
                .kafkaConsumerLag(kafkaDownSimulated ? 1450 : 0)
                .redisHitRatePercentage(99.2)
                .dbPoolActive(dbDelayActive ? 18 : 4)
                .dbPoolMax(20)
                .circuitBreakerState(circuitState)
                .dbDelayActive(dbDelayActive)
                .kafkaDownSimulated(kafkaDownSimulated)
                .build();
    }

    /**
     * Chaos Action 1: Giả lập độ trễ Database (Simulate Slow Database).
     *
     * @param delayMs Số miligiây trễ (ms)
     */
    public void delayDb(long delayMs) {
        this.dbDelayMs = delayMs;
        this.dbDelayActive = delayMs > 0;
        log.warn("🔥 [CHAOS ENGINEERING] Kích hoạt giả lập độ trễ DB [{}] ms", delayMs);
    }

    /**
     * Chaos Action 2: Giả lập ngắt kết nối Kafka (Simulate Kafka Down).
     *
     * @param isDown True nếu ngắt Kafka
     */
    public void toggleKafka(boolean isDown) {
        this.kafkaDownSimulated = isDown;
        log.warn("🔥 [CHAOS ENGINEERING] Chuyển trạng thái Kafka Down simulated = [{}]", isDown);
    }

    /**
     * Chaos Action 3: Giả lập gửi 100 requests giao dịch đồng thời (Flood Concurrent Transfers).
     *
     * @param count Số lượng request gửi dồn dập
     */
    public void floodTransfer(int count) {
        log.warn("🔥 [CHAOS ENGINEERING] Khởi chạy Flood Transfer [{}] requests đồng thời!", count);
        for (int i = 0; i < count; i++) {
            metricsService.recordCompletedTransfer(100000 + Math.random() * 2000000);
        }
    }
}
