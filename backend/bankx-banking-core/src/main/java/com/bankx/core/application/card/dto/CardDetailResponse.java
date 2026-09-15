package com.bankx.core.application.card.dto;

import com.bankx.core.domain.card.CardBrand;
import com.bankx.core.domain.card.CardStatus;
import com.bankx.core.domain.card.CardType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi thông tin chi tiết Thẻ Ngân hàng.
 * 
 * @param id ID duy nhất của Thẻ
 * @param customerId ID khách hàng sở hữu
 * @param accountNumber Số tài khoản liên kết
 * @param cardHolderName Tên in trên thẻ
 * @param maskedPan Số thẻ đã che mờ (4000 12** **** 8899)
 * @param panToken Token mã hóa thẻ (dùng cho thanh toán online)
 * @param mockCvv Mã CVV ảo (chỉ trả về khi mới phát hành)
 * @param cardType Loại thẻ (VIRTUAL_DEBIT...)
 * @param cardBrand Thương hiệu thẻ (VISA, MASTERCARD...)
 * @param expiryMonth Tháng hết hạn (MM)
 * @param expiryYear Năm hết hạn (YY)
 * @param spendingLimit Hạn mức 1 lần
 * @param dailyLimit Hạn mức theo ngày
 * @param status Trạng thái FSM của thẻ (ACTIVE, FROZEN, BLOCKED, EXPIRED)
 * @param createdAt Thời gian phát hành
 */
public record CardDetailResponse(
        String id,
        String customerId,
        String accountNumber,
        String cardHolderName,
        String maskedPan,
        String panToken,
        String mockCvv,
        CardType cardType,
        CardBrand cardBrand,
        String expiryMonth,
        String expiryYear,
        BigDecimal spendingLimit,
        BigDecimal dailyLimit,
        CardStatus status,
        Instant createdAt
) {
}
