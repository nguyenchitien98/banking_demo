package com.bankx.core.application.transfer;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.ledger.LedgerApplicationService;
import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import com.bankx.core.domain.ledger.model.Transaction;
import com.bankx.core.domain.ledger.model.TransactionType;
import com.bankx.core.domain.transfer.model.BankTransfer;
import com.bankx.core.domain.transfer.model.TransferLimit;
import com.bankx.core.domain.transfer.model.TransferStatus;
import com.bankx.core.domain.transfer.model.TransferType;
import com.bankx.core.domain.transfer.port.out.BankTransferRepository;
import com.bankx.core.domain.transfer.port.out.TransferLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Service ứng dụng xử lý các giao dịch Chuyển tiền ngân hàng (Transfer Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Service Bean quản lý các luồng nghiệp vụ chuyển tiền.</li>
 *   <li>{@code @Transactional}: Quản lý giao dịch tài chính toàn vẹn.</li>
 *   <li>{@code @Retryable}: Tự động thử lại tối đa 3 lần với Exponential Backoff khi phát hiện xung đột Optimistic Lock.</li>
 * </ul>
 * </p>
 *
 * <p><b>So sánh Kiến trúc Concurrency Control (Pessimistic vs Optimistic Locking):</b>
 * <ul>
 *   <li><b>Pessimistic Lock (SELECT FOR UPDATE):</b> Khóa cứng dòng CSDL ở mức DB Engine. Tuyệt đối an toàn nhưng gây nghẽn Connection Pool và giảm TPS nghiêm trọng khi tải cao.</li>
 *   <li><b>Optimistic Lock (@Version - Lựa chọn BankX):</b> Không khóa DB row. Hibernate tự kiểm tra {@code WHERE version = old_version}. Nếu phát hiện phiên bản bị thay đổi đồng thời, Spring ném {@link ObjectOptimisticLockingFailureException} và {@code @Retryable} sẽ tự động reload số dư mới và thử lại 3 lần.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class TransferApplicationService {

    private final BankTransferRepository transferRepository;
    private final TransferLimitRepository limitRepository;
    private final BankAccountRepository accountRepository;
    private final LedgerApplicationService ledgerService;
    private final Random random = new Random();

    public TransferApplicationService(BankTransferRepository transferRepository,
                                       TransferLimitRepository limitRepository,
                                       BankAccountRepository accountRepository,
                                       LedgerApplicationService ledgerService) {
        this.transferRepository = transferRepository;
        this.limitRepository = limitRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
    }

    /**
     * Truy vấn thông tin người thụ hưởng theo số tài khoản (Recipient Inquiry).
     *
     * @param targetAccountNumber Số tài khoản thụ hưởng
     * @return {@link BankAccount} thông tin tài khoản thụ hưởng
     */
    @Transactional(readOnly = true)
    public BankAccount inquireRecipient(String targetAccountNumber) {
        return accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản thụ hưởng có số: " + targetAccountNumber));
    }

    /**
     * Khởi tạo và thực thi giao dịch Chuyển tiền nội bộ (Internal Bank Transfer).
     *
     * <p>Tích hợp tự động Thử lại (Retry) tối đa 3 lần nếu xảy ra xung đột Khóa lạc quan (Optimistic Lock @Version).</p>
     *
     * @param userId ID người dùng thực hiện chuyển tiền
     * @param sourceAccountId ID tài khoản trích nợ
     * @param targetAccountNumber Số tài khoản thụ hưởng
     * @param amountVal Số tiền chuyển
     * @param description Nội dung chuyển tiền
     * @return Lệnh chuyển tiền {@link BankTransfer} đã hoàn thành
     */
    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public BankTransfer createInternalTransfer(UUID userId, UUID sourceAccountId, String targetAccountNumber, BigDecimal amountVal, String description) {
        Money transferAmount = Money.of(amountVal, "VND");

        // 1. Fetch & Validate Source Account
        BankAccount sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản trích nợ"));

        sourceAccount.checkCanTransact();

        // 2. Fetch & Validate Target Account
        BankAccount targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản thụ hưởng: " + targetAccountNumber));

        targetAccount.checkCanTransact();

        if (sourceAccount.getId().equals(targetAccount.getId())) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Tài khoản trích nợ và tài khoản thụ hưởng không được trùng nhau");
        }

        // 3. Transfer Limits Check
        TransferLimit limit = limitRepository.findByCustomerId(sourceAccount.getCustomerId())
                .orElseGet(() -> new TransferLimit(UUID.randomUUID(), sourceAccount.getCustomerId(), Money.ofVnd(50_000_000), Money.ofVnd(500_000_000)));

        Instant startOfDay = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).truncatedTo(ChronoUnit.DAYS).toInstant();
        BigDecimal dailySum = transferRepository.getDailyAccumulatedAmount(sourceAccountId, startOfDay);
        Money dailyAccumulated = Money.of(dailySum, "VND");

        limit.validateTransferLimit(transferAmount, dailyAccumulated);

        // 4. Create Transfer Domain Record
        String transferCode = generateTransferCode();
        BankTransfer transfer = new BankTransfer(
                UUID.randomUUID(),
                transferCode,
                sourceAccountId,
                targetAccount.getId(),
                targetAccount.getAccountNumber(),
                targetAccount.getAccountName(),
                transferAmount,
                Money.ZERO, // Miễn phí chuyển tiền nội bộ BankX
                description != null ? description : "Chuyển tiền nội bộ BankX",
                TransferType.INTERNAL,
                TransferStatus.PROCESSING,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        // 5. Execute Double-Entry Ledger Transaction & Account Balance Updates
        Transaction tx = ledgerService.recordDoubleEntry(
                sourceAccountId,
                targetAccount.getId(),
                amountVal,
                transfer.getDescription(),
                TransactionType.INTERNAL_TRANSFER
        );

        // 6. Mark Transfer COMPLETED
        transfer.markCompleted(tx.getId());
        return transferRepository.save(transfer);
    }

    /**
     * Lấy chi tiết lệnh chuyển tiền theo ID.
     *
     * @param transferId ID lệnh chuyển tiền
     * @return {@link BankTransfer}
     */
    @Transactional(readOnly = true)
    public BankTransfer getTransferDetail(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin lệnh chuyển tiền: " + transferId));
    }

    /**
     * Lấy lịch sử lệnh chuyển tiền của một tài khoản.
     *
     * @param accountId ID tài khoản
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách {@link BankTransfer}
     */
    @Transactional(readOnly = true)
    public List<BankTransfer> getAccountTransfers(UUID accountId, int limit) {
        return transferRepository.findByAccountId(accountId, limit > 0 ? limit : 20);
    }

    private String generateTransferCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rand = 1000 + random.nextInt(9000);
        return "TRF" + dateStr + rand;
    }
}
