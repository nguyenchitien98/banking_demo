package com.bankx.core.application.account;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.common.util.MaskingUtils;
import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.model.Money;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import com.bankx.core.domain.customer.model.Customer;
import com.bankx.core.domain.customer.repository.CustomerRepository;
import com.bankx.core.infrastructure.cache.AccountCacheService;
import com.bankx.core.presentation.account.dto.BankAccountResponse;
import com.bankx.core.presentation.account.dto.CreateAccountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Application Service quản lý tài khoản thanh toán ngân hàng (Account Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Spring Service Bean chứa luồng xử lý nghiệp vụ tài khoản.</li>
 *   <li>{@code @Transactional}: Đảm bảo giao dịch CSDL an toàn, tự động tăng @Version (Optimistic Lock).</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class AccountApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AccountApplicationService.class);
    private static final SecureRandom random = new SecureRandom();

    private final BankAccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountCacheService cacheService;

    public AccountApplicationService(BankAccountRepository accountRepository,
                                     CustomerRepository customerRepository,
                                     AccountCacheService cacheService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.cacheService = cacheService;
    }

    /**
     * Lấy danh sách tài khoản thuộc về người dùng đang đăng nhập (kèm Redis Balance Caching).
     *
     * @param userId ID người dùng
     * @return Danh sách {@link BankAccountResponse}
     */
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getMyAccounts(UUID userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin khách hàng"));

        List<BankAccount> accounts = accountRepository.findByCustomerId(customer.getId());

        return accounts.stream().map(account -> {
            // Kiểm tra Redis Cache số dư
            BigDecimal cachedBalance = cacheService.getCachedBalance(account.getAccountNumber());
            BigDecimal balanceToUse = cachedBalance != null ? cachedBalance : account.getBalance().getAmount();

            if (cachedBalance == null) {
                cacheService.cacheBalance(account.getAccountNumber(), account.getBalance().getAmount());
            }

            return buildResponseWithBalance(account, balanceToUse);
        }).toList();
    }

    /**
     * Mở tài khoản thanh toán mới cho khách hàng.
     *
     * @param userId ID người dùng
     * @param request Payload {@link CreateAccountRequest}
     * @return {@link BankAccountResponse} tài khoản vừa mở
     */
    @Transactional
    public BankAccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin khách hàng"));

        // Sinh số tài khoản 10 chữ số độc nhất ngẫu nhiên (ví dụ: 0008881234)
        String accountNumber = "000" + (10000000 + random.nextInt(90000000));

        Money initialBalance = Money.ofVnd(0);
        BankAccount newAccount = new BankAccount(
                UUID.randomUUID(),
                customer.getId(),
                accountNumber,
                request.accountName() != null ? request.accountName() : "Tài khoản thanh toán",
                initialBalance,
                request.currency() != null ? request.currency() : Money.DEFAULT_CURRENCY,
                "ACTIVE",
                0L
        );

        BankAccount saved = accountRepository.save(newAccount);
        log.info("Mở tài khoản thanh toán mới thành công cho CIF [{}], STK: [{}]", customer.getCifNumber(), MaskingUtils.maskAccountNumber(saved.getAccountNumber()));
        return buildResponse(saved);
    }

    /**
     * Phong tỏa tài khoản (FREEZE).
     *
     * @param accountId ID tài khoản cần phong tỏa
     * @return {@link BankAccountResponse}
     */
    @Transactional
    public BankAccountResponse freezeAccount(UUID accountId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.freeze();
        BankAccount saved = accountRepository.save(account);
        cacheService.evictBalanceCache(saved.getAccountNumber());

        log.info("Đã phong tỏa tài khoản STK: [{}]", MaskingUtils.maskAccountNumber(saved.getAccountNumber()));
        return buildResponse(saved);
    }

    private BankAccountResponse buildResponse(BankAccount account) {
        return buildResponseWithBalance(account, account.getBalance().getAmount());
    }

    private BankAccountResponse buildResponseWithBalance(BankAccount account, BigDecimal balance) {
        return new BankAccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountName(),
                balance,
                account.getCurrency(),
                account.getStatus(),
                account.getVersion()
        );
    }
}
