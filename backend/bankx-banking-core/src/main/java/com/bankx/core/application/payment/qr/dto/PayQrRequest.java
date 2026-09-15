package com.bankx.core.application.payment.qr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO yêu cầu thực thi thanh toán giao dịch qua mã QR.
 * 
 * @param sourceAccountId ID tài khoản trích tiền
 * @param targetAccountNumber Số tài khoản nhận tiền
 * @param targetBankBin Mã BIN ngân hàng thụ hưởng
 * @param targetAccountName Tên người nhận tiền
 * @param amount Số tiền chuyển
 * @param description Nội dung chuyển tiền
 * @param qrPayload Chuỗi QR đã quét
 */
public record PayQrRequest(
        @NotBlank(message = "ID tài khoản nguồn không được để trống")
        String sourceAccountId,

        @NotBlank(message = "Số tài khoản nhận tiền không được để trống")
        String targetAccountNumber,

        @NotBlank(message = "Mã BIN ngân hàng không được để trống")
        String targetBankBin,

        String targetAccountName,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1000.00", message = "Số tiền tối thiểu là 1,000 VND")
        BigDecimal amount,

        String description,

        @NotBlank(message = "Payload QR không được để trống")
        String qrPayload
) {
}
