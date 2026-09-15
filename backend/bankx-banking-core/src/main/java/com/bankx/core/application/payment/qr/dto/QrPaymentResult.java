package com.bankx.core.application.payment.qr.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi kết quả thực thi thanh toán QR.
 * 
 * @param paymentId Mã giao dịch QR
 * @param sourceAccountId ID tài khoản trích tiền
 * @param targetAccountNumber Số tài khoản nhận tiền
 * @param targetBankBin Mã BIN ngân hàng nhận
 * @param targetAccountName Tên chủ tài khoản nhận
 * @param amount Số tiền đã chuyển
 * @param description Nội dung chuyển tiền
 * @param status Trạng thái (COMPLETED)
 * @param timestamp Thời gian giao dịch
 */
public record QrPaymentResult(
        String paymentId,
        String sourceAccountId,
        String targetAccountNumber,
        String targetBankBin,
        String targetAccountName,
        BigDecimal amount,
        String description,
        String status,
        Instant timestamp
) {
}
