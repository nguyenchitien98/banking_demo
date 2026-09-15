package com.bankx.core.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) hiển thị chi tiết thông tin khách hàng dành cho giao diện Quản trị viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String identityNumber;
    private String kycStatus;
    private String accountStatus;
    private Instant createdAt;
}
