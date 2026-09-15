package com.bankx.core.infrastructure.persistence.ledger.adapter;

import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.ledger.model.LedgerEntry;
import com.bankx.core.domain.ledger.port.out.LedgerRepository;
import com.bankx.core.domain.ledger.model.Transaction;
import com.bankx.core.infrastructure.persistence.ledger.entity.LedgerEntryJpaEntity;
import com.bankx.core.infrastructure.persistence.ledger.entity.TransactionJpaEntity;
import com.bankx.core.infrastructure.persistence.ledger.repository.SpringDataLedgerEntryRepository;
import com.bankx.core.infrastructure.persistence.ledger.repository.SpringDataTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter triển khai Output Port {@link LedgerRepository} để tương tác với cơ sở dữ liệu qua Spring Data JPA.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký Adapter làm Spring Bean trong IoC Container.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final SpringDataTransactionRepository transactionRepository;
    private final SpringDataLedgerEntryRepository ledgerEntryRepository;

    public LedgerRepositoryAdapter(SpringDataTransactionRepository transactionRepository,
                                  SpringDataLedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        TransactionJpaEntity entity = mapToJpaEntity(transaction);
        TransactionJpaEntity saved = transactionRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Transaction> findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId).map(this::mapToDomain);
    }

    @Override
    public Optional<Transaction> findTransactionByReference(String reference) {
        return transactionRepository.findByTransactionReference(reference).map(this::mapToDomain);
    }

    @Override
    public List<LedgerEntry> findEntriesByAccountId(UUID accountId, int limit) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapEntryToDomain)
                .collect(Collectors.toList());
    }

    private TransactionJpaEntity mapToJpaEntity(Transaction domain) {
        TransactionJpaEntity entity = new TransactionJpaEntity(
                domain.getId(),
                domain.getTransactionReference(),
                domain.getType(),
                domain.getStatus(),
                domain.getAmount().getAmount(),
                domain.getAmount().getCurrency(),
                domain.getDescription(),
                domain.getCreatedAt()
        );

        for (LedgerEntry entry : domain.getEntries()) {
            LedgerEntryJpaEntity entryEntity = new LedgerEntryJpaEntity(
                    entry.id(),
                    entity,
                    entry.accountId(),
                    entry.entryType(),
                    entry.amount().getAmount(),
                    entry.amount().getCurrency(),
                    entry.balanceAfter().getAmount(),
                    entry.createdAt()
            );
            entity.addEntry(entryEntity);
        }

        return entity;
    }

    private Transaction mapToDomain(TransactionJpaEntity entity) {
        Transaction domain = new Transaction(
                entity.getId(),
                entity.getTransactionReference(),
                entity.getTransactionType(),
                entity.getStatus(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getDescription(),
                entity.getCreatedAt()
        );

        for (LedgerEntryJpaEntity entryEntity : entity.getEntries()) {
            domain.addLedgerEntry(mapEntryToDomain(entryEntity));
        }

        return domain;
    }

    private LedgerEntry mapEntryToDomain(LedgerEntryJpaEntity entity) {
        return new LedgerEntry(
                entity.getId(),
                entity.getTransaction() != null ? entity.getTransaction().getId() : null,
                entity.getAccountId(),
                entity.getEntryType(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                Money.of(entity.getBalanceAfter(), entity.getCurrency()),
                entity.getCreatedAt()
        );
    }
}
