package com.bankx.core.domain.payment.model;

import java.math.BigDecimal;

/**
 * Phản hồi DTO thông tin tra cứu hóa đơn từ nhà cung cấp dịch vụ (Bill Inquiry Response).
 *
 * @param customerBillCode Mã khách hàng / Mã danh bạ hóa đơn
 * @param customerName Tên chủ hộ / Khách hàng thanh toán
 * @param providerCode Mã nhà cung cấp dịch vụ
 * @param providerName Tên nhà cung cấp dịch vụ
 * @param amount Số tiền nợ cần thanh toán
 * @param fee Phí thanh toán dịch vụ (mặc định 0 VND)
 * @param period Kỳ thanh toán (ví dụ: "09/2026")
 * @param description Chi tiết hóa đơn
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record BillInquiryResponse(
        String customerBillCode,
        String customerName,
        String providerCode,
        String providerName,
        BigDecimal amount,
        BigDecimal fee,
        String period,
        String description
) {}
