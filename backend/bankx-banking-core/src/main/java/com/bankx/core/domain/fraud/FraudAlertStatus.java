package com.bankx.core.domain.fraud;

/**
 * Trạng thái duyệt Cảnh báo Gian lận (Fraud Alert Status).
 */
public enum FraudAlertStatus {
    /** Đang chờ Admin/Kỹ sư kiểm tra */
    PENDING_REVIEW,

    /** Đã xác nhận giao dịch gian lận và xử lý chặn */
    RESOLVED,

    /** Cảnh báo nhầm (Báo động giả) */
    FALSE_POSITIVE
}
