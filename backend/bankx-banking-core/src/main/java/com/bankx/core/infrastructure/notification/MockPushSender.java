package com.bankx.core.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Component giả lập gửi Push Notification qua Firebase Cloud Messaging (FCM Mock).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký MockPushSender làm Spring Bean trong IoC Container.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class MockPushSender {

    private static final Logger log = LoggerFactory.getLogger(MockPushSender.class);

    /**
     * Giả lập đẩy Push Notification tới thiết bị di động của người dùng.
     *
     * @param customerId ID khách hàng
     * @param title Tiêu đề push
     * @param body Nội dung push
     */
    public void sendPushNotification(UUID customerId, String title, String body) {
        log.info("🔔 [MOCK FCM PUSH] Sending Mobile Push to CustomerId [{}]: \"{}\" - {}",
                customerId, title, body);
    }
}
