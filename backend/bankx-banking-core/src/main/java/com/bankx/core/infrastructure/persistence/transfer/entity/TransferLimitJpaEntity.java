package com.bankx.core.infrastructure.persistence.transfer.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity JPA lưu trữ bảng hạn mức chuyển tiền (transfer_limits).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Entity}: Đánh dấu Class là JPA Entity đại diện cho bảng {@code transfer_limits}.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Entity
@Table(name = "transfer_limits")
public class TransferLimitJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "single_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal singleLimit;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TransferLimitJpaEntity() {}

    public TransferLimitJpaEntity(UUID id, UUID customerId, BigDecimal singleLimit, BigDecimal dailyLimit, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.singleLimit = singleLimit;
        this.dailyLimit = dailyLimit;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getSingleLimit() {
        return singleLimit;
    }

    public void setSingleLimit(BigDecimal singleLimit) {
        this.singleLimit = singleLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
