package com.bankx.core.domain.payment.provider;

import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;

import java.math.BigDecimal;

/**
 * Interface đại diện cho Chiến lược Thanh toán Hóa đơn (Strategy Pattern - Payment Provider Strategy).
 *
 * <p>Mọi Nhà cung cấp dịch vụ thanh toán hóa đơn (EVN Điện lực, Nước sinh hoạt, Viettel Telecom, v.v.)
 * đều phải triển khai Interface này để tuân thủ nguyên lý Open-Closed Principle (OCP) của SOLID.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface PaymentProvider {

    /**
     * Lấy Mã Định danh Nhà cung cấp duy nhất (ví dụ: "EVN_HN", "WATER_HCM").
     *
     * @return Chuỗi Mã Provider
     */
    String getProviderCode();

    /**
     * Lấy Danh mục Dịch vụ (ví dụ: "ELECTRICITY", "WATER", "TELECOM").
     *
     * @return Chuỗi Category
     */
    String getCategory();

    /**
     * Truy vấn thông tin hóa đơn và nợ cước của khách hàng từ hệ thống Nhà cung cấp.
     *
     * @param customerBillCode Mã khách hàng / Mã danh bạ hóa đơn
     * @return {@link BillInquiryResponse} thông tin nợ cước
     */
    BillInquiryResponse inquiryBill(String customerBillCode);

    /**
     * Gửi yêu cầu gạch nợ và thanh toán hóa đơn tới hệ thống Nhà cung cấp.
     *
     * @param customerBillCode Mã khách hàng
     * @param amount Số tiền thanh toán
     * @return {@link PaymentExecutionResult} kết quả gạch nợ
     */
    PaymentExecutionResult executePayment(String customerBillCode, BigDecimal amount);
}
