package com.bankx.core.application.ledger;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.infrastructure.cache.AccountCacheService;
import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import com.bankx.core.domain.ledger.model.*;
import com.bankx.core.domain.ledger.port.out.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service ứng dụng xử lý nghiệp vụ hạch toán kế toán ghi sổ kép (Double-Entry Bookkeeping Ledger Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Service Bean quản lý luồng nghiệp vụ ghi sổ kép.</li>
 *   <li>{@code @Transactional}: Đảm bảo tính nguyên tố (ACID) của giao dịch tài chính. Tất cả biến động số dư và bút toán ghi sổ đều cùng thành công hoặc cuộn ngược (rollback) nếu thất bại.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class LedgerApplicationService {

    private final LedgerRepository ledgerRepository;
    private final BankAccountRepository accountRepository;
    private final AccountCacheService cacheService;
    private final Random random = new Random();

    public LedgerApplicationService(LedgerRepository ledgerRepository,
                                     BankAccountRepository accountRepository,
                                     AccountCacheService cacheService) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
        this.cacheService = cacheService;
    }

    /**
     * Thực hiện hạch toán ghi sổ kép cho giao dịch giữa hai tài khoản ngân hàng.
     *
     * <p>Quy trình:
     * 1. Kiểm tra tài khoản Nợ (Debit) và Có (Credit).
     * 2. Rút tiền từ tài khoản Nợ và Nạp tiền vào tài khoản Có.
     * 3. Lưu biến động số dư tài khoản (Kiểm tra Optimistic Lock {@code @Version}).
     * 4. Tạo bút toán DEBIT và CREDIT.
     * 5. Xác thực quy tắc bất biến {@code SUM(DEBIT) == SUM(CREDIT)}.
     * 6. Lưu bản ghi giao dịch bất biến vào CSDL.
     * 7. Xóa cache Redis số dư cũ để cập nhật tức thì.
     * </p>
     *
     * @param debitAccountId ID tài khoản trích nợ (tài khoản gửi tiền)
     * @param creditAccountId ID tài khoản ghi có (tài khoản nhận tiền)
     * @param amountVal Số tiền giao dịch
     * @param description Diễn giải giao dịch
     * @param type Loại giao dịch {@link TransactionType}
     * @return Giao dịch {@link Transaction} đã hạch toán thành công
     */
    @Transactional
    public Transaction recordDoubleEntry(UUID debitAccountId, UUID creditAccountId, BigDecimal amountVal, String description, TransactionType type) {
        if (debitAccountId.equals(creditAccountId)) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Tài khoản trích nợ và tài khoản thụ hưởng không được trùng nhau");
        }

        Money transferAmount = Money.of(amountVal, "VND");

        // 1. Fetch Accounts
        BankAccount debitAccount = accountRepository.findById(debitAccountId)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản trích nợ: " + debitAccountId));

        BankAccount creditAccount = accountRepository.findById(creditAccountId)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản thụ hưởng: " + creditAccountId));

        // 2. Perform Money Operations (Domain Logic & Limit/Status check)
        debitAccount.withdraw(transferAmount);
        creditAccount.deposit(transferAmount);

        // 3. Persist Updated Accounts (Triggers JPA Optimistic Locking @Version)
        accountRepository.save(debitAccount);
        accountRepository.save(creditAccount);

        // 4. Generate Transaction Reference Number (e.g. TXN2026091512345678)
        String refNo = generateTransactionRef();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                refNo,
                type != null ? type : TransactionType.INTERNAL_TRANSFER,
                TransactionStatus.PENDING,
                transferAmount,
                description,
                Instant.now()
        );

        // 5. Create DEBIT Ledger Entry for Debit Account
        LedgerEntry debitEntry = new LedgerEntry(
                UUID.randomUUID(),
                transaction.getId(),
                debitAccount.getId(),
                EntryType.DEBIT,
                transferAmount,
                debitAccount.getBalance(),
                Instant.now()
        );
        transaction.addLedgerEntry(debitEntry);

        // 6. Create CREDIT Ledger Entry for Credit Account
        LedgerEntry creditEntry = new LedgerEntry(
                UUID.randomUUID(),
                transaction.getId(),
                creditAccount.getId(),
                EntryType.CREDIT,
                transferAmount,
                creditAccount.getBalance(),
                Instant.now()
        );
        transaction.addLedgerEntry(creditEntry);

        // 7. Validate Financial Invariant: SUM(DEBIT) == SUM(CREDIT)
        transaction.validateDoubleEntryInvariant();
        transaction.markSuccess();

        // 8. Save Immutable Transaction
        Transaction savedTx = ledgerRepository.saveTransaction(transaction);

        // 9. Evict Redis Balance Cache for both accounts
        cacheService.evictBalanceCache(debitAccount.getAccountNumber());
        cacheService.evictBalanceCache(creditAccount.getAccountNumber());

        return savedTx;
    }

    /**
     * Lấy chi tiết giao dịch theo ID.
     *
     * @param transactionId ID giao dịch
     * @return Aggregate Root {@link Transaction}
     */
    @Transactional(readOnly = true)
    public Transaction getTransactionDetail(UUID transactionId) {
        return ledgerRepository.findTransactionById(transactionId)
                .orElseThrow(() -> new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Không tìm thấy giao dịch với ID: " + transactionId));
    }

    /**
     * Lấy danh sách bút toán ghi sổ của một tài khoản.
     *
     * @param accountId ID tài khoản
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách {@link LedgerEntry}
     */
    @Transactional(readOnly = true)
    public List<LedgerEntry> getAccountEntries(UUID accountId, int limit) {
        return ledgerRepository.findEntriesByAccountId(accountId, limit > 0 ? limit : 20);
    }

    private String generateTransactionRef() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomDigits = 1000 + random.nextInt(9000);
        return "TXN" + timestamp + randomDigits;
    }
}
