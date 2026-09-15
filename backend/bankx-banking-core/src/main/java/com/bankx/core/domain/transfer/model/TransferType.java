package com.bankx.core.domain.transfer.model;

/**
 * Loại hình chuyển tiền (Transfer Type).
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum TransferType {
    /** Chuyển tiền nội bộ BankX */
    INTERNAL,

    /** Chuyển tiền nhanh liên ngân hàng NAPAS 24/7 */
    NAPAS247
}
