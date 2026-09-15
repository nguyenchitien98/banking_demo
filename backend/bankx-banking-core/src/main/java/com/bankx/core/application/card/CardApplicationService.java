package com.bankx.core.application.card;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.card.dto.CardDetailResponse;
import com.bankx.core.application.card.dto.CreateVirtualCardRequest;
import com.bankx.core.application.card.dto.UpdateCardLimitsRequest;
import com.bankx.core.domain.card.BankCardJpaEntity;
import com.bankx.core.domain.card.CardStatus;
import com.bankx.core.domain.card.CardTokenizationService;
import com.bankx.core.domain.card.CardType;
import com.bankx.core.domain.card.SpringDataBankCardRepository;
import com.bankx.core.infrastructure.persistence.account.BankAccountJpaEntity;
import com.bankx.core.infrastructure.persistence.account.SpringDataBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service tầng Application quản lý phát hành Thẻ Ảo và chuyển đổi trạng thái Vòng đời Thẻ (Card FSM).
 * 
 * <p>Sử dụng các annotation {@link Service} để khai báo Spring Bean, {@link Slf4j}
 * để ghi log nghiệp vụ, {@link RequiredArgsConstructor} để tiêm tự động các repositories,
 * và {@link Transactional} để quản lý CSDL transaction.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardApplicationService {

    private final SpringDataBankCardRepository bankCardRepository;
    private final SpringDataBankAccountRepository bankAccountRepository;
    private final CardTokenizationService cardTokenizationService;

    /**
     * Lấy danh sách thẻ ngân hàng thuộc sở hữu của khách hàng.
     * 
     * @param customerId ID khách hàng
     * @return Danh sách DTO chi tiết thẻ
     */
    @Transactional(readOnly = true)
    public List<CardDetailResponse> getCardsByCustomerId(String customerId) {
        log.info("Lấy danh sách thẻ ngân hàng cho khách hàng [{}]", customerId);
        return bankCardRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Lấy thông tin chi tiết một thẻ ngân hàng theo ID.
     * 
     * @param cardId ID thẻ
     * @return DTO chi tiết thẻ
     */
    @Transactional(readOnly = true)
    public CardDetailResponse getCardById(String cardId) {
        BankCardJpaEntity entity = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin thẻ ngân hàng"));
        return mapToResponse(entity);
    }

    /**
     * Phát hành Thẻ Ảo mới (Virtual Card Issuance) tuân thủ quy trình Tokenization.
     * 
     * @param request Thông tin đăng ký mở thẻ ảo
     * @return DTO thông tin thẻ mới phát hành kèm CVV khởi tạo
     */
    @Transactional
    public CardDetailResponse createVirtualCard(CreateVirtualCardRequest request) {
        log.info("Khởi tạo phát hành Thẻ Ảo cho khách hàng [{}] liên kết tài khoản [{}]",
                request.customerId(), request.accountNumber());

        BankAccountJpaEntity account = bankAccountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new BankingException(ErrorCode.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản ngân hàng liên kết"));

        String rawPan = cardTokenizationService.generateRawPan(request.cardBrand());
        String maskedPan = cardTokenizationService.maskPan(rawPan);
        String panToken = cardTokenizationService.generatePanToken(request.cardBrand());
        String mockCvv = cardTokenizationService.generateCvv();

        LocalDate now = LocalDate.now();
        String expiryMonth = String.format("%02d", now.getMonthValue());
        String expiryYear = String.format("%02d", (now.getYear() + 3) % 100);

        String cardId = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BankCardJpaEntity entity = BankCardJpaEntity.builder()
                .id(cardId)
                .customerId(request.customerId())
                .accountNumber(account.getAccountNumber())
                .cardHolderName(account.getAccountName() != null ? account.getAccountName().toUpperCase() : "KHACH HANG BANKX")
                .maskedPan(maskedPan)
                .panToken(panToken)
                .cardType(CardType.VIRTUAL_DEBIT)
                .cardBrand(request.cardBrand())
                .expiryMonth(expiryMonth)
                .expiryYear(expiryYear)
                .spendingLimit(request.spendingLimit())
                .dailyLimit(request.spendingLimit().multiply(new java.math.BigDecimal("2")))
                .status(CardStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        BankCardJpaEntity saved = bankCardRepository.save(entity);
        log.info("Phát hành Thẻ Ảo thành công. Mã thẻ: {}, Token: {}", cardId, panToken);

        return new CardDetailResponse(
                saved.getId(),
                saved.getCustomerId(),
                saved.getAccountNumber(),
                saved.getCardHolderName(),
                saved.getMaskedPan(),
                saved.getPanToken(),
                mockCvv,
                saved.getCardType(),
                saved.getCardBrand(),
                saved.getExpiryMonth(),
                saved.getExpiryYear(),
                saved.getSpendingLimit(),
                saved.getDailyLimit(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    /**
     * Tạm khóa thẻ (FSM Transition: ACTIVE -> FROZEN).
     * 
     * @param cardId ID thẻ
     * @return DTO thông tin thẻ sau khi khóa
     */
    @Transactional
    public CardDetailResponse freezeCard(String cardId) {
        log.info("Yêu cầu tạm khóa thẻ [{}]", cardId);
        BankCardJpaEntity card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ ngân hàng"));

        card.getStatus().validateTransitionTo(CardStatus.FROZEN);
        card.setStatus(CardStatus.FROZEN);

        BankCardJpaEntity saved = bankCardRepository.save(card);
        log.info("Thẻ [{}] đã được tạm khóa thành công (FROZEN)", cardId);
        return mapToResponse(saved);
    }

    /**
     * Mở khóa thẻ tạm thời (FSM Transition: FROZEN -> ACTIVE).
     * 
     * @param cardId ID thẻ
     * @return DTO thông tin thẻ sau khi mở khóa
     */
    @Transactional
    public CardDetailResponse unfreezeCard(String cardId) {
        log.info("Yêu cầu mở khóa thẻ [{}]", cardId);
        BankCardJpaEntity card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ ngân hàng"));

        card.getStatus().validateTransitionTo(CardStatus.ACTIVE);
        card.setStatus(CardStatus.ACTIVE);

        BankCardJpaEntity saved = bankCardRepository.save(card);
        log.info("Thẻ [{}] đã được mở khóa hoạt động trở lại (ACTIVE)", cardId);
        return mapToResponse(saved);
    }

    /**
     * Khóa thẻ vĩnh viễn / Báo mất thẻ (FSM Transition: ACTIVE/FROZEN -> BLOCKED).
     * 
     * @param cardId ID thẻ
     * @return DTO thông tin thẻ sau khi khóa vĩnh viễn
     */
    @Transactional
    public CardDetailResponse blockCard(String cardId) {
        log.warn("Yêu cầu khóa vĩnh viễn / Báo mất thẻ [{}]", cardId);
        BankCardJpaEntity card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ ngân hàng"));

        card.getStatus().validateTransitionTo(CardStatus.BLOCKED);
        card.setStatus(CardStatus.BLOCKED);

        BankCardJpaEntity saved = bankCardRepository.save(card);
        log.warn("Thẻ [{}] đã bị khóa vĩnh viễn (BLOCKED)", cardId);
        return mapToResponse(saved);
    }

    /**
     * Cập nhật hạn mức thanh toán online và hạn mức ngày của Thẻ.
     * 
     * @param cardId ID thẻ
     * @param request DTO chứa hạn mức mới
     * @return DTO thông tin thẻ sau khi cập nhật hạn mức
     */
    @Transactional
    public CardDetailResponse updateCardLimits(String cardId, UpdateCardLimitsRequest request) {
        log.info("Cập nhật hạn mức cho thẻ [{}]: Hạn mức 1 lần [{}], Hạn mức ngày [{}]",
                cardId, request.spendingLimit(), request.dailyLimit());

        BankCardJpaEntity card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ ngân hàng"));

        if (card.getStatus() == CardStatus.BLOCKED || card.getStatus() == CardStatus.EXPIRED) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Không thể cài đặt hạn mức cho thẻ đã khóa vĩnh viễn hoặc hết hạn");
        }

        card.setSpendingLimit(request.spendingLimit());
        card.setDailyLimit(request.dailyLimit());

        BankCardJpaEntity saved = bankCardRepository.save(card);
        return mapToResponse(saved);
    }

    private CardDetailResponse mapToResponse(BankCardJpaEntity entity) {
        return new CardDetailResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAccountNumber(),
                entity.getCardHolderName(),
                entity.getMaskedPan(),
                entity.getPanToken(),
                "***", // Default masked CVV
                entity.getCardType(),
                entity.getCardBrand(),
                entity.getExpiryMonth(),
                entity.getExpiryYear(),
                entity.getSpendingLimit(),
                entity.getDailyLimit(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
