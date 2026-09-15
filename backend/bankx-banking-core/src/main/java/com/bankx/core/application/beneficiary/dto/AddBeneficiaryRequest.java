package com.bankx.core.application.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu lưu thêm người thụ hưởng vào danh bạ.
 * 
 * @param customerId ID khách hàng sở hữu
 * @param accountNumber Số tài khoản nhận tiền
 * @param bankBin Mã BIN ngân hàng (ví dụ: 970400 cho BankX, 970423 cho TPBank)
 * @param bankName Tên thương hiệu ngân hàng
 * @param accountHolderName Tên chủ tài khoản nhận
 * @param nickname Biệt danh gợi nhớ (tùy chọn)
 */
public record AddBeneficiaryRequest(
        @NotBlank(message = "ID khách hàng không được để trống")
        String customerId,

        @NotBlank(message = "Số tài khoản thụ hưởng không được để trống")
        String accountNumber,

        String bankBin,

        String bankName,

        @NotBlank(message = "Tên chủ tài khoản không được để trống")
        String accountHolderName,

        String nickname
) {
}
