package com.bankx.core.domain.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Động cơ Đánh giá Quy tắc Gian lận (Fraud Detection Rule Engine).
 * 
 * <p>Sử dụng annotation {@link Component} để Spring quản lý bean engine,
 * {@link Slf4j} để ghi nhận log phân tích điểm số rủi ro, và {@link RequiredArgsConstructor}
 * để tiêm danh sách quy tắc từ CSDL.</p>
 * 
 * <p><b>5 Quy tắc tính điểm mặc định (Built-in Rules):</b></p>
 * <ul>
 *   <li><b>Rule 1 (HIGH_AMOUNT)</b>: Giao dịch &gt; 100,000,000 VND $\rightarrow$ Điểm +40</li>
 *   <li><b>Rule 2 (HIGH_VELOCITY)</b>: Giao dịch &gt; 5 lần/phút $\rightarrow$ Điểm +30</li>
 *   <li><b>Rule 3 (NEW_DEVICE)</b>: Thiết bị mới &amp; Số tiền &gt; 50,000,000 VND $\rightarrow$ Điểm +50</li>
 *   <li><b>Rule 4 (NEW_BENEFICIARY)</b>: Thụ hưởng mới &amp; Số tiền &gt; 20,000,000 VND $\rightarrow$ Điểm +20</li>
 *   <li><b>Rule 5 (NIGHT_TIME)</b>: Giao dịch Đêm khuya (0h-4h) &amp; Số tiền &gt; 10,000,000 VND $\rightarrow$ Điểm +15</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudRuleEngine {

    private final SpringDataFraudRuleRepository fraudRuleRepository;

    /**
     * Đánh giá rủi ro cho một giao dịch dựa trên ngữ cảnh {@link TransactionEvaluationContext}.
     * 
     * @param context Ngữ cảnh chứa thông tin tài khoản, thiết bị, số tiền, thời gian
     * @return Kết quả đánh giá {@link FraudEvaluationResult}
     */
    public FraudEvaluationResult evaluate(TransactionEvaluationContext context) {
        log.info("Bắt đầu đánh giá rủi ro giao dịch cho tài khoản [{}] số tiền [{}]",
                context.sourceAccountId(), context.amount());

        List<String> triggeredRules = new ArrayList<>();
        int totalScore = 0;

        // Fetch active rules from DB or fallback
        List<FraudRuleJpaEntity> activeRules = fraudRuleRepository.findByIsActive(true);

        // 1. Rule 1: HIGH_AMOUNT (> 100M)
        if (isRuleActive(activeRules, "HIGH_AMOUNT") && context.amount() != null 
                && context.amount().compareTo(new BigDecimal("100000000")) > 0) {
            triggeredRules.add("HIGH_AMOUNT");
            totalScore += getRuleScore(activeRules, "HIGH_AMOUNT", 40);
        }

        // 2. Rule 2: HIGH_VELOCITY (> 5 txs/min)
        if (isRuleActive(activeRules, "HIGH_VELOCITY") && context.velocityLastMinute() > 5) {
            triggeredRules.add("HIGH_VELOCITY");
            totalScore += getRuleScore(activeRules, "HIGH_VELOCITY", 30);
        }

        // 3. Rule 3: NEW_DEVICE + amount > 50M
        if (isRuleActive(activeRules, "NEW_DEVICE") && context.isNewDevice() 
                && context.amount() != null && context.amount().compareTo(new BigDecimal("50000000")) > 0) {
            triggeredRules.add("NEW_DEVICE");
            totalScore += getRuleScore(activeRules, "NEW_DEVICE", 50);
        }

        // 4. Rule 4: NEW_BENEFICIARY + amount > 20M
        if (isRuleActive(activeRules, "NEW_BENEFICIARY") && context.isNewBeneficiary() 
                && context.amount() != null && context.amount().compareTo(new BigDecimal("20000000")) > 0) {
            triggeredRules.add("NEW_BENEFICIARY");
            totalScore += getRuleScore(activeRules, "NEW_BENEFICIARY", 20);
        }

        // 5. Rule 5: NIGHT_TIME (0h-4h) + amount > 10M
        if (isRuleActive(activeRules, "NIGHT_TIME") 
                && (context.transactionHour() >= 0 && context.transactionHour() <= 4) 
                && context.amount() != null && context.amount().compareTo(new BigDecimal("10000000")) > 0) {
            triggeredRules.add("NIGHT_TIME");
            totalScore += getRuleScore(activeRules, "NIGHT_TIME", 15);
        }

        int finalScore = Math.min(100, totalScore);
        RiskAction action = RiskAction.fromScore(finalScore);

        String summary = String.format("Risk Score: %d/100 | Action: %s | Vi phạm: %s",
                finalScore, action.name(), String.join(", ", triggeredRules));

        log.info("Kết quả đánh giá gian lận: {}", summary);

        return new FraudEvaluationResult(finalScore, action, triggeredRules, summary);
    }

    private boolean isRuleActive(List<FraudRuleJpaEntity> rules, String ruleCode) {
        if (rules == null || rules.isEmpty()) return true;
        return rules.stream()
                .filter(r -> ruleCode.equalsIgnoreCase(r.getRuleCode()))
                .findFirst()
                .map(FraudRuleJpaEntity::getIsActive)
                .orElse(true);
    }

    private int getRuleScore(List<FraudRuleJpaEntity> rules, String ruleCode, int defaultScore) {
        if (rules == null || rules.isEmpty()) return defaultScore;
        return rules.stream()
                .filter(r -> ruleCode.equalsIgnoreCase(r.getRuleCode()))
                .findFirst()
                .map(FraudRuleJpaEntity::getWeightScore)
                .orElse(defaultScore);
    }
}
