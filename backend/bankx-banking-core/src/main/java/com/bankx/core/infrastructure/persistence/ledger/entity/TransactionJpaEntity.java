package com.bankx.core.infrastructure.persistence.ledger.entity;

import com.bankx.core.domain.ledger.model.TransactionStatus;
import com.bankx.core.domain.ledger.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity JPA lưu trữ bảng giao dịch tài chính (transactions).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Entity}: Đánh dấu Class là JPA Entity đại diện cho bảng {@code transactions}.</li>
 *   <li>{@code @Table(name = "transactions")}: Khai báo tên bảng tương ứng trong Postgres.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LedgerEntryJpaEntity> entries = new ArrayList<>();

    public TransactionJpaEntity() {}

    public TransactionJpaEntity(UUID id, String transactionReference, TransactionType transactionType, TransactionStatus status, BigDecimal amount, String currency, String description, Instant createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.transactionType = transactionType;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<LedgerEntryJpaEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryJpaEntity> entries) {
        this.entries = entries;
    }

    public void addEntry(LedgerEntryJpaEntity entry) {
        entries.add(entry);
        entry.setTransaction(this);
    }
}
