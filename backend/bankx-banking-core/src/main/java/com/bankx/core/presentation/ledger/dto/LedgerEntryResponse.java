package com.bankx.core.presentation.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Phản hồi DTO chi tiết bút toán ghi sổ đơn lẻ.
 *
 * @param id ID bút toán
 * @param transactionId ID giao dịch
 * @param accountId ID tài khoản
 * @param entryType Loại bút toán (DEBIT / CREDIT)
 * @param amount Số tiền hạch toán
 * @param currency Loại tiền tệ ("VND")
 * @param balanceAfter Số dư tài khoản ngay sau khi ghi bút toán
 * @param createdAt Thời điểm ghi nhận
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        UUID accountId,
        String entryType,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        Instant createdAt
) {}
