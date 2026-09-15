package com.bankx.core.application.beneficiary.dto;

/**
 * DTO phản hồi tra cứu tên chủ tài khoản nhận tiền.
 * 
 * @param accountNumber Số tài khoản
 * @param accountHolderName Tên chủ tài khoản in hoa
 * @param bankBin Mã BIN ngân hàng
 * @param bankName Tên ngân hàng
 */
public record AccountLookupResponse(
        String accountNumber,
        String accountHolderName,
        String bankBin,
        String bankName
) {
}
