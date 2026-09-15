package com.bankx.core.domain.fraud;

/**
 * Hành động xử lý dựa trên Điểm rủi ro (Risk Score) của giao dịch.
 * 
 * <ul>
 *   <li><b>ALLOW</b>: Risk Score &lt; 40 $\rightarrow$ Phê duyệt giao dịch tức thì.</li>
 *   <li><b>OTP_REQUIRED</b>: Risk Score 40 – 70 $\rightarrow$ Yêu cầu xác thực OTP 2 lớp.</li>
 *   <li><b>BLOCK</b>: Risk Score &gt; 70 $\rightarrow$ Từ chối giao dịch &amp; Bắn Cảnh báo Gian lận (Fraud Alert).</li>
 * </ul>
 */
public enum RiskAction {
    ALLOW,
    OTP_REQUIRED,
    BLOCK;

    /**
     * Phân loại hành động xử lý dựa trên điểm số Risk Score (0 - 100).
     * 
     * @param score Tổng điểm rủi ro
     * @return Đối tượng {@link RiskAction} tương ứng
     */
    public static RiskAction fromScore(int score) {
        if (score >= 70) {
            return BLOCK;
        } else if (score >= 40) {
            return OTP_REQUIRED;
        } else {
            return ALLOW;
        }
    }
}
