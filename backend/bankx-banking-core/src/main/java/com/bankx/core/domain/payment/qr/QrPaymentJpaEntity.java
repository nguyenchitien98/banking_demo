package com.bankx.core.domain.payment.qr;

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
 * JPA Entity biểu diễn lịch sử thực thi giao dịch thanh toán qua mã QR (VietQR).
 * 
 * <p>Sử dụng các annotation Lombok {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * để tự động tạo boilerplate code. Annotation {@link Entity} đánh dấu lớp được ánh xạ vào CSDL
 * và {@link Table} chỉ định bảng target {@code qr_payments}.</p>
 */
@Entity
@Table(name = "qr_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "source_account_id", length = 64, nullable = false)
    private String sourceAccountId;

    @Column(name = "target_account_number", length = 64, nullable = false)
    private String targetAccountNumber;

    @Column(name = "target_bank_bin", length = 16, nullable = false)
    private String targetBankBin;

    @Column(name = "target_account_name", length = 128)
    private String targetAccountName;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false)
    private String qrPayload;

    @Column(name = "is_dynamic")
    private Boolean isDynamic;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
