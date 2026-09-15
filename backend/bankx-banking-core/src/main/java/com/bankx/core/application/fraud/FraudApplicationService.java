package com.bankx.core.application.fraud;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.fraud.dto.EvaluateTransactionRequest;
import com.bankx.core.application.fraud.dto.ReviewAlertRequest;
import com.bankx.core.domain.fraud.FraudAlertJpaEntity;
import com.bankx.core.domain.fraud.FraudAlertStatus;
import com.bankx.core.domain.fraud.FraudEvaluationResult;
import com.bankx.core.domain.fraud.FraudRuleEngine;
import com.bankx.core.domain.fraud.FraudRuleJpaEntity;
import com.bankx.core.domain.fraud.RiskAction;
import com.bankx.core.domain.fraud.SpringDataFraudAlertRepository;
import com.bankx.core.domain.fraud.SpringDataFraudRuleRepository;
import com.bankx.core.domain.fraud.TransactionEvaluationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Service ứng dụng quản lý tính toán Điểm rủi ro (Risk Score) và Xử lý Cảnh báo Gian lận.
 * 
 * <p>Sử dụng các annotation {@link Service} để Spring quản lý bean, {@link Slf4j}
 * ghi nhận log hệ thống, {@link RequiredArgsConstructor} tự động tiêm dependencies,
 * và {@link Transactional} để quản lý các giao dịch CSDL.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudApplicationService {

    private final FraudRuleEngine fraudRuleEngine;
    private final SpringDataFraudAlertRepository fraudAlertRepository;
    private final SpringDataFraudRuleRepository fraudRuleRepository;

    /**
     * Đánh giá rủi ro cho một yêu cầu giao dịch chuyển tiền/thanh toán.
     * 
     * @param request DTO tham số giao dịch
     * @return Kết quả đánh giá {@link FraudEvaluationResult}
     */
    @Transactional
    public FraudEvaluationResult evaluateTransaction(EvaluateTransactionRequest request) {
        int hour = (request.customHour() != null) 
                ? request.customHour() : LocalTime.now().getHour();

        TransactionEvaluationContext context = new TransactionEvaluationContext(
                request.sourceAccountId(),
                request.targetAccountNumber(),
                request.amount(),
                request.isNewDevice(),
                request.isNewBeneficiary(),
                request.velocityLastMinute(),
                hour
        );

        FraudEvaluationResult result = fraudRuleEngine.evaluate(context);

        // Nếu giao dịch có Risk Score >= 70 (Hành động BLOCK), tự động tạo Cảnh báo Fraud Alert vào DB
        if (result.riskAction() == RiskAction.BLOCK) {
            String alertId = "ALERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            FraudAlertJpaEntity alert = FraudAlertJpaEntity.builder()
                    .id(alertId)
                    .transactionId(txId)
                    .sourceAccountId(request.sourceAccountId())
                    .targetAccountNumber(request.targetAccountNumber())
                    .amount(request.amount())
                    .riskScore(result.riskScore())
                    .riskAction(result.riskAction())
                    .triggeredRules(String.join(", ", result.triggeredRules()))
                    .status(FraudAlertStatus.PENDING_REVIEW)
                    .createdAt(Instant.now())
                    .build();

            fraudAlertRepository.save(alert);
            log.warn("Đã lưu Cảnh báo Gian lận (Fraud Alert) mới vào CSDL. Alert ID: {}", alertId);
        }

        return result;
    }

    /**
     * Lấy danh sách tất cả các Cảnh báo Gian lận trong hệ thống.
     * 
     * @return Danh sách entity cảnh báo gian lận
     */
    @Transactional(readOnly = true)
    public List<FraudAlertJpaEntity> getAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Phê duyệt / Xử lý một Cảnh báo Gian lận.
     * 
     * @param alertId ID cảnh báo
     * @param request DTO chứa trạng thái mới và ghi chú
     * @return Entity cảnh báo sau khi cập nhật
     */
    @Transactional
    public FraudAlertJpaEntity reviewAlert(String alertId, ReviewAlertRequest request) {
        log.info("Duyệt Cảnh báo Gian lận [{}]: Trạng thái mới [{}]", alertId, request.newStatus());

        FraudAlertJpaEntity alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cảnh báo gian lận"));

        alert.setStatus(request.newStatus());
        alert.setReviewerNotes(request.reviewerNotes());

        return fraudAlertRepository.save(alert);
    }

    /**
     * Lấy danh sách các Quy tắc Gian lận (Fraud Rules).
     * 
     * @return Danh sách quy tắc
     */
    @Transactional(readOnly = true)
    public List<FraudRuleJpaEntity> getRules() {
        return fraudRuleRepository.findAll();
    }

    /**
     * Bật / Tắt một Quy tắc Gian lận.
     * 
     * @param ruleId ID quy tắc
     * @param isActive Trạng thái bật/tắt
     * @return Entity quy tắc sau khi cập nhật
     */
    @Transactional
    public FraudRuleJpaEntity toggleRule(String ruleId, boolean isActive) {
        log.info("Thay đổi trạng thái quy tắc [{}]: isActive={}", ruleId, isActive);

        FraudRuleJpaEntity rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy tắc gian lận"));

        rule.setIsActive(isActive);
        return fraudRuleRepository.save(rule);
    }
}
