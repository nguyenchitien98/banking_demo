package com.bankx.core.domain.payment.model;

/**
 * Phản hồi kết quả thực thi thanh toán hóa đơn từ Provider (Payment Execution Result).
 *
 * @param success Trạng thái thành công
 * @param transactionReference Mã tham chiếu giao dịch phía nhà cung cấp
 * @param message Khung thông báo phản hồi
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record PaymentExecutionResult(
        boolean success,
        String transactionReference,
        String message
) {}
