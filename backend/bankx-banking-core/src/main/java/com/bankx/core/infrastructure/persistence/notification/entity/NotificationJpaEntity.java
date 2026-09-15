package com.bankx.core.infrastructure.persistence.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity JPA lưu trữ bảng thông báo ứng dụng (notifications).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Entity}: Đánh dấu Class là JPA Entity tương ứng với bảng {@code notifications}.</li>
 *   <li>{@code @Table(name = "notifications")}: Khai báo bảng tương ứng trong CSDL Postgres.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationJpaEntity() {}

    public NotificationJpaEntity(UUID id, UUID customerId, String title, String content, String type,
                                boolean isRead, String referenceId, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.customerId = customerId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.isRead = isRead;
        this.referenceId = referenceId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void markAsRead() {
        this.isRead = true;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
