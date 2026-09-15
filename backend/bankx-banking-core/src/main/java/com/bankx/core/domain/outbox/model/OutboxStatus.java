package com.bankx.core.domain.outbox.model;

/**
 * Trạng thái của Sự kiện trong Transactional Outbox (Outbox Event Status).
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum OutboxStatus {
    /** Đang chờ Polling Service đọc và đẩy sang Kafka */
    PENDING,

    /** Đã publish thành công sang Kafka Topic */
    SENT,

    /** Thất bại khi gửi sang Kafka sau khi thử lại quá số lần quy định (Max retries exceeded) */
    FAILED
}
