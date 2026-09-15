package com.bankx.core.domain.beneficiary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA Entity biểu diễn Danh bạ Người thụ hưởng {@code beneficiaries}.
 * 
 * <p>Sử dụng các annotation Lombok {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * để tự động hóa mã lặp lại. Annotation {@link Entity} chỉ định JPA Entity và {@link Table} ánh xạ vào CSDL.</p>
 * 
 * <p>Quản lý trường {@code transferCount} để theo dõi tần suất chuyển tiền và hỗ trợ gợi ý
 * người nhận thường xuyên nhất cho giao diện người dùng.</p>
 */
@Entity
@Table(name = "beneficiaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "account_number", length = 64, nullable = false)
    private String accountNumber;

    @Column(name = "bank_bin", length = 16, nullable = false)
    private String bankBin;

    @Column(name = "bank_name", length = 128, nullable = false)
    private String bankName;

    @Column(name = "account_holder_name", length = 128, nullable = false)
    private String accountHolderName;

    @Column(name = "nickname", length = 128)
    private String nickname;

    @Column(name = "transfer_count", nullable = false)
    private Integer transferCount;

    @Column(name = "last_transfer_at", nullable = false)
    private Instant lastTransferAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
