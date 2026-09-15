package com.bankx.core.presentation.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO cập nhật hồ sơ cá nhân của khách hàng.
 *
 * @param fullName Họ và tên mới
 * @param address Địa chỉ thường trú mới
 * @param dateOfBirth Ngày sinh
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Địa chỉ không được để trống")
        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate dateOfBirth
) {}
