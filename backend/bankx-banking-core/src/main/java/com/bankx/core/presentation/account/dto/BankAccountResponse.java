package com.bankx.core.presentation.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Phản hồi DTO chứa thông tin tài khoản thanh toán ngân hàng.
 *
 * @param id ID tài khoản
 * @param customerId ID khách hàng sở hữu
 * @param accountNumber Số tài khoản (đã che giấu hoặc đầy đủ tùy context)
 * @param accountName Tên tài khoản
 * @param balance Số dư khả dụng
 * @param currency Loại tiền tệ ("VND")
 * @param status Trạng thái (ACTIVE, FROZEN)
 * @param version Version khóa lạc quan (Optimistic Lock)
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record BankAccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currency,
        String status,
        long version
) {}
