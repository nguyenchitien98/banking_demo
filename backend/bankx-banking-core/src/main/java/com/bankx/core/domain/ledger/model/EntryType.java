package com.bankx.core.domain.ledger.model;

/**
 * Loại bút toán kế toán ghi sổ kép (Double-Entry Bookkeeping Entry Type).
 *
 * <p>Quy tắc kế toán ngân hàng:
 * <ul>
 *   <li>{@code DEBIT} (Ghi Nợ): Giảm tài khoản tài sản / tăng chi phí. Đối với tài khoản thanh toán tiền gửi, DEBIT làm giảm số dư.</li>
 *   <li>{@code CREDIT} (Ghi Có): Tăng tài khoản nợ/vốn / giảm tài sản. Đối với tài khoản thanh toán tiền gửi, CREDIT làm tăng số dư.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum EntryType {
    /** Bút toán Ghi Nợ (Rút tiền / Chuyển tiền đi) */
    DEBIT,

    /** Bút toán Ghi Có (Nạp tiền / Nhận tiền vào) */
    CREDIT
}
