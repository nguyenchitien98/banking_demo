package com.bankx.core.domain.ledger.model;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.account.model.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root quản lý Giao dịch Tài chính & Bút toán Ghi sổ kép (Transaction Aggregate Root).
 *
 * <p>Quản lý bất biến kế toán cốt lõi:
 * <ul>
 *   <li><b>Double-Entry Invariant:</b> Tổng tiền Ghi Nợ (DEBIT) bắt buộc phải bằng Tổng tiền Ghi Có (CREDIT).</li>
 *   <li>Mỗi giao dịch phải chứa ít nhất 1 bút toán DEBIT và 1 bút toán CREDIT.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class Transaction {

    private final UUID id;
    private final String transactionReference;
    private final TransactionType type;
    private TransactionStatus status;
    private final Money amount;
    private final String description;
    private final Instant createdAt;
    private final List<LedgerEntry> entries = new ArrayList<>();

    public Transaction(UUID id, String transactionReference, TransactionType type, TransactionStatus status, Money amount, String description, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.transactionReference = transactionReference;
        this.type = type;
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * Thêm bút toán đơn lẻ vào giao dịch.
     *
     * @param entry Bút toán {@link LedgerEntry}
     */
    public void addLedgerEntry(LedgerEntry entry) {
        if (entry != null) {
            this.entries.add(entry);
        }
    }

    /**
     * Kiểm tra và xác thực quy tắc bất biến Ghi sổ kép (Double-Entry Bookkeeping Invariant).
     *
     * <p>Quy tắc:
     * 1. Phải có ít nhất 1 bút toán DEBIT và 1 bút toán CREDIT.
     * 2. SUM(DEBIT) == SUM(CREDIT).
     * </p>
     *
     * @throws BankingException Nếu vi phạm quy tắc kế toán ghi sổ kép
     */
    public void validateDoubleEntryInvariant() {
        if (entries.isEmpty()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Giao dịch không chứa bút toán hạch toán nào");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        boolean hasDebit = false;
        boolean hasCredit = false;

        for (LedgerEntry entry : entries) {
            if (entry.entryType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(entry.amount().getAmount());
                hasDebit = true;
            } else if (entry.entryType() == EntryType.CREDIT) {
                totalCredit = totalCredit.add(entry.amount().getAmount());
                hasCredit = true;
            }
        }

        if (!hasDebit || !hasCredit) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Giao dịch bất hợp lệ: Phải bao gồm cả bút toán Ghi Nợ (DEBIT) và Ghi Có (CREDIT)");
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, String.format(
                    "Vi phạm nguyên tắc kế toán Ghi sổ kép: Tổng DEBIT (%s) không bằng Tổng CREDIT (%s)",
                    totalDebit, totalCredit
            ));
        }
    }

    public void markSuccess() {
        this.status = TransactionStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Money getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
