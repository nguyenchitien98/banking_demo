package com.bankx.core.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) chứa chi tiết của một bản ghi nhật ký kiểm toán quản trị hệ thống.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {
    private String id;
    private String adminUsername;
    private String adminRole;
    private String actionType;
    private String targetId;
    private String details;
    private Instant createdAt;
}
