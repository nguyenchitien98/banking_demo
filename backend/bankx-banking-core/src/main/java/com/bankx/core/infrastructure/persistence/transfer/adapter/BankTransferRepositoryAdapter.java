package com.bankx.core.infrastructure.persistence.transfer.adapter;

import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.transfer.model.BankTransfer;
import com.bankx.core.domain.transfer.model.TransferStatus;
import com.bankx.core.domain.transfer.port.out.BankTransferRepository;
import com.bankx.core.infrastructure.persistence.transfer.entity.BankTransferJpaEntity;
import com.bankx.core.infrastructure.persistence.transfer.repository.SpringDataBankTransferRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter triển khai Output Port {@link BankTransferRepository} để lưu trữ và truy vấn Lệnh chuyển tiền.
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
public class BankTransferRepositoryAdapter implements BankTransferRepository {

    private final SpringDataBankTransferRepository transferRepository;

    public BankTransferRepositoryAdapter(SpringDataBankTransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    public BankTransfer save(BankTransfer transfer) {
        BankTransferJpaEntity entity = mapToJpaEntity(transfer);
        BankTransferJpaEntity saved = transferRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<BankTransfer> findById(UUID id) {
        return transferRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<BankTransfer> findByTransferCode(String transferCode) {
        return transferRepository.findByTransferCode(transferCode).map(this::mapToDomain);
    }

    @Override
    public List<BankTransfer> findByAccountId(UUID accountId, int limit) {
        return transferRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                        accountId, accountId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getDailyAccumulatedAmount(UUID sourceAccountId, Instant startOfDay) {
        BigDecimal sum = transferRepository.sumAmountBySourceAccountIdAndStatusAndCreatedAtGreaterThanEqual(
                sourceAccountId, TransferStatus.COMPLETED, startOfDay);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BankTransferJpaEntity mapToJpaEntity(BankTransfer domain) {
        return new BankTransferJpaEntity(
                domain.getId(),
                domain.getTransferCode(),
                domain.getSourceAccountId(),
                domain.getTargetAccountId(),
                domain.getTargetAccountNumber(),
                domain.getTargetAccountName(),
                domain.getAmount().getAmount(),
                domain.getAmount().getCurrency(),
                domain.getFee().getAmount(),
                domain.getDescription(),
                domain.getTransferType(),
                domain.getStatus(),
                domain.getTransactionId(),
                domain.getFailureReason(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private BankTransfer mapToDomain(BankTransferJpaEntity entity) {
        return new BankTransfer(
                entity.getId(),
                entity.getTransferCode(),
                entity.getSourceAccountId(),
                entity.getTargetAccountId(),
                entity.getTargetAccountNumber(),
                entity.getTargetAccountName(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                Money.of(entity.getFee(), entity.getCurrency()),
                entity.getDescription(),
                entity.getTransferType(),
                entity.getStatus(),
                entity.getTransactionId(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
