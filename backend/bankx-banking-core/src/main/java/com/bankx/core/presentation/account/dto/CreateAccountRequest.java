package com.bankx.core.presentation.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO yêu cầu mở tài khoản thanh toán mới.
 *
 * @param accountName Tên gọi tài khoản (ví dụ: "Tài khoản thanh toán chính")
 * @param currency Loại tiền tệ ("VND")
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record CreateAccountRequest(
        @NotBlank(message = "Tên tài khoản không được để trống")
        @Size(max = 100, message = "Tên tài khoản tối đa 100 ký tự")
        String accountName,

        String currency
) {}
