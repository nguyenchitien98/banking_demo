package com.bankx.core.domain.account.model;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;

import java.time.Instant;
import java.util.UUID;

/**
 * Thống thể Domain Tài Khoản Thanh Toán (Bank Account Aggregate Root).
 *
 * <p>Quản lý toàn bộ nghiệp vụ số dư tài khoản, nạp/rút/chuyển tiền,
 * trạng thái phong tỏa (FREEZE) và cơ chế khóa lạc quan (Optimistic Lock {@code version}).</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class BankAccount {

    private final UUID id;
    private final UUID customerId;
    private final String accountNumber;
    private final String accountName;
    private Money balance;
    private final String currency;
    private String status; // ACTIVE, FROZEN, CLOSED
    private long version;  // Optimistic Locking version
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Khởi tạo đối tượng BankAccount Domain.
     *
     * @param id ID tài khoản
     * @param customerId ID khách hàng sở hữu
     * @param accountNumber Số tài khoản ngân hàng (10-12 số)
     * @param accountName Tên tài khoản
     * @param balance Số dư tài khoản dạng {@link Money}
     * @param currency Loại tiền tệ
     * @param status Trạng thái (ACTIVE, FROZEN)
     * @param version Phiên bản lạc quan (Optimistic Lock version)
     */
    public BankAccount(UUID id, UUID customerId, String accountNumber, String accountName,
                       Money balance, String currency, String status, long version) {
        this.id = id != null ? id : UUID.randomUUID();
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.currency = currency != null ? currency : Money.DEFAULT_CURRENCY;
        this.balance = balance != null ? balance : Money.ofVnd(0);
        this.status = status != null ? status : "ACTIVE";
        this.version = version;
    }

    /**
     * Thực hiện nạp tiền (Ghi Có / Credit) vào tài khoản.
     *
     * @param amount Số tiền nạp
     */
    public void deposit(Money amount) {
        checkCanTransact();
        if (amount == null || amount.isNegative()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Số tiền nạp phải lớn hơn 0");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Thực hiện rút tiền (Ghi Nợ / Debit) khỏi tài khoản.
     *
     * @param amount Số tiền rút
     */
    public void withdraw(Money amount) {
        checkCanTransact();
        if (amount == null || amount.isNegative()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Số tiền rút phải lớn hơn 0");
        }
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new BankingException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Phong tỏa tài khoản (FREEZE).
     */
    public void freeze() {
        this.status = "FROZEN";
        this.updatedAt = Instant.now();
    }

    /**
     * Mở phong tỏa tài khoản (UNFREEZE).
     */
    public void unfreeze() {
        this.status = "ACTIVE";
        this.updatedAt = Instant.now();
    }

    /**
     * Kiểm tra tài khoản có đủ điều kiện thực hiện giao dịch hay không.
     */
    public void checkCanTransact() {
        if ("FROZEN".equalsIgnoreCase(this.status)) {
            throw new BankingException(ErrorCode.ACCOUNT_FROZEN);
        }
        if ("CLOSED".equalsIgnoreCase(this.status)) {
            throw new BankingException(ErrorCode.ACCOUNT_CLOSED);
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }
    public Money getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
