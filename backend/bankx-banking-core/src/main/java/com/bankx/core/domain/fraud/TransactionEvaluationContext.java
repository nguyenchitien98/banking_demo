package com.bankx.core.domain.fraud;

import java.math.BigDecimal;

/**
 * Ngữ cảnh đánh giá rủi ro cho một giao dịch chuyển tiền/thanh toán.
 * 
 * @param sourceAccountId ID tài khoản nguồn
 * @param targetAccountNumber Số tài khoản thụ hưởng
 * @param amount Số tiền giao dịch
 * @param isNewDevice True nếu giao dịch từ thiết bị chưa từng xác thực
 * @param isNewBeneficiary True nếu chuyển tiền cho tài khoản thụ hưởng mới lần đầu
 * @param velocityLastMinute Số lượng giao dịch thực hiện từ tài khoản nguồn trong 60 giây qua
 * @param transactionHour Giờ trong ngày (0-23) phát sinh giao dịch
 */
public record TransactionEvaluationContext(
        String sourceAccountId,
        String targetAccountNumber,
        BigDecimal amount,
        boolean isNewDevice,
        boolean isNewBeneficiary,
        int velocityLastMinute,
        int transactionHour
) {
}
