package com.bankx.core.presentation.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phản hồi DTO cho thông tin chi tiết Giao dịch tài chính và danh sách Bút toán Ghi sổ kép.
 *
 * @param id ID giao dịch
 * @param transactionReference Mã tham chiếu giao dịch (duy nhất)
 * @param transactionType Loại giao dịch (INTERNAL_TRANSFER, DEPOSIT, WITHDRAWAL, BILL_PAYMENT)
 * @param status Trạng thái (SUCCESS, PENDING, FAILED)
 * @param amount Số tiền giao dịch
 * @param currency Loại tiền tệ ("VND")
 * @param description Diễn giải giao dịch
 * @param createdAt Thời điểm tạo giao dịch
 * @param entries Danh sách các bút toán ghi sổ đính kèm {@link LedgerEntryResponse}
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record TransactionResponse(
        UUID id,
        String transactionReference,
        String transactionType,
        String status,
        BigDecimal amount,
        String currency,
        String description,
        Instant createdAt,
        List<LedgerEntryResponse> entries
) {}
