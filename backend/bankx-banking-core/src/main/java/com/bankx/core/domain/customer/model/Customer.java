package com.bankx.core.domain.customer.model;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * Thống thể Domain Khách Hàng (Customer Aggregate Root).
 *
 * <p>Quản lý toàn bộ thông tin định danh cá nhân, mã CIF ngân hàng,
 * trạng thái xác thực KYC và cập nhật hồ sơ cá nhân.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class Customer {

    private final UUID id;
    private final UUID userId;
    private final String cifNumber;
    private String fullName;
    private final String identityNumber;
    private LocalDate dateOfBirth;
    private String address;
    private String status; // VERIFIED, PENDING_KYC, SUSPENDED
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Khởi tạo đối tượng Customer Domain.
     *
     * @param id ID khách hàng
     * @param userId ID tài khoản User sở hữu
     * @param cifNumber Mã CIF duy nhất ngân hàng cấp
     * @param fullName Họ và tên đầy đủ
     * @param identityNumber Số CCCD / CMND / Hộ chiếu
     * @param dateOfBirth Ngày tháng năm sinh
     * @param address Địa chỉ thường trú / liên lạc
     * @param status Trạng thái xác thực KYC
     */
    public Customer(UUID id, UUID userId, String cifNumber, String fullName,
                    String identityNumber, LocalDate dateOfBirth, String address, String status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId;
        this.cifNumber = cifNumber;
        this.fullName = fullName;
        this.identityNumber = identityNumber;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.status = status != null ? status : "VERIFIED";
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân.
     *
     * @param fullName Họ và tên mới
     * @param address Địa chỉ liên lạc mới
     * @param dateOfBirth Ngày sinh mới
     */
    public void updateProfile(String fullName, String address, LocalDate dateOfBirth) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName.trim();
        }
        if (address != null && !address.isBlank()) {
            this.address = address.trim();
        }
        if (dateOfBirth != null) {
            this.dateOfBirth = dateOfBirth;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Xác nhận hoàn tất xác thực KYC khách hàng.
     */
    public void verifyKyc() {
        this.status = "VERIFIED";
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCifNumber() { return cifNumber; }
    public String getFullName() { return fullName; }
    public String getIdentityNumber() { return identityNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
