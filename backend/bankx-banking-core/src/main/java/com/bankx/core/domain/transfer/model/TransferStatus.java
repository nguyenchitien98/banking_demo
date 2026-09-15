package com.bankx.core.domain.transfer.model;

/**
 * Trạng thái của Lệnh chuyển tiền (Bank Transfer Status).
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum TransferStatus {
    /** Đang chờ xử lý */
    PENDING,

    /** Đang thực thi giao dịch (Đang gọi Saga / Outbox) */
    PROCESSING,

    /** Chuyển tiền thành công */
    COMPLETED,

    /** Chuyển tiền thất bại */
    FAILED
}
