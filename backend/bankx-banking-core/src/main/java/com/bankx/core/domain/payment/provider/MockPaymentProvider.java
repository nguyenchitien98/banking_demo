package com.bankx.core.domain.payment.provider;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy triển khai Nhà cung cấp Thử nghiệm BankX (Mock Payment Provider Strategy).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component("MOCK_PROVIDER")}: Đăng ký Strategy làm Spring Bean.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component("MOCK_PROVIDER")
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderCode() {
        return "MOCK_PROVIDER";
    }

    @Override
    public String getCategory() {
        return "OTHER";
    }

    @Override
    public BillInquiryResponse inquiryBill(String customerBillCode) {
        if (customerBillCode == null || customerBillCode.isBlank()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Mã khách hàng thử nghiệm không được để trống!");
        }

        return new BillInquiryResponse(
                customerBillCode,
                "KHÁCH HÀNG DEMO BANKX",
                getProviderCode(),
                "Nhà cung cấp Thử nghiệm BankX",
                new BigDecimal("250000"),
                BigDecimal.ZERO,
                "09/2026",
                "Hóa đơn dịch vụ thử nghiệm kỳ 09/2026 cho mã " + customerBillCode
        );
    }

    @Override
    public PaymentExecutionResult executePayment(String customerBillCode, BigDecimal amount) {
        String txRef = "MCK-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentExecutionResult(true, txRef, "Thanh toán hóa đơn thử nghiệm thành công!");
    }
}
