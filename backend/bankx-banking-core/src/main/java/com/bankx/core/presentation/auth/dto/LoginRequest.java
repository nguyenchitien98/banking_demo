package com.bankx.core.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) chứa thông tin yêu cầu đăng nhập từ phía client.
 *
 * @param username Tên đăng nhập người dùng
 * @param password Mật khẩu đăng nhập
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
        String password
) {}
