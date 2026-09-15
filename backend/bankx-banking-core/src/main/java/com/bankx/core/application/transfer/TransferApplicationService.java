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

import com.bankx.core.infrastructure.sms.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

import com.bankx.core.application.outbox.OutboxService;

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
 * <p><b>So sánh Kiến trúc Concurrency Control, Risk-Based OTP & Outbox Pattern (Sprint 11):</b>
 * <ul>
 *   <li><b>Chuyển tiền thấp hơn 5.000.000 VND:</b> Giao dịch hoàn tất ngay lập tức.</li>
 *   <li><b>Chuyển tiền từ 5.000.000 VND trở lên:</b> Chuyển sang trạng thái {@code PENDING_OTP}, sinh mã OTP 6 chữ số lưu Redis 120s và chờ người dùng xác thực.</li>
 *   <li><b>Outbox Pattern:</b> Ghi bản tin {@code TRANSFER_COMPLETED} hoặc {@code TRANSFER_FAILED} vào bảng {@code outbox_events} trong cùng Database Transaction.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class TransferApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TransferApplicationService.class);
    public static final BigDecimal RISK_THRESHOLD_OTP = new BigDecimal("5000000");
    public static final long OTP_TTL_SECONDS = 120;

    private final BankTransferRepository transferRepository;
    private final TransferLimitRepository limitRepository;
    private final BankAccountRepository accountRepository;
    private final LedgerApplicationService ledgerService;
    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;
    private final OutboxService outboxService;
    private final Random random = new Random();

    public TransferApplicationService(BankTransferRepository transferRepository,
                                       TransferLimitRepository limitRepository,
                                       BankAccountRepository accountRepository,
                                       LedgerApplicationService ledgerService,
                                       StringRedisTemplate redisTemplate,
                                       SmsSender smsSender,
                                       OutboxService outboxService) {
        this.transferRepository = transferRepository;
        this.limitRepository = limitRepository;
        this.accountRepository = accountRepository;
        this.ledgerService = ledgerService;
        this.redisTemplate = redisTemplate;
        this.smsSender = smsSender;
        this.outboxService = outboxService;
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
     * <p>Tích hợp Risk-based OTP & Transactional Outbox Pattern.</p>
     *
     * @param userId ID người dùng thực hiện chuyển tiền
     * @param sourceAccountId ID tài khoản trích nợ
     * @param targetAccountNumber Số tài khoản thụ hưởng
     * @param amountVal Số tiền chuyển
     * @param description Nội dung chuyển tiền
     * @return Lệnh chuyển tiền {@link BankTransfer} ở trạng thái COMPLETED hoặc PENDING_OTP
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

        // Check if Risk-based OTP is required (amount >= 5,000,000 VND)
        boolean isHighRisk = amountVal.compareTo(RISK_THRESHOLD_OTP) >= 0;
        TransferStatus initialStatus = isHighRisk ? TransferStatus.PENDING_OTP : TransferStatus.PROCESSING;

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
                initialStatus,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        if (isHighRisk) {
            // Lưu record chuyển tiền ở trạng thái PENDING_OTP
            BankTransfer savedTransfer = transferRepository.save(transfer);

            // Sinh mã OTP 6 chữ số
            String otpCode = String.valueOf(100000 + random.nextInt(900000));
            String otpKey = "transfer_otp:" + savedTransfer.getId();
            redisTemplate.opsForValue().set(otpKey, otpCode, Duration.ofSeconds(OTP_TTL_SECONDS));

            String smsContent = String.format("[BankX] Ma OTP xac thuc giao dich chuyen tien %s VND (%s) la: %s. Hieu luc 2 phut.",
                    amountVal, transferCode, otpCode);
            smsSender.sendSms("CUSTOMER_PHONE", smsContent);
            log.info("Sprint 10 Risk-based OTP generated for transferId [{}] code [{}]: OTP [{}]", savedTransfer.getId(), transferCode, otpCode);

            return savedTransfer;
        }

        // 5. Nếu < 5M VND: Thực thi hạch toán sổ kép & Cập nhật số dư ngay lập tức
        Transaction tx = ledgerService.recordDoubleEntry(
                sourceAccountId,
                targetAccount.getId(),
                amountVal,
                transfer.getDescription(),
                TransactionType.INTERNAL_TRANSFER
        );

        // 6. Mark Transfer COMPLETED
        transfer.markCompleted(tx.getId());
        BankTransfer completedTransfer = transferRepository.save(transfer);

        // Sprint 11: Transactional Outbox Event Publishing
        outboxService.publishEventWithinTransaction("BankTransfer", completedTransfer.getId().toString(), "TRANSFER_COMPLETED", completedTransfer);

        return completedTransfer;
    }

    /**
     * Xác thực mã OTP và hoàn tất giao dịch Chuyển tiền.
     *
     * @param userId ID người dùng
     * @param transferId ID lệnh chuyển tiền
     * @param otpCode Mã OTP 6 chữ số
     * @return Lệnh chuyển tiền {@link BankTransfer} ở trạng thái COMPLETED
     */
    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public BankTransfer confirmTransferOtp(UUID userId, UUID transferId, String otpCode) {
        BankTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giao dịch chuyển tiền: " + transferId));

        if (transfer.getStatus() != TransferStatus.PENDING_OTP) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Giao dịch không ở trạng thái chờ OTP hoặc đã hoàn tất!");
        }

        String otpKey = "transfer_otp:" + transferId;
        String attemptsKey = "transfer_otp_attempts:" + transferId;

        String savedOtp = redisTemplate.opsForValue().get(otpKey);
        if (savedOtp == null) {
            throw new BankingException(ErrorCode.OTP_INVALID_OR_EXPIRED, "Mã OTP đã hết hạn (quá 120 giây) hoặc không tồn tại. Vui lòng thực hiện lại giao dịch!");
        }

        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (!savedOtp.equals(otpCode)) {
            attempts++;
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofSeconds(300));
            log.warn("Xác thực OTP chuyển tiền thất bại cho transferId [{}]. Lần sai {}/3", transferId, attempts);

            if (attempts >= 3) {
                transfer.markFailed("Nhập sai mã OTP quá 3 lần");
                BankTransfer failedTransfer = transferRepository.save(transfer);
                redisTemplate.delete(otpKey);
                redisTemplate.delete(attemptsKey);

                // Sprint 11: Transactional Outbox Event Publishing on Failure
                outboxService.publishEventWithinTransaction("BankTransfer", failedTransfer.getId().toString(), "TRANSFER_FAILED", failedTransfer);

                throw new BankingException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED, "Bạn đã nhập sai mã OTP quá 3 lần. Giao dịch bị hủy!");
            }
            throw new BankingException(ErrorCode.OTP_INVALID_OR_EXPIRED, "Mã OTP không chính xác. Bạn còn " + (3 - attempts) + " lần thử.");
        }

        // OTP thành công -> xóa khỏi Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);

        // Thực thi hạch toán sổ kép & Cập nhật số dư
        Transaction tx = ledgerService.recordDoubleEntry(
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId(),
                transfer.getAmount().getAmount(),
                transfer.getDescription(),
                TransactionType.INTERNAL_TRANSFER
        );

        transfer.markCompleted(tx.getId());
        BankTransfer completedTransfer = transferRepository.save(transfer);

        // Sprint 11: Transactional Outbox Event Publishing on Completion
        outboxService.publishEventWithinTransaction("BankTransfer", completedTransfer.getId().toString(), "TRANSFER_COMPLETED", completedTransfer);

        return completedTransfer;
    }


    /**
     * Lấy mã OTP giả lập từ Redis (phục vụ hiển thị gợi ý kiểm thử Sprint 10 UI).
     *
     * @param transferId ID giao dịch chuyển tiền
     * @return Mã OTP hoặc null
     */
    public String getMockOtpForTransfer(UUID transferId) {
        return redisTemplate.opsForValue().get("transfer_otp:" + transferId);
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
