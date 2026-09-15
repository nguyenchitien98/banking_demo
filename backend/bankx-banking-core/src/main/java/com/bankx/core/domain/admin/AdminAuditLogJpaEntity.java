package com.bankx.core.domain.admin;

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
 * JPA Entity biểu diễn Nhật ký Kiểm toán Quản trị viên {@code admin_audit_logs}.
 * 
 * <p>Annotation {@link Entity} đánh dấu đây là một JPA Entity quản lý trạng thái vết thao tác quản trị.
 * Annotation {@link Table} ánh xạ entity vào bảng {@code admin_audit_logs} trong cơ sở dữ liệu.
 * Các annotation Lombok như {@link Data}, {@link Builder}, {@link NoArgsConstructor}, {@link AllArgsConstructor}
 * hỗ trợ sinh tự động mã getter/setter, builder pattern và constructor đầy đủ tham số.</p>
 */
@Entity
@Table(name = "admin_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "admin_username", length = 128, nullable = false)
    private String adminUsername;

    @Column(name = "admin_role", length = 32, nullable = false)
    private String adminRole;

    @Column(name = "action_type", length = 64, nullable = false)
    private String actionType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "details", length = 255)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
