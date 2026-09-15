package com.bankx.core.presentation.transfer.dto;

import java.util.UUID;

/**
 * Phản hồi DTO truy vấn thông tin người thụ hưởng.
 *
 * @param accountId ID tài khoản thụ hưởng
 * @param accountNumber Số tài khoản
 * @param accountName Tên tài khoản người nhận
 * @param status Trạng thái tài khoản
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record RecipientInquiryResponse(
        UUID accountId,
        String accountNumber,
        String accountName,
        String status
) {}
