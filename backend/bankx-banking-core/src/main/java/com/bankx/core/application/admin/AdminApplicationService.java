package com.bankx.core.application.admin;

import com.bankx.core.application.admin.dto.AdminAuditLogResponse;
import com.bankx.core.application.admin.dto.AdminCustomerResponse;
import com.bankx.core.application.admin.dto.AdminKpiResponse;
import com.bankx.core.application.admin.dto.ReviewKycRequest;
import com.bankx.core.domain.admin.AdminAuditLogJpaEntity;
import com.bankx.core.domain.admin.SpringDataAdminAuditLogRepository;
import com.bankx.core.domain.fraud.SpringDataFraudAlertRepository;
import com.bankx.core.infrastructure.persistence.account.BankAccountJpaEntity;
import com.bankx.core.infrastructure.persistence.account.SpringDataBankAccountRepository;
import com.bankx.core.infrastructure.persistence.customer.CustomerJpaEntity;
import com.bankx.core.infrastructure.persistence.customer.SpringDataCustomerRepository;
import com.bankx.core.infrastructure.persistence.ledger.repository.SpringDataTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Service nghiệp vụ xử lý các thao tác Quản trị hệ thống dành cho Admin Portal.
 * 
 * <p>Annotation {@link Service} đăng ký class này thành một Spring Service Bean đảm nhiệm xử lý logic nghiệp vụ
 * tầng Application cho Quản trị viên (Admin, Teller, Auditor).
 * Annotation {@link Transactional} đảm bảo tính Toàn vẹn dữ liệu (ACID) khi thực hiện các cập nhật như duyệt eKYC,
 * khóa/mở khóa tài khoản và ghi nhật ký kiểm toán.</p>
 */
@Service
public class AdminApplicationService {

    private final SpringDataCustomerRepository customerRepository;
    private final SpringDataBankAccountRepository bankAccountRepository;
    private final SpringDataAdminAuditLogRepository adminAuditLogRepository;
    private final SpringDataTransactionRepository transactionRepository;
    private final SpringDataFraudAlertRepository fraudAlertRepository;

    public AdminApplicationService(SpringDataCustomerRepository customerRepository,
                                   SpringDataBankAccountRepository bankAccountRepository,
                                   SpringDataAdminAuditLogRepository adminAuditLogRepository,
                                   SpringDataTransactionRepository transactionRepository,
                                   SpringDataFraudAlertRepository fraudAlertRepository) {
        this.customerRepository = customerRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    /**
     * Lấy danh sách các chỉ số thống kê tổng quan (KPIs) của hệ thống.
     *
     * @return {@link AdminKpiResponse} chứa tổng số khách hàng, tài khoản, số dư, eKYC chờ duyệt, giao dịch hôm nay.
     */
    @Transactional(readOnly = true)
    public AdminKpiResponse getDashboardKpis() {
        List<CustomerJpaEntity> customers = customerRepository.findAll();
        List<BankAccountJpaEntity> accounts = bankAccountRepository.findAll();

        long totalCustomers = customers.size();
        long pendingKyc = customers.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) || "PENDING_VERIFICATION".equalsIgnoreCase(c.getStatus()))
                .count();

        long totalAccounts = accounts.size();
        long frozenAccounts = accounts.stream()
                .filter(a -> "FROZEN".equalsIgnoreCase(a.getStatus()) || "BLOCKED".equalsIgnoreCase(a.getStatus()))
                .count();

        BigDecimal totalBalance = accounts.stream()
                .map(BankAccountJpaEntity::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long todayTxCount = transactionRepository.count();
        long highRiskCount = fraudAlertRepository.count();

        return AdminKpiResponse.builder()
                .totalCustomers(totalCustomers)
                .totalAccounts(totalAccounts)
                .totalSystemBalance(totalBalance)
                .pendingKycCount(pendingKyc)
                .todayTransactionCount(todayTxCount)
                .frozenAccountCount(frozenAccounts)
                .highRiskAlertCount(highRiskCount)
                .build();
    }

