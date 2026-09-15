package com.bankx.core.domain.payment.provider;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy triển khai dịch vụ Thanh toán Nước sinh hoạt (DNP Water HCM - Payment Provider Strategy).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component("WATER_HCM")}: Đăng ký Strategy làm Spring Bean.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component("WATER_HCM")
public class WaterPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderCode() {
        return "WATER_HCM";
    }

    @Override
    public String getCategory() {
        return "WATER";
    }

    @Override
    public BillInquiryResponse inquiryBill(String customerBillCode) {
        if (customerBillCode == null || customerBillCode.isBlank()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Mã danh bạ cấp nước không được để trống!");
        }

        int codeHash = Math.abs(customerBillCode.hashCode());
        BigDecimal mockAmount = new BigDecimal((120 + (codeHash % 450)) * 1000);

        return new BillInquiryResponse(
                customerBillCode,
                "TRAN THI B (Hộ Nước Sinh Hoạt DNP)",
                getProviderCode(),
                "Nước sinh hoạt DNP Hồ Chí Minh",
                mockAmount,
                BigDecimal.ZERO,
                "09/2026",
                "Tiền nước sinh hoạt kỳ 09/2026 cho mã danh bạ " + customerBillCode
        );
    }

    @Override
    public PaymentExecutionResult executePayment(String customerBillCode, BigDecimal amount) {
        String txRef = "WTR-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentExecutionResult(true, txRef, "Gạch nợ tiền nước DNP thành công!");
    }
}
