package com.bankx.core.domain.cqrs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity biểu diễn CQRS Read Model {@code transaction_history_views}.
 * 
 * <p>Annotation {@link Entity} chỉ định JPA Entity quản lý trạng thái Read Model phi chuẩn hóa (Denormalized)
 * chuyên phục vụ truy vấn lịch sử giao dịch tốc độ cao.
 * Annotation {@link Table} ánh xạ entity vào bảng {@code transaction_history_views} trong cơ sở dữ liệu.
 * Các annotation Lombok {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * hỗ trợ tự động sinh mã boilerplate getter/setter và builder pattern.</p>
 */
@Entity
@Table(name = "transaction_history_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryViewJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "transaction_reference", length = 64, nullable = false)
    private String transactionReference;

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "account_number", length = 64, nullable = false)
    private String accountNumber;

    @Column(name = "opposite_account_number", length = 64)
    private String oppositeAccountNumber;

    @Column(name = "opposite_account_name", length = 128)
    private String oppositeAccountName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "direction", length = 16, nullable = false)
    private String direction;

    @Column(name = "transaction_type", length = 32, nullable = false)
    private String transactionType;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
