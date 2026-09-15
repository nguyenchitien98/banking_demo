package com.bankx.core.application.engineering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object (DTO) chứa bản tổng hợp sức khỏe toàn bộ hệ thống ngân hàng (Engineering Portal Health Summary).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringHealthResponse {
    /** Trạng thái tổng quan hệ thống ("HEALTHY", "DEGRADED", "CRITICAL") */
    private String systemStatus;

    /** Bản đồ trạng thái từng module dịch vụ (vd: Auth -> UP, Transfer -> DEGRADED) */
    private Map<String, String> serviceHealthGrid;

    /** Tải lượng giao dịch thời gian thực (Transactions Per Second - TPS) */
    private double currentTps;

    /** Tỷ lệ lỗi giao dịch thời gian thực (%) */
    private double errorRatePercentage;

    /** Độ trễ P99 xử lý giao dịch hệ thống (ms) */
    private double p99LatencyMs;

    /** Kafka Consumer Group Lag hiện tại */
    private long kafkaConsumerLag;

    /** Tỷ lệ Redis Cache Hit Rate (%) */
    private double redisHitRatePercentage;

    /** Trạng thái HikariCP DB Connection Pool */
    private int dbPoolActive;
    private int dbPoolMax;

    /** Trạng thái Circuit Breaker ("CLOSED", "OPEN", "HALF_OPEN") */
    private String circuitBreakerState;

    /** Cờ đánh dấu các trạng thái Chaos đang diễn ra */
    private boolean dbDelayActive;
    private boolean kafkaDownSimulated;
}
