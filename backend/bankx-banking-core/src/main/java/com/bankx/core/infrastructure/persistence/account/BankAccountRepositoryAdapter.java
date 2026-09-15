package com.bankx.core.infrastructure.persistence.account;

import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter triển khai Port Interface {@link BankAccountRepository}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Khai báo Spring Repository Bean persistence cho BankAccount.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public class BankAccountRepositoryAdapter implements BankAccountRepository {

    private final SpringDataBankAccountRepository jpaRepository;

    public BankAccountRepositoryAdapter(SpringDataBankAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<BankAccount> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public Optional<BankAccount> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public BankAccount save(BankAccount account) {
        BankAccountJpaEntity entity = toEntity(account);
        BankAccountJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private BankAccount toDomain(BankAccountJpaEntity entity) {
        Money balance = Money.of(entity.getBalance(), entity.getCurrency());
        BankAccount account = new BankAccount(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                balance,
                entity.getCurrency(),
                entity.getStatus(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
        account.setCreatedAt(entity.getCreatedAt());
        account.setUpdatedAt(entity.getUpdatedAt());
        return account;
    }

    private BankAccountJpaEntity toEntity(BankAccount account) {
        BankAccountJpaEntity entity = new BankAccountJpaEntity();
        entity.setId(account.getId());
        entity.setCustomerId(account.getCustomerId());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setAccountName(account.getAccountName());
        entity.setBalance(account.getBalance().getAmount());
        entity.setCurrency(account.getCurrency());
        entity.setStatus(account.getStatus());
        entity.setVersion(account.getVersion());
        return entity;
    }
}
