package com.bankx.core.application.payment.qr.dto;

import java.math.BigDecimal;

/**
 * DTO phản hồi dữ liệu mã VietQR sinh ra.
 * 
 * @param qrPayload Chuỗi mã VietQR EMVCo nguyên bản
 * @param bankBin Mã BIN ngân hàng
 * @param bankName Tên ngân hàng
 * @param accountNumber Số tài khoản nhận tiền
 * @param accountHolderName Tên chủ tài khoản nhận tiền
 * @param amount Số tiền (nếu có)
 * @param description Nội dung nhận tiền
 */
public record GenerateQrResponse(
        String qrPayload,
        String bankBin,
        String bankName,
        String accountNumber,
        String accountHolderName,
        BigDecimal amount,
        String description
) {
}
