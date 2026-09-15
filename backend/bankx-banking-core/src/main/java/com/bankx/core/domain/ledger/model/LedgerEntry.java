package com.bankx.core.domain.ledger.model;

import com.bankx.core.domain.account.model.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Bút toán kế toán đơn lẻ trong hệ thống ghi sổ kép (Single Ledger Entry Domain Model).
 *
 * <p>Mỗi bút toán gắn liền với một giao dịch tài chính bất biến (IMMUTABLE).
 * Hệ thống ghi sổ kép đảm bảo mọi thay đổi số dư tài khoản đều xuất phát từ bút toán này.
 * </p>
 *
 * @param id ID duy nhất của bút toán
 * @param transactionId ID của giao dịch tổng liên quan
 * @param accountId ID tài khoản ngân hàng chịu tác động
 * @param entryType Loại bút toán (DEBIT hoặc CREDIT)
 * @param amount Số tiền hạch toán
 * @param balanceAfter Số dư tài khoản ngay sau khi ghi bút toán
 * @param createdAt Thời điểm ghi nhận bút toán (Bất biến)
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record LedgerEntry(
        UUID id,
        UUID transactionId,
        UUID accountId,
        EntryType entryType,
        Money amount,
        Money balanceAfter,
        Instant createdAt
) {
    public LedgerEntry {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
