package com.bankx.core.domain.transfer.model;

import com.bankx.core.domain.account.model.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root quản lý Lệnh chuyển tiền (Bank Transfer Aggregate Root).
 *
 * <p>Quản lý vòng đời và trạng thái của một yêu cầu chuyển tiền từ tài khoản nguồn tới tài khoản đích.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class BankTransfer {

    private final UUID id;
    private final String transferCode;
    private final UUID sourceAccountId;
    private final UUID targetAccountId;
    private final String targetAccountNumber;
    private final String targetAccountName;
    private final Money amount;
    private final Money fee;
    private final String description;
    private final TransferType transferType;
    private TransferStatus status;
    private UUID transactionId;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public BankTransfer(UUID id, String transferCode, UUID sourceAccountId, UUID targetAccountId,
                        String targetAccountNumber, String targetAccountName, Money amount, Money fee,
                        String description, TransferType transferType, TransferStatus status,
                        UUID transactionId, String failureReason, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.transferCode = transferCode;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.targetAccountNumber = targetAccountNumber;
        this.targetAccountName = targetAccountName;
        this.amount = amount;
        this.fee = fee != null ? fee : Money.ZERO;
        this.description = description;
        this.transferType = transferType != null ? transferType : TransferType.INTERNAL;
        this.status = status != null ? status : TransferStatus.PENDING;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void markCompleted(UUID transactionId) {
        this.status = TransferStatus.COMPLETED;
        this.transactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransferCode() {
        return transferCode;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public String getTargetAccountName() {
        return targetAccountName;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getFee() {
        return fee;
    }

    public String getDescription() {
        return description;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