    /**
     * Lấy danh sách tất cả các khách hàng trong hệ thống.
     *
     * @return Danh sách {@link AdminCustomerResponse}
     */
    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> listCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> AdminCustomerResponse.builder()
                        .id(c.getId().toString())
                        .username("user_" + c.getCifNumber())
                        .fullName(c.getFullName())
                        .email(c.getCifNumber().toLowerCase() + "@bankx.com")
                        .phoneNumber("09" + String.format("%08d", Math.abs(c.getCifNumber().hashCode()) % 100000000))
                        .identityNumber(c.getIdentityNumber())
                        .kycStatus(c.getStatus())
                        .accountStatus("ACTIVE")
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Lấy danh sách nhật ký kiểm toán thao tác quản trị.
     *
     * @return Danh sách {@link AdminAuditLogResponse}
     */
    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getAuditLogs() {
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(log -> AdminAuditLogResponse.builder()
                        .id(log.getId())
                        .adminUsername(log.getAdminUsername())
                        .adminRole(log.getAdminRole())
                        .actionType(log.getActionType())
                        .targetId(log.getTargetId())
                        .details(log.getDetails())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Phê duyệt hoặc từ chối hồ sơ eKYC của khách hàng.
     *
     * @param customerId ID của khách hàng
     * @param request DTO chứa thông tin trạng thái mới và lý do
     * @param adminUsername Tên đăng nhập của quản trị viên thực hiện
     * @return Customer DTO sau khi được cập nhật
     */
    @Transactional
    public AdminCustomerResponse reviewKyc(String customerId, ReviewKycRequest request, String adminUsername) {
        CustomerJpaEntity customer = customerRepository.findAll().stream()
                .filter(c -> c.getId().toString().equalsIgnoreCase(customerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với ID: " + customerId));

        customer.setStatus(request.getStatus());
        customerRepository.save(customer);

        String actionType = "VERIFIED".equalsIgnoreCase(request.getStatus()) ? "KYC_APPROVE" : "KYC_REJECT";
        logAdminAction(adminUsername, "ROLE_ADMIN", actionType, customerId, request.getReason());

        return AdminCustomerResponse.builder()
                .id(customer.getId().toString())
                .username("user_" + customer.getCifNumber())
                .fullName(customer.getFullName())
                .email(customer.getCifNumber().toLowerCase() + "@bankx.com")
                .phoneNumber("09" + String.format("%08d", Math.abs(customer.getCifNumber().hashCode()) % 100000000))
                .identityNumber(customer.getIdentityNumber())
                .kycStatus(customer.getStatus())
                .accountStatus("ACTIVE")
                .createdAt(customer.getCreatedAt())
                .build();
    }

    /**
     * Khóa/Đóng băng tài khoản thanh toán của khách hàng.
     *
     * @param accountId ID của tài khoản
     * @param reason Lý do khóa tài khoản
     * @param adminUsername Tên quản trị viên thực hiện
     */
    @Transactional
    public void freezeAccount(String accountId, String reason, String adminUsername) {
        BankAccountJpaEntity account = bankAccountRepository.findAll().stream()
                .filter(a -> a.getId().toString().equalsIgnoreCase(accountId) || a.getAccountNumber().equalsIgnoreCase(accountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với ID: " + accountId));

        account.setStatus("FROZEN");
        bankAccountRepository.save(account);

        logAdminAction(adminUsername, "ROLE_ADMIN", "ACCOUNT_FREEZE", account.getAccountNumber(), reason);
    }

    /**
     * Ghi nhật ký kiểm toán hành động quản trị viên vào cơ sở dữ liệu.
     *
     * @param adminUsername Tên đăng nhập quản trị viên
     * @param adminRole Vai trò quản trị (ROLE_ADMIN, ROLE_TELLER, ROLE_AUDITOR)
     * @param actionType Loại hành động (KYC_APPROVE, ACCOUNT_FREEZE, ...)
     * @param targetId ID mục tiêu chịu tác động
     * @param details Chi tiết lý do thao tác
     */
    private void logAdminAction(String adminUsername, String adminRole, String actionType, String targetId, String details) {
        AdminAuditLogJpaEntity auditLog = AdminAuditLogJpaEntity.builder()
                .id("LOG-" + System.currentTimeMillis())
                .adminUsername(adminUsername != null ? adminUsername : "admin_sys")
                .adminRole(adminRole != null ? adminRole : "ROLE_ADMIN")
                .actionType(actionType)
                .targetId(targetId)
                .details(details)
                .createdAt(Instant.now())
                .build();
        adminAuditLogRepository.save(auditLog);
    }
}
