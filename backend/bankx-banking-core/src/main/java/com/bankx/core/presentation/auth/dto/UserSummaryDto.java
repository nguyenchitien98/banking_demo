package com.bankx.core.presentation.auth.dto;

import java.util.Set;
import java.util.UUID;

/**
 * DTO tóm tắt thông tin tài khoản người dùng sau khi xác thực thành công.
 *
 * @param id ID người dùng
 * @param username Tên đăng nhập
 * @param email Email đã che giấu hoặc đầy đủ
 * @param roles Các vai trò hệ thống
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record UserSummaryDto(
        UUID id,
        String username,
        String email,
        Set<String> roles
) {}
