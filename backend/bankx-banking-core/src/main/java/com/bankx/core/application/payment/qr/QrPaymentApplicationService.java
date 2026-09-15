package com.bankx.core.application.payment.qr;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.ledger.LedgerApplicationService;
import com.bankx.core.application.outbox.OutboxService;
import com.bankx.core.application.payment.qr.dto.GenerateQrRequest;
import com.bankx.core.application.payment.qr.dto.GenerateQrResponse;
import com.bankx.core.application.payment.qr.dto.ParseQrRequest;
import com.bankx.core.application.payment.qr.dto.ParseQrResponse;
import com.bankx.core.application.payment.qr.dto.PayQrRequest;
import com.bankx.core.application.payment.qr.dto.QrPaymentResult;
import com.bankx.core.infrastructure.persistence.account.BankAccountJpaEntity;
import com.bankx.core.infrastructure.persistence.account.SpringDataBankAccountRepository;
import com.bankx.core.domain.payment.qr.QrPaymentJpaEntity;
import com.bankx.core.domain.payment.qr.SpringDataQrPaymentRepository;
import com.bankx.core.domain.payment.qr.VietQrGenerator;
import com.bankx.core.domain.payment.qr.VietQrParser;
import com.bankx.core.domain.payment.qr.VietQrPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankx.core.domain.ledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service tầng Application chịu trách nhiệm quản lý quy trình thanh toán và sinh mã VietQR.
 * 
 * <p>Sử dụng các annotation {@link Service} để Spring quản lý nghiệp vụ, {@link Slf4j}
 * để ghi nhận log hệ thống, {@link RequiredArgsConstructor} để tự động tiêm dependencies,
 * và {@link Transactional} để duy trì tính nhất quán dữ liệu CSDL.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrPaymentApplicationService {

    private final VietQrParser vietQrParser;
    private final VietQrGenerator vietQrGenerator;
    private final SpringDataBankAccountRepository bankAccountRepository;
    private final SpringDataQrPaymentRepository qrPaymentRepository;
    private final LedgerApplicationService ledgerApplicationService;
    private final OutboxService outboxService;

    /**
     * Phân tích và kiểm tra tính hợp lệ của chuỗi mã VietQR.
     * 
     * @param request DTO chứa chuỗi QR
     * @return DTO chứa thông tin đã giải mã và tên người nhận
     */
    @Transactional(readOnly = true)
    public ParseQrResponse parseQr(ParseQrRequest request) {
        log.info("Phân tích chuỗi VietQR: {}", request.qrData());
        VietQrPayload payload = vietQrParser.parse(request.qrData());

        if (!payload.crcValid()) {
            log.warn("Mã QR không vượt qua kiểm tra checksum CRC-16");
        }

        // Tra cứu tên tài khoản nhận tiền trong hệ thống BankX (nếu khớp BIN)
        String holderName = payload.accountHolderName();
        if (holderName == null || holderName.isEmpty()) {
            holderName = resolveAccountHolderName(payload.bankBin(), payload.accountNumber());
        }

        return new ParseQrResponse(
                payload.bankBin(),
                payload.bankName(),
                payload.accountNumber(),
                holderName,
                payload.amount(),
                payload.description(),
                payload.isDynamic(),
                payload.crcValid(),
                payload.rawPayload()
        );
    }

    /**
     * Sinh mã VietQR tĩnh hoặc động cho tài khoản chỉ định.
     * 
     * @param request Thông số sinh mã QR (Số TK, số tiền, nội dung)
     * @return DTO chứa chuỗi VietQR chuẩn EMVCo
     */
    @Transactional(readOnly = true)
    public GenerateQrResponse generateQr(GenerateQrRequest request) {
        String bankBin = (request.bankBin() != null && !request.bankBin().isBlank()) 
                ? request.bankBin() : "970400";
        
        BankAccountJpaEntity account = bankAccountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản nhận tiền"));

        String holderName = resolveAccountHolderName(bankBin, request.accountNumber());
        String qrPayload = vietQrGenerator.generate(bankBin, request.accountNumber(), request.amount(), request.description());

        String bankName = "970423".equals(bankBin) ? "TPBank" : "BankX Digital Bank";

        return new GenerateQrResponse(
                qrPayload,
                bankBin,
                bankName,
                request.accountNumber(),
                holderName,
                request.amount(),
                request.description()
        );
    }

    /**
     * Thực thi chuyển tiền/thanh toán qua mã VietQR.
     * 
     * <p>Được bảo vệ bởi {@link Retryable} khi gặp xung đột Optimistic Locking {@link ObjectOptimisticLockingFailureException}.</p>
     * 
     * @param request DTO chi tiết giao dịch thanh toán QR
     * @return Kết quả giao dịch thanh toán QR
     */
    @Transactional
    @Retryable(
            retryFor = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2.0)
    )
    public QrPaymentResult payQr(PayQrRequest request) {
        log.info("Thực thi thanh toán QR từ tài khoản [{}] đến [{}] số tiền [{}]",
                request.sourceAccountId(), request.targetAccountNumber(), request.amount());

        UUID sourceUuid;
        try {
            sourceUuid = UUID.fromString(request.sourceAccountId());
        } catch (IllegalArgumentException e) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "ID tài khoản nguồn không hợp lệ");
        }

        BankAccountJpaEntity sourceAccount = bankAccountRepository.findById(sourceUuid)
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản nguồn"));

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new BankingException(ErrorCode.ACCOUNT_FROZEN, "Tài khoản trích tiền đang bị khóa");
        }

        if (sourceAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new BankingException(ErrorCode.INSUFFICIENT_BALANCE, "Số dư tài khoản không đủ thanh toán");
        }

        // Trích tiền tài khoản nguồn
        BigDecimal newBalance = sourceAccount.getBalance().subtract(request.amount());
        sourceAccount.setBalance(newBalance);
        bankAccountRepository.save(sourceAccount);

        // Hạch toán Double-Entry Ledger
        String paymentId = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID systemAccountId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ledgerApplicationService.recordDoubleEntry(
                sourceAccount.getId(),
                systemAccountId,
                request.amount(),
                "Thanh toán VietQR đến STK " + request.targetAccountNumber() + " - " + request.description(),
                TransactionType.INTERNAL_TRANSFER
        );

        // Lưu thông tin giao dịch QR
        QrPaymentJpaEntity entity = QrPaymentJpaEntity.builder()
                .id(paymentId)
                .sourceAccountId(sourceAccount.getId().toString())
                .targetAccountNumber(request.targetAccountNumber())
                .targetBankBin(request.targetBankBin())
                .targetAccountName(request.targetAccountName())
                .amount(request.amount())
                .description(request.description())
                .qrPayload(request.qrPayload())
                .isDynamic(true)
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build();
        qrPaymentRepository.save(entity);

        // Đẩy sự kiện Transactional Outbox
        String outboxPayload = String.format(
                "{\"paymentId\":\"%s\",\"sourceAccountId\":\"%s\",\"targetAccount\":\"%s\",\"amount\":%s,\"type\":\"QR_PAYMENT\"}",
                paymentId, sourceAccount.getId().toString(), request.targetAccountNumber(), request.amount().toPlainString()
        );
        outboxService.publishEventWithinTransaction("QrPayment", paymentId, "QR_PAYMENT_COMPLETED", outboxPayload);

        log.info("Thanh toán QR thành công. Mã giao dịch: {}", paymentId);

        return new QrPaymentResult(
                paymentId,
                sourceAccount.getId().toString(),
                request.targetAccountNumber(),
                request.targetBankBin(),
                request.targetAccountName(),
                request.amount(),
                request.description(),
                "COMPLETED",
                entity.getCreatedAt()
        );
    }

    /**
     * Tra cứu tên chủ tài khoản nhận tiền.
     * 
     * @param bankBin Mã BIN ngân hàng
     * @param accountNumber Số tài khoản
     * @return Tên chủ tài khoản in hoa không dấu
     */
    private String resolveAccountHolderName(String bankBin, String accountNumber) {
        if ("970423".equals(bankBin)) {
            return "NGUYEN CHITIEN (TPBANK MOCK)";
        }
        return bankAccountRepository.findByAccountNumber(accountNumber)
                .map(acc -> "TÀI KHOẢN BANKX " + acc.getAccountNumber().substring(acc.getAccountNumber().length() - 4))
                .orElse("TÀI KHOẢN THỤ HƯỞNG " + accountNumber);
    }
}
