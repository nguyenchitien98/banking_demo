package com.bankx.core.infrastructure.persistence.transfer.adapter;

import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.transfer.model.TransferLimit;
import com.bankx.core.domain.transfer.port.out.TransferLimitRepository;
import com.bankx.core.infrastructure.persistence.transfer.entity.TransferLimitJpaEntity;
import com.bankx.core.infrastructure.persistence.transfer.repository.SpringDataTransferLimitRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter triển khai Output Port {@link TransferLimitRepository} cho hạn mức giao dịch.
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
public class TransferLimitRepositoryAdapter implements TransferLimitRepository {

    private final SpringDataTransferLimitRepository limitRepository;

    public TransferLimitRepositoryAdapter(SpringDataTransferLimitRepository limitRepository) {
        this.limitRepository = limitRepository;
    }

    @Override
    public Optional<TransferLimit> findByCustomerId(UUID customerId) {
        return limitRepository.findByCustomerId(customerId).map(this::mapToDomain);
    }

    @Override
    public TransferLimit save(TransferLimit limit) {
        TransferLimitJpaEntity entity = mapToJpaEntity(limit);
        TransferLimitJpaEntity saved = limitRepository.save(entity);
        return mapToDomain(saved);
    }

    private TransferLimitJpaEntity mapToJpaEntity(TransferLimit domain) {
        return new TransferLimitJpaEntity(
                domain.getId(),
                domain.getCustomerId(),
                domain.getSingleLimit().getAmount(),
                domain.getDailyLimit().getAmount(),
                Instant.now()
        );
    }

    private TransferLimit mapToDomain(TransferLimitJpaEntity entity) {
        return new TransferLimit(
                entity.getId(),
                entity.getCustomerId(),
                Money.ofVnd(entity.getSingleLimit().longValue()),
                Money.ofVnd(entity.getDailyLimit().longValue())
        );
    }
}
