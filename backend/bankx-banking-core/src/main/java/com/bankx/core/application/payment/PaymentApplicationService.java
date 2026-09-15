package com.bankx.core.application.payment;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.ledger.LedgerApplicationService;
import com.bankx.core.application.outbox.OutboxService;
import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import com.bankx.core.domain.ledger.model.Transaction;
import com.bankx.core.domain.ledger.model.TransactionType;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.domain.payment.model.PaymentExecutionResult;
import com.bankx.core.domain.payment.provider.PaymentProvider;
import com.bankx.core.domain.payment.provider.PaymentProviderFactory;
import com.bankx.core.infrastructure.persistence.payment.entity.BillPaymentJpaEntity;
import com.bankx.core.infrastructure.persistence.payment.entity.PaymentProviderJpaEntity;
import com.bankx.core.infrastructure.persistence.payment.repository.SpringDataBillPaymentRepository;
import com.bankx.core.infrastructure.persistence.payment.repository.SpringDataPaymentProviderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
 * Service ứng dụng xử lý các nghiệp vụ Thanh toán Hóa đơn Dịch vụ (Payment Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Service Bean quản lý quy trình thanh toán hóa đơn.</li>
 *   <li>{@code @Transactional}: Đảm bảo tính toàn vẹn giao dịch trích nợ, hạch toán sổ kép và lưu outbox event.</li>
 *   <li>{@code @Retryable}: Thử lại tối đa 3 lần nếu xảy ra xung đột Khóa lạc quan (@Version) trên tài khoản trích nợ.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class PaymentApplicationService {

    private final PaymentProviderFactory providerFactory;
    private final SpringDataPaymentProviderRepository providerRepository;
    private final SpringDataBillPaymentRepository paymentRepository;
    private final BankAccountRepository accountRepository;
    private final LedgerApplicationService ledgerService;
    private final OutboxService outboxService;
    private final Random random = new Random();

    public PaymentApplicationService(PaymentProviderFactory providerFactory,
                                     SpringDataPaymentProviderRepository providerRepository,
                                     SpringDataBillPaymentRepository paymentRepository,
                                     BankAccountRepository accountRepository,
                                     LedgerApplicationService ledgerService,
                                     OutboxService outboxService) {
        this.providerFactory = providerFactory;
        this.providerRepository = providerRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
    }

    /**
     * Lấy danh sách tất cả các Nhà cung cấp dịch vụ đang hoạt động.
     *
     * @return Danh sách {@link PaymentProviderJpaEntity}
     */
    @Transactional(readOnly = true)
    public List<PaymentProviderJpaEntity> getActiveProviders() {
        return providerRepository.findByStatusOrderByNameAsc("ACTIVE");
    }

    /**
     * Tra cứu thông tin nợ cước hóa đơn từ Nhà cung cấp dịch vụ thông qua Strategy Pattern.
     *
     * @param providerCode Mã nhà cung cấp (ví dụ: "EVN_HN", "WATER_HCM")
     * @param customerBillCode Mã khách hàng hóa đơn
     * @return {@link BillInquiryResponse}
     */
    @Transactional(readOnly = true)
    public BillInquiryResponse inquireBill(String providerCode, String customerBillCode) {
        PaymentProvider provider = providerFactory.getProvider(providerCode);
        return provider.inquiryBill(customerBillCode);
    }

    /**
     * Thực thi Thanh toán Hóa đơn dịch vụ.
     *
     * @param userId ID người dùng
     * @param sourceAccountId ID tài khoản trích nợ
     * @param providerCode Mã nhà cung cấp
     * @param customerBillCode Mã khách hàng
     * @param amount Số tiền thanh toán
     * @return {@link BillPaymentJpaEntity} bản ghi thanh toán đã hoàn tất
     */
    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public BillPaymentJpaEntity payBill(UUID userId, UUID sourceAccountId, String providerCode, String customerBillCode, BigDecimal amount) {
        // 1. Fetch & Validate Source Account
        BankAccount sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản trích nợ"));

        sourceAccount.checkCanTransact();

        // 2. Tra cứu chiến lược thanh toán Provider Strategy
        PaymentProvider provider = providerFactory.getProvider(providerCode);
        BillInquiryResponse inquiry = provider.inquiryBill(customerBillCode);

        // 3. Thực thi gạch nợ với Provider
        PaymentExecutionResult execResult = provider.executePayment(customerBillCode, amount);
        if (!execResult.success()) {
            throw new BankingException(ErrorCode.PAYMENT_FAILED, "Gạch nợ với nhà cung cấp thất bại: " + execResult.message());
        }


        // 4. Hạch toán sổ kép Double-Entry Ledger (Trừ tiền tài khoản người dùng -> Cộng tài khoản hệ thống)
        UUID systemAccountId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Transaction tx = ledgerService.recordDoubleEntry(
                sourceAccountId,
                systemAccountId,
                amount,
                "Thanh toan hoa don " + inquiry.providerName() + " (Ma KH: " + customerBillCode + ")",
                TransactionType.BILL_PAYMENT
        );

        // 5. Lưu vết bản ghi BillPayment
        String paymentCode = generatePaymentCode();
        BillPaymentJpaEntity payment = new BillPaymentJpaEntity(
                UUID.randomUUID(),
                paymentCode,
                sourceAccountId,
                providerCode,
                customerBillCode,
                inquiry.customerName(),
                amount,
                inquiry.fee(),
                inquiry.period(),
                "COMPLETED",
                tx.getId(),
                Instant.now()
        );

        BillPaymentJpaEntity savedPayment = paymentRepository.save(payment);

        // 6. Ghi bản tin Transactional Outbox Event
        outboxService.publishEventWithinTransaction(
                "BillPayment",
                savedPayment.getId().toString(),
                "PAYMENT_COMPLETED",
                savedPayment
        );

        return savedPayment;
    }

    /**
     * Lấy lịch sử thanh toán hóa đơn của một tài khoản.
     *
     * @param sourceAccountId ID tài khoản trích nợ
     * @return Danh sách {@link BillPaymentJpaEntity}
     */
    @Transactional(readOnly = true)
    public List<BillPaymentJpaEntity> getPaymentHistory(UUID sourceAccountId) {
        return paymentRepository.findBySourceAccountIdOrderByCreatedAtDesc(sourceAccountId);
    }

    private String generatePaymentCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int rand = 1000 + random.nextInt(9000);
        return "PAY" + dateStr + rand;
    }
}
