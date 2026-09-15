package com.bankx.core.domain.payment.provider;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy triển khai dịch vụ Thanh toán Điện lực (EVN Hà Nội - Payment Provider Strategy).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component("EVN_HN")}: Đăng ký Strategy làm Spring Bean tự động đưa vào Provider Map.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component("EVN_HN")
public class EvnPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderCode() {
        return "EVN_HN";
    }

    @Override
    public String getCategory() {
        return "ELECTRICITY";
    }

    @Override
    public BillInquiryResponse inquiryBill(String customerBillCode) {
        if (customerBillCode == null || customerBillCode.isBlank()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Mã khách hàng EVN không được để trống!");
        }

        // Mock lookup simulation: Hóa đơn điện ngẫu nhiên theo mã khách hàng
        int codeHash = Math.abs(customerBillCode.hashCode());
        BigDecimal mockAmount = new BigDecimal((350 + (codeHash % 1200)) * 1000);

        return new BillInquiryResponse(
                customerBillCode,
                "NGUYEN VAN A (Hộ Điện Lực HN)",
                getProviderCode(),
                "Điện lực Hà Nội (EVN Hà Nội)",
                mockAmount,
                BigDecimal.ZERO,
                "09/2026",
                "Hóa đơn tiền điện kỳ 09/2026 cho mã KH " + customerBillCode
        );
    }

    @Override
    public PaymentExecutionResult executePayment(String customerBillCode, BigDecimal amount) {
        String txRef = "EVN-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentExecutionResult(true, txRef, "Gạch nợ điện lực EVN thành công!");
    }
}
