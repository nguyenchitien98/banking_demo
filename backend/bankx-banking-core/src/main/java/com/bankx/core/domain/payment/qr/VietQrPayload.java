package com.bankx.core.domain.payment.qr;

import java.math.BigDecimal;

/**
 * Record chứa dữ liệu phân tích (parse) hoặc khởi tạo từ chuỗi VietQR chuẩn EMVCo.
 * 
 * <p>Đối tượng này được sử dụng để truyền tải dữ liệu thanh toán qua mã QR giữa các tầng
 * Domain, Application và Presentation.</p>
 * 
 * @param bankBin Mã BIN ngân hàng thụ hưởng (ví dụ: 970423 cho TPBank, 970400 cho BankX)
 * @param bankName Tên ngân hàng thụ hưởng
 * @param accountNumber Số tài khoản nhận tiền
 * @param accountHolderName Tên chủ tài khoản nhận tiền (nếu có)
 * @param amount Số tiền thanh toán (nếu là QR động)
 * @param description Nội dung chuyển tiền / thanh toán
 * @param isDynamic True nếu là QR động (đã bao gồm số tiền), False nếu là QR tĩnh (chưa có số tiền)
 * @param crcValid True nếu mã kiểm tra CRC-16 của chuỗi QR hợp lệ theo chuẩn EMVCo
 * @param rawPayload Chuỗi QR gốc định dạng EMVCo (TLVs)
 */
public record VietQrPayload(
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
