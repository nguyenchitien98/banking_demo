package com.bankx.core.application.payment.qr.dto;

import java.math.BigDecimal;

/**
 * DTO phản hồi thông tin chi tiết giải mã từ chuỗi VietQR.
 * 
 * @param bankBin Mã BIN ngân hàng
 * @param bankName Tên ngân hàng
 * @param accountNumber Số tài khoản thụ hưởng
 * @param accountHolderName Tên chủ tài khoản thụ hưởng
 * @param amount Số tiền (nếu là QR động)
 * @param description Nội dung chuyển tiền
 * @param isDynamic True nếu là QR động
 * @param crcValid True nếu mã CRC16 khớp
 * @param rawPayload Chuỗi mã gốc
 */
public record ParseQrResponse(
        String bankBin,
        String bankName,
        String accountNumber,
        String accountHolderName,
        BigDecimal amount,
        String description,
        boolean isDynamic,
        boolean crcValid,
        String rawPayload
) {
}
