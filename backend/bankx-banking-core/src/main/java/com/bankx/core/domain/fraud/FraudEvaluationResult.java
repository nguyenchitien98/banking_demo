package com.bankx.core.domain.fraud;

import java.util.List;

/**
 * Kết quả đánh giá rủi ro gian lận từ Engine.
 * 
 * @param riskScore Tổng điểm rủi ro (0 - 100)
 * @param riskAction Hành động đề xuất (ALLOW, OTP_REQUIRED, BLOCK)
 * @param triggeredRules Danh sách mã quy tắc vi phạm
 * @param summaryDetail Mô tả chi tiết kết quả đánh giá
 */
public record FraudEvaluationResult(
        int riskScore,
        RiskAction riskAction,
        List<String> triggeredRules,
        String summaryDetail
) {
}
