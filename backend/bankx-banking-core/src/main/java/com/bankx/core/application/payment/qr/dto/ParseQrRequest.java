package com.bankx.core.application.payment.qr.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu giải mã chuỗi VietQR.
 * 
 * @param qrData Chuỗi dữ liệu QR thu thập từ camera/upload
 */
public record ParseQrRequest(
        @NotBlank(message = "Dữ liệu QR không được để trống")
        String qrData
) {
}
