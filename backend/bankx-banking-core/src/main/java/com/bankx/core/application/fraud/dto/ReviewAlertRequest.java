package com.bankx.core.application.fraud.dto;

import com.bankx.core.domain.fraud.FraudAlertStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO yêu cầu duyệt Cảnh báo Gian lận.
 * 
 * @param newStatus Trạng thái mới (RESOLVED, FALSE_POSITIVE)
 * @param reviewerNotes Ghi chú xử lý của Admin/Kỹ sư
 */
public record ReviewAlertRequest(
        @NotNull(message = "Trạng thái mới không được để trống")
        FraudAlertStatus newStatus,

        String reviewerNotes
) {
}
