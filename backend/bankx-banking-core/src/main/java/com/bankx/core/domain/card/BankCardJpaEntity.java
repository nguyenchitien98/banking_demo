package com.bankx.core.domain.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity biểu diễn bảng Thẻ ngân hàng {@code bank_cards}.
 * 
 * <p>Sử dụng các annotation Lombok {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * để giảm bớt mã thừa. Annotation {@link Entity} chỉ định JPA Entity và {@link Table} chỉ định tên bảng.</p>
 * 
 * <p><b>Tuân thủ PCI-DSS Tokenization:</b>
 * Số thẻ PAN gốc KHÔNG BAO GIỜ được lưu trực tiếp vào CSDL. CSDL chỉ lưu chuỗi {@code maskedPan}
 * (ví dụ: {@code 4000 12** **** 8899}) và {@code panToken} duy nhất để xác thực giao dịch.</p>
 */
@Entity
@Table(name = "bank_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCardJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "account_number", length = 64, nullable = false)
    private String accountNumber;

    @Column(name = "card_holder_name", length = 128, nullable = false)
    private String cardHolderName;

    @Column(name = "masked_pan", length = 32, nullable = false)
    private String maskedPan;

    @Column(name = "pan_token", length = 128, nullable = false, unique = true)
    private String panToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", length = 32, nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", length = 32, nullable = false)
    private CardBrand cardBrand;

    @Column(name = "expiry_month", length = 2, nullable = false)
    private String expiryMonth;

    @Column(name = "expiry_year", length = 2, nullable = false)
    private String expiryYear;

    @Column(name = "spending_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal spendingLimit;

    @Column(name = "daily_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal dailyLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private CardStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
