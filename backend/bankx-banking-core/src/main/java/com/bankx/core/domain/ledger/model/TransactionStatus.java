package com.bankx.core.domain.ledger.model;

/**
 * Trạng thái xử lý của Giao dịch ngân hàng (Banking Transaction Status).
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum TransactionStatus {
    /** Giao dịch thành công và đã hạch toán sổ kép */
    SUCCESS,

    /** Giao dịch đang được xử lý (Saga pattern) */
    PENDING,

    /** Giao dịch thất bại / bị từ chối */
    FAILED
}
