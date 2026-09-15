package com.bankx.core.presentation.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Phản hồi DTO chi tiết kết quả lệnh chuyển tiền.
 *
 * @param id ID lệnh chuyển tiền
 * @param transferCode Mã chuyển tiền duy nhất
 * @param sourceAccountId ID tài khoản nguồn
 * @param targetAccountId ID tài khoản đích
 * @param targetAccountNumber Số tài khoản người nhận
 * @param targetAccountName Tên người nhận
 * @param amount Số tiền chuyển
 * @param currency Loại tiền tệ ("VND")
 * @param fee Phí chuyển tiền
 * @param description Diễn giải nội dung
 * @param transferType Loại chuyển tiền (INTERNAL / NAPAS247)
 * @param status Trạng thái (COMPLETED / FAILED)
 * @param transactionId ID giao dịch hạch toán sổ kép
 * @param createdAt Thời điểm tạo lệnh
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record TransferResponse(
        UUID id,
        String transferCode,
        UUID sourceAccountId,
        UUID targetAccountId,
        String targetAccountNumber,
        String targetAccountName,
        BigDecimal amount,
        String currency,
        BigDecimal fee,
        String description,
        String transferType,
        String status,
        UUID transactionId,
        Instant createdAt
) {}
