package com.bankx.core.infrastructure.notification;

import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.account.repository.BankAccountRepository;
import com.bankx.core.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import com.bankx.core.infrastructure.persistence.notification.repository.SpringDataNotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Event-Driven Consumer lắng nghe các sự kiện từ Kafka Topic để tạo thông báo tự động (Notification Kafka Listener).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký Event Consumer Bean.</li>
 *   <li>{@code @KafkaListener}: Đăng ký phương thức làm Kafka Event Consumer đọc bản tin từ Topic {@code transfer.completed} và {@code transfer.failed}.</li>
 * </ul>
 * </p>
 *
 * <p><b>Mô hình Idempotent Consumer:</b>
 * Sử dụng Redis key {@code consumed_event:{id}} với TTL 1 giờ. Nếu phát hiện message trùng lặp do Kafka rebalance hay network retry,
 * Consumer sẽ tự động bỏ qua để chống bắn thông báo trùng lặp cho khách hàng.
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class NotificationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaListener.class);
    private static final long CONSUMED_EVENT_TTL_SECONDS = 3600;

    private final StringRedisTemplate redisTemplate;
    private final SpringDataNotificationRepository notificationRepository;
    private final BankAccountRepository accountRepository;
    private final MockEmailSender emailSender;
    private final MockPushSender pushSender;
    private final ObjectMapper objectMapper;

    public NotificationKafkaListener(StringRedisTemplate redisTemplate,
                                      SpringDataNotificationRepository notificationRepository,
                                      BankAccountRepository accountRepository,
                                      MockEmailSender emailSender,
                                      MockPushSender pushSender,
                                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.objectMapper = objectMapper;
    }

    /**
     * Phương thức Lắng nghe bản tin từ Kafka Topic {@code transfer.completed} và {@code transfer.failed}.
     *
     * @param payload Payload JSON của bản tin từ Outbox Event
     */
    @KafkaListener(topics = {"transfer.completed", "transfer.failed"}, groupId = "bankx-notification-group")
    @Transactional
    public void handleTransferEvent(String payload) {
        log.info("Sprint 12 Notification Consumer nhận Kafka Event: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String transferId = root.has("id") ? root.get("id").asText() : UUID.randomUUID().toString();
            String transferCode = root.has("transferCode") ? root.get("transferCode").asText() : "TRF-UNKNOWN";
            String status = root.has("status") ? root.get("status").asText() : "COMPLETED";
            String sourceAccountIdStr = root.has("sourceAccountId") ? root.get("sourceAccountId").asText() : null;
            String targetAccountName = root.has("targetAccountName") ? root.get("targetAccountName").asText() : "N/A";
            String targetAccountNumber = root.has("targetAccountNumber") ? root.get("targetAccountNumber").asText() : "N/A";
            double amountVal = root.has("amount") && root.get("amount").has("amount") ? root.get("amount").get("amount").asDouble() : 0.0;

            // 1. Idempotent Consumer Check via Redis
            String lockKey = "consumed_event:" + transferId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(CONSUMED_EVENT_TTL_SECONDS));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("⚠️ [Idempotent Consumer Hit] Bản tin transferId [{}] đã được xử lý trước đó, tự động bỏ qua để chống bắn thông báo trùng!", transferId);
                return;
            }

            // 2. Tìm Customer ID từ Source Account
            UUID customerId = UUID.randomUUID(); // Default fallback
            if (sourceAccountIdStr != null) {
                try {
                    UUID sourceAccId = UUID.fromString(sourceAccountIdStr);
                    BankAccount sourceAcc = accountRepository.findById(sourceAccId).orElse(null);
                    if (sourceAcc != null) {
                        customerId = sourceAcc.getCustomerId();
                    }
                } catch (Exception ex) {
                    log.warn("Không thể parse sourceAccountId [{}]: {}", sourceAccountIdStr, ex.getMessage());
                }
            }

            // 3. Xử lý nghiệp vụ thông báo theo loại sự kiện
            boolean isSuccess = !"FAILED".equalsIgnoreCase(status);
            String title = isSuccess
                    ? "Biến động số dư: Chuyển tiền thành công"
                    : "Cảnh báo: Giao dịch chuyển tiền thất bại";

            String content = isSuccess
                    ? String.format("Giao dịch %s: Bạn đã chuyển thành công %,.0f VND tới STK %s (%s).", transferCode, amountVal, targetAccountNumber, targetAccountName)
                    : String.format("Giao dịch %s chuyển %,.0f VND tới STK %s thất bại.", transferCode, amountVal, targetAccountNumber);

            String type = isSuccess ? "TRANSFER_SUCCESS" : "TRANSFER_FAILED";

            // Save In-App Notification record to DB
            NotificationJpaEntity notification = new NotificationJpaEntity(
                    UUID.randomUUID(),
                    customerId,
                    title,
                    content,
                    type,
                    false,
                    transferCode,
                    Instant.now()
            );
            notificationRepository.save(notification);

            // Send Email & FCM Push
            emailSender.sendEmail("customer@bankx.com", title, content);
            pushSender.sendPushNotification(customerId, title, content);

            log.info("✅ Đã xử lý và bắn thông báo Event-Driven thành công cho giao dịch [{}]", transferCode);

        } catch (Exception e) {
            log.error("❌ Lỗi khi parse Kafka Notification Event: {}", e.getMessage(), e);
        }
    }
}
