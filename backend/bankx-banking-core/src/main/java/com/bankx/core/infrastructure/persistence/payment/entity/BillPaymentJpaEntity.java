package com.bankx.core.infrastructure.persistence.payment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity JPA lưu trữ chi tiết giao dịch thanh toán hóa đơn (bill_payments).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Entity}: Đánh dấu Class tương ứng với bảng {@code bill_payments}.</li>
 *   <li>{@code @Table(name = "bill_payments")}: Ánh xạ bảng trong CSDL Postgres.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Entity
@Table(name = "bill_payments")
public class BillPaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "payment_code", nullable = false, unique = true, length = 64)
    private String paymentCode;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "customer_bill_code", nullable = false, length = 64)
    private String customerBillCode;

    @Column(name = "customer_name", nullable = false, length = 128)
    private String customerName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "period", nullable = false, length = 32)
    private String period;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BillPaymentJpaEntity() {}

    public BillPaymentJpaEntity(UUID id, String paymentCode, UUID sourceAccountId, String providerCode,
                               String customerBillCode, String customerName, BigDecimal amount,
                               BigDecimal fee, String period, String status, UUID transactionId, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.paymentCode = paymentCode;
        this.sourceAccountId = sourceAccountId;
        this.providerCode = providerCode;
        this.customerBillCode = customerBillCode;
        this.customerName = customerName;
        this.amount = amount;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.period = period;
        this.status = status;
        this.transactionId = transactionId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getCustomerBillCode() {
        return customerBillCode;
    }

    public void setCustomerBillCode(String customerBillCode) {
        this.customerBillCode = customerBillCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
