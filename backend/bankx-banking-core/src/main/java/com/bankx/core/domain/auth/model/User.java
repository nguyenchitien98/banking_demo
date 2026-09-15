package com.bankx.core.domain.auth.model;

import com.bankx.common.exception.ErrorCode;
import com.bankx.common.exception.BankingException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Thống thể Domain Người dùng (User Aggregate Root).
 *
 * <p>Quản lý toàn bộ nghiệp vụ tài khoản đăng nhập, trạng thái khóa tài khoản,
 * đếm số lần đăng nhập sai và quản lý vai trò phân quyền.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class User {

    private final UUID id;
    private final String username;
    private String passwordHash;
    private String email;
    private String phone;
    private String status;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Set<UserRole> roles;
    private Instant createdAt;
    private Instant updatedAt;

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_DURATION_MINUTES = 30;

    /**
     * Khởi tạo đối tượng User mới.
     *
     * @param id ID người dùng
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã băm (BCrypt)
     * @param email Địa chỉ email
     * @param phone Số điện thoại
     * @param status Trạng thái (ACTIVE, LOCKED, INACTIVE)
     * @param failedLoginAttempts Số lần đăng nhập sai liên tiếp
     * @param lockedUntil Thời điểm khóa tài khoản kết thúc
     * @param roles Tập hợp các vai trò người dùng
     */
    public User(UUID id, String username, String passwordHash, String email, String phone,
                String status, int failedLoginAttempts, Instant lockedUntil, Set<UserRole> roles) {
        this.id = id != null ? id : UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.phone = phone;
        this.status = status != null ? status : "ACTIVE";
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    /**
     * Kiểm tra xem tài khoản hiện tại có đang bị khóa hay không.
     *
     * @return {@code true} nếu tài khoản đang trong trạng thái bị khóa
     */
    public boolean isAccountLocked() {
        if ("LOCKED".equalsIgnoreCase(status)) {
            if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
                // Đã hết thời hạn khóa -> tự động mở lại
                this.status = "ACTIVE";
                this.failedLoginAttempts = 0;
                this.lockedUntil = null;
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Ghi nhận 1 lần đăng nhập thất bại. Nếu quá 5 lần sẽ tự động khóa tài khoản 30 phút.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockAccount(LOCK_DURATION_MINUTES);
        }
    }

    /**
     * Khóa tài khoản trong số phút quy định.
     *
     * @param minutes Số phút khóa
     */
    public void lockAccount(long minutes) {
        this.status = "LOCKED";
        this.lockedUntil = Instant.now().plusSeconds(minutes * 60);
    }

    /**
     * Reset số lần đăng nhập sai về 0 khi đăng nhập thành công.
     */
    public void resetFailedLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        if ("LOCKED".equalsIgnoreCase(this.status)) {
            this.status = "ACTIVE";
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Set<UserRole> getRoles() { return Collections.unmodifiableSet(roles); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
