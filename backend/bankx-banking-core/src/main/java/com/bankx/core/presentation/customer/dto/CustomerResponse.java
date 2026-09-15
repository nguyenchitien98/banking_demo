package com.bankx.core.presentation.customer.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Phản hồi DTO chứa thông tin hồ sơ cá nhân và trạng thái KYC của khách hàng.
 *
 * @param id ID khách hàng
 * @param cifNumber Mã CIF ngân hàng
 * @param fullName Họ và tên đầy đủ
 * @param identityNumber Số CCCD / CMND (đã che giấu)
 * @param phone Số điện thoại (đã che giấu)
 * @param email Email (đã che giấu)
 * @param dateOfBirth Ngày sinh
 * @param address Địa chỉ thường trú
 * @param status Trạng thái xác thực KYC
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record CustomerResponse(
        UUID id,
        String cifNumber,
        String fullName,
        String identityNumber,
        String phone,
        String email,
        LocalDate dateOfBirth,
        String address,
        String status
) {}
