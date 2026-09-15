package com.bankx.core.infrastructure.notification;

import com.bankx.common.util.MaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Component giả lập gửi Email thông báo biến động số dư và cảnh báo giao dịch.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký MockEmailSender làm Spring Bean trong IoC Container.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class MockEmailSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

    /**
     * Giả lập gửi Email cho khách hàng (an toàn thông tin, mask địa chỉ email).
     *
     * @param toEmail Email người nhận
     * @param subject Tiêu đề email
     * @param body Nội dung email
     */
    public void sendEmail(String toEmail, String subject, String body) {
        log.info("📧 [MOCK EMAIL SENDER] Sending Email to [{}]: Subject: \"{}\" | Body: {}",
                MaskingUtils.maskEmail(toEmail), subject, body);
    }
}
