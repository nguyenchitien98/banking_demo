package com.bankx.core.domain.ledger.model;

/**
 * Loại giao dịch ngân hàng (Banking Transaction Type).
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum TransactionType {
    /** Chuyển tiền nội bộ giữa các tài khoản BankX */
    INTERNAL_TRANSFER,

    /** Nạp tiền vào tài khoản */
    DEPOSIT,

    /** Rút tiền khỏi tài khoản */
    WITHDRAWAL,

    /** Thanh toán hóa đơn (Điện, Nước, Internet...) */
    BILL_PAYMENT
}
