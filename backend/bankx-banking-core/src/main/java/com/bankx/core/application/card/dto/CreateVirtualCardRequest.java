package com.bankx.core.application.card.dto;

import com.bankx.core.domain.card.CardBrand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO yêu cầu phát hành Thẻ Ảo mới (Virtual Card Issue Request).
 * 
 * @param customerId ID khách hàng sở hữu
 * @param accountNumber Số tài khoản liên kết trích nợ
 * @param cardBrand Thương hiệu thẻ (VISA, MASTERCARD, NAPAS)
 * @param spendingLimit Hạn mức giao dịch online
 */
public record CreateVirtualCardRequest(
        @NotBlank(message = "ID khách hàng không được để trống")
        String customerId,

        @NotBlank(message = "Số tài khoản liên kết không được để trống")
        String accountNumber,

        @NotNull(message = "Thương hiệu thẻ không được để trống")
        CardBrand cardBrand,

        @NotNull(message = "Hạn mức thanh toán không được để trống")
        @DecimalMin(value = "1000000.00", message = "Hạn mức tối thiểu từ 1,000,000 VND")
        BigDecimal spendingLimit
) {
}
