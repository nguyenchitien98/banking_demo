package com.bankx.core.infrastructure.persistence.payment.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity JPA lưu trữ thông tin bảng nhà cung cấp dịch vụ (payment_providers).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Entity}: Đánh dấu Class tương ứng với bảng {@code payment_providers}.</li>
 *   <li>{@code @Table(name = "payment_providers")}: Ánh xạ bảng trong CSDL Postgres.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Entity
@Table(name = "payment_providers")
public class PaymentProviderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PaymentProviderJpaEntity() {}

    public PaymentProviderJpaEntity(UUID id, String code, String name, String category, String logoUrl, String status, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.category = category;
        this.logoUrl = logoUrl;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
