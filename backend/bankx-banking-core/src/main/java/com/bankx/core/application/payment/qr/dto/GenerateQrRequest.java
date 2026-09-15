package com.bankx.core.application.payment.qr.dto;

import java.math.BigDecimal;

/**
 * DTO yêu cầu sinh mã VietQR nhận tiền cho tài khoản.
 * 
 * @param accountNumber Số tài khoản nhận tiền
 * @param bankBin Mã BIN ngân hàng (nếu để trống tự mặc định 970400)
 * @param amount Số tiền chỉ định (tùy chọn)
 * @param description Nội dung nhận tiền (tùy chọn)
 */
public record GenerateQrRequest(
        String accountNumber,
        String bankBin,
        BigDecimal amount,
        String description
) {
}
