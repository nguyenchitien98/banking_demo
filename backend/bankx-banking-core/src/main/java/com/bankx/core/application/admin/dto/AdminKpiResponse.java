package com.bankx.core.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) chứa các chỉ số KPI thống kê tổng quan hệ thống dành cho Admin Dashboard.
 * 
 * <p>Không sử dụng annotation tầng dữ liệu vì đây là đối tượng thuần túy chuyên chở dữ liệu (Data Transfer Object)
 * trả về cho REST API Client.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminKpiResponse {
    /** Tổng số lượng khách hàng đã đăng ký */
    private long totalCustomers;

    /** Tổng số lượng tài khoản thanh toán */
    private long totalAccounts;

    /** Tổng số dư lưu thông trên toàn hệ thống (VND) */
    private BigDecimal totalSystemBalance;

    /** Số lượng hồ sơ eKYC đang chờ duyệt */
    private long pendingKycCount;

    /** Số lượng giao dịch diễn ra trong ngày hôm nay */
    private long todayTransactionCount;

    /** Số lượng tài khoản bị đóng băng/khóa */
    private long frozenAccountCount;

    /** Số lượng giao dịch nghi vấn gian lận cần xem xét */
    private long highRiskAlertCount;
}
