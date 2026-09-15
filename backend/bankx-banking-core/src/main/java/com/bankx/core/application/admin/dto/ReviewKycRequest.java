package com.bankx.core.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) nhận yêu cầu phê duyệt hoặc từ chối hồ sơ eKYC từ Quản trị viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewKycRequest {
    /** Trạng thái mới ("VERIFIED" hoặc "REJECTED") */
    private String status;

    /** Lý do duyệt hoặc từ chối hồ sơ eKYC */
    private String reason;
}
