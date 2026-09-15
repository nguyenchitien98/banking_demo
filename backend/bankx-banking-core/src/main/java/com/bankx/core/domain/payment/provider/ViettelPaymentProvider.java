package com.bankx.core.domain.payment.provider;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy triển khai dịch vụ Thanh toán Cước Internet/Điện thoại Viettel (Telecom Payment Provider Strategy).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component("VIETTEL_TEL")}: Đăng ký Strategy làm Spring Bean.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component("VIETTEL_TEL")
public class ViettelPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderCode() {
        return "VIETTEL_TEL";
    }

    @Override
    public String getCategory() {
        return "TELECOM";
    }

    @Override
    public BillInquiryResponse inquiryBill(String customerBillCode) {
        if (customerBillCode == null || customerBillCode.isBlank()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Mã thuê bao / tài khoản Viettel không được để trống!");
        }

        int codeHash = Math.abs(customerBillCode.hashCode());
        BigDecimal mockAmount = new BigDecimal((180 + (codeHash % 600)) * 1000);

        return new BillInquiryResponse(
                customerBillCode,
                "LE VAN C (Thuê bao Viettel Telecom)",
                getProviderCode(),
                "Viettel Telecom (Internet/Cước thoại)",
                mockAmount,
                BigDecimal.ZERO,
                "09/2026",
                "Cước Internet & viễn thông Viettel kỳ 09/2026 cho sđt/mã thuê bao " + customerBillCode
        );
    }

    @Override
    public PaymentExecutionResult executePayment(String customerBillCode, BigDecimal amount) {
        String txRef = "VTT-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentExecutionResult(true, txRef, "Gạch nợ cước Viettel Telecom thành công!");
    }
}
