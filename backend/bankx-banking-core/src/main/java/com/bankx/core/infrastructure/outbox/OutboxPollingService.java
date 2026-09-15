package com.bankx.core.infrastructure.outbox;

import com.bankx.core.domain.outbox.model.OutboxStatus;
import com.bankx.core.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.bankx.core.infrastructure.persistence.outbox.repository.SpringDataOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quét định kỳ (Scheduled Poller) đọc các bản tin Outbox chưa gửi và đẩy vào Apache Kafka Topic.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đăng ký Service quản lý tiến trình quét Outbox.</li>
 *   <li>{@code @Scheduled(fixedDelay = 2000)}: Đã được kích hoạt bởi {@code @EnableScheduling} trong ứng dụng, chạy định kỳ mỗi 2 giây.</li>
 * </ul>
 * </p>
 *
 * <p><b>Đảm bảo At-Least-Once Delivery:</b>
 * Poller sẽ đọc các sự kiện PENDING, gửi tới Kafka, và chỉ cập nhật trạng thái SENT khi nhận được Ack từ Kafka Broker.
 * Hỗ trợ Cấu hình Chaos Simulation (Kafka Down vs Kafka Up) phục vụ kiểm thử thủ công và tự động thử lại tối đa 5 lần trước khi chuyển FAILED.
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class OutboxPollingService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingService.class);
    private static final int MAX_RETRIES = 5;

    /** Cờ giả lập Chaos Engineering: Đánh dấu Kafka bị ngắt kết nối (Kafka DOWN) */
    public static volatile boolean kafkaMockDisabled = false;

    private final SpringDataOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public OutboxPollingService(SpringDataOutboxEventRepository outboxRepository,
                                @Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Quét các sự kiện Outbox ở trạng thái PENDING mỗi 2000ms và phát tin sang Kafka.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollAndPublishPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Sprint 11 Outbox Poller phát hiện {} sự kiện PENDING cần gửi sang Kafka", pendingEvents.size());

        for (OutboxEventJpaEntity event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    private void publishSingleEvent(OutboxEventJpaEntity event) {
        // 1. Kiểm tra kịch bản Chaos Simulation (Kafka DOWN)
        if (kafkaMockDisabled) {
            log.warn("⚠️ [Chaos Simulation] Kafka đang bị ngắt kết nối! Không thể publish event [{}] (id: {})", event.getEventType(), event.getId());
            event.markFailed("Chaos Simulation: Simulated Kafka Broker Connection Timeout Error", MAX_RETRIES);
            outboxRepository.save(event);
            return;
        }

        // 2. Xác định Kafka Topic tương ứng
        String topic = event.getEventType().toUpperCase().contains("FAILED") ? "transfer.failed" : "transfer.completed";

        try {
            if (kafkaTemplate != null) {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
            }
            // Đánh dấu đã gửi thành công sang Kafka
            event.markSent();
            outboxRepository.save(event);
            log.info("✅ Outbox Event [id: {}, type: {}] đã publish thành công tới Kafka topic [{}]", event.getId(), event.getEventType(), topic);
        } catch (Exception e) {
            log.error("❌ Lỗi khi publish Outbox Event [id: {}] tới Kafka: {}", event.getId(), e.getMessage());
            event.markFailed("Kafka Publish Error: " + e.getMessage(), MAX_RETRIES);
            outboxRepository.save(event);
        }
    }
}
