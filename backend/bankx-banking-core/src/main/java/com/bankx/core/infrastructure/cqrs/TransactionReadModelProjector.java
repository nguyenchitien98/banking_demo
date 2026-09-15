package com.bankx.core.infrastructure.cqrs;

import com.bankx.core.domain.cqrs.SpringDataTransactionHistoryViewRepository;
import com.bankx.core.domain.cqrs.TransactionHistoryViewJpaEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Projector lắng nghe các sự kiện sự vụ (Events) từ Kafka để cập nhật vào CQRS Read Model {@code transaction_history_views}.
 * 
 * <p>Annotation {@link Component} đăng ký class này là một Spring Component chịu trách nhiệm duy trì tính Đồng bộ Đạt sau (Eventual Consistency)
 * giữa Write Model (Cơ sở dữ liệu giao dịch cốt lõi) và Read Model (Bảng truy vấn đọc siêu tốc).</p>
 * 
 * <p>Annotation {@link KafkaListener} lắng nghe các Topic giao dịch {@code transfer.completed}, {@code payment.completed}, {@code qr.completed}.
 * Tích hợp Idempotent Consumer bằng Redis key {@code read_model_projected:{id}} chống tạo trùng bản ghi Read Model.</p>
 */
@Component
public class TransactionReadModelProjector {

    private static final Logger log = LoggerFactory.getLogger(TransactionReadModelProjector.class);
    private static final long PROJECTOR_LOCK_TTL_SECONDS = 3600;

    private final SpringDataTransactionHistoryViewRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TransactionReadModelProjector(SpringDataTransactionHistoryViewRepository repository,
                                          StringRedisTemplate redisTemplate,
                                          ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Lắng nghe bản tin giao dịch từ Kafka và ghi nhận vào CQRS Read Model.
     *
     * @param payload Payload JSON của bản tin sự kiện giao dịch
     */
    @KafkaListener(topics = {"transfer.completed", "payment.completed", "qr.completed"}, groupId = "bankx-cqrs-read-model-group")
    @Transactional
    public void projectToReadModel(String payload) {
        log.info("CQRS Read Model Projector nhận Kafka Event: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String txId = root.has("id") ? root.get("id").asText() : UUID.randomUUID().toString();
            String txRef = root.has("transferCode") ? root.get("transferCode").asText() : "REF-" + System.currentTimeMillis();
            String customerId = root.has("customerId") ? root.get("customerId").asText() : "CUST-001";
            String sourceAcc = root.has("sourceAccountNumber") ? root.get("sourceAccountNumber").asText() : "1000188888";
            String targetAcc = root.has("targetAccountNumber") ? root.get("targetAccountNumber").asText() : "N/A";
            String targetName = root.has("targetAccountName") ? root.get("targetAccountName").asText() : "N/A";
            double amount = root.has("amount") && root.get("amount").has("amount") ? root.get("amount").get("amount").asDouble() : 0.0;
            String type = root.has("type") ? root.get("type").asText() : "INTERNAL_TRANSFER";
            String category = root.has("category") ? root.get("category").asText() : "TRANSFER";
            String description = root.has("description") ? root.get("description").asText() : "Chuyển tiền thành công qua CQRS Projector";

            // Idempotent Consumer check via Redis
            String lockKey = "read_model_projected:" + txId;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(PROJECTOR_LOCK_TTL_SECONDS));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("⚠️ [CQRS Projector Hit] Transaction [{}] đã được ghi vào Read Model trước đó, bỏ qua trùng lặp!", txId);
                return;
            }

            // Ghi nhận bản ghi Nợ (DEBIT) cho tài khoản trích nợ
            TransactionHistoryViewJpaEntity debitView = TransactionHistoryViewJpaEntity.builder()
                    .id("READ-DEBIT-" + txId)
                    .transactionReference(txRef)
                    .customerId(customerId)
                    .accountNumber(sourceAcc)
                    .oppositeAccountNumber(targetAcc)
                    .oppositeAccountName(targetName)
                    .amount(BigDecimal.valueOf(amount))
                    .direction("DEBIT")
                    .transactionType(type)
                    .category(category)
                    .description(description)
                    .status("COMPLETED")
                    .createdAt(Instant.now())
                    .build();
            repository.save(debitView);

            // Ghi nhận bản ghi Có (CREDIT) cho tài khoản thụ hưởng nếu hợp lệ
            if (!"N/A".equalsIgnoreCase(targetAcc) && !targetAcc.isBlank()) {
                TransactionHistoryViewJpaEntity creditView = TransactionHistoryViewJpaEntity.builder()
                        .id("READ-CREDIT-" + txId)
                        .transactionReference(txRef)
                        .customerId("CUST-TARGET")
                        .accountNumber(targetAcc)
                        .oppositeAccountNumber(sourceAcc)
                        .oppositeAccountName("Tài khoản nguồn")
                        .amount(BigDecimal.valueOf(amount))
                        .direction("CREDIT")
                        .transactionType(type)
                        .category(category)
                        .description("Nhận tiền từ " + sourceAcc)
                        .status("COMPLETED")
                        .createdAt(Instant.now())
                        .build();
                repository.save(creditView);
            }

            log.info("✅ [CQRS Projection] Đã ghi thành công Read Model cho giao dịch [{}]", txRef);

        } catch (Exception e) {
            log.error("❌ Lỗi khi project sự kiện vào CQRS Read Model: {}", e.getMessage(), e);
        }
    }
}
