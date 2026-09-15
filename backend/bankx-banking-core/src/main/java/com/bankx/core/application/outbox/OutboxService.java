package com.bankx.core.application.outbox;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.outbox.model.OutboxStatus;
import com.bankx.core.infrastructure.persistence.outbox.entity.OutboxEventJpaEntity;
import com.bankx.core.infrastructure.persistence.outbox.repository.SpringDataOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service ứng dụng quản lý ghi và cập nhật sự kiện trong mô hình Transactional Outbox.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Khai báo Spring Service Bean xử lý nghiệp vụ lưu trữ Outbox Events.</li>
 * </ul>
 * </p>
 *
 * <p><b>Nguyên lý Transactional Outbox:</b>
 * Ghi bản tin Outbox Event vào CSDL <i>trong cùng một Database Transaction với nghiệp vụ chính (như Chuyển tiền)</i>.
 * Đảm bảo 100% tính toàn vẹn dữ liệu: Nếu DB Rollback thì Outbox Event cũng bị hủy, nếu DB Commit thành công thì Outbox Event chắc chắn được lưu trữ.
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final SpringDataOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(SpringDataOutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Ghi nhận sự kiện Outbox Event cùng Transaction với nghiệp vụ chính.
     *
     * @param aggregateType Loại Aggregate (ví dụ: "BankTransfer")
     * @param aggregateId ID của Aggregate (ID chuyển tiền)
     * @param eventType Loại sự kiện (ví dụ: "TRANSFER_COMPLETED", "TRANSFER_FAILED")
     * @param payload Target Payload Object được tự động serialize sang JSON
     * @return {@link OutboxEventJpaEntity} đã được lưu vào CSDL
     */
    @Transactional
    public OutboxEventJpaEntity publishEventWithinTransaction(String aggregateType, String aggregateId, String eventType, Object payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize payload outbox sang JSON cho aggregateId [{}]: {}", aggregateId, e.getMessage());
            payloadJson = String.valueOf(payload);
        }

        OutboxEventJpaEntity event = new OutboxEventJpaEntity(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payloadJson,
                OutboxStatus.PENDING,
                0,
                null,
                Instant.now(),
                null
        );

        OutboxEventJpaEntity saved = outboxRepository.save(event);
        log.info("Sprint 11 Outbox Event được lưu vào DB [id: {}, type: {}, aggregateId: {}]", saved.getId(), eventType, aggregateId);
        return saved;
    }

    /**
     * Lấy 50 sự kiện Outbox mới nhất cho giao diện giám sát Real-time.
     *
     * @return Danh sách {@link OutboxEventJpaEntity}
     */
    @Transactional(readOnly = true)
    public List<OutboxEventJpaEntity> getRecentEvents() {
        return outboxRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /**
     * Thử lại thủ công một sự kiện bị FAILED.
     *
     * @param id ID sự kiện outbox
     * @return {@link OutboxEventJpaEntity}
     */
    @Transactional
    public OutboxEventJpaEntity retryFailedEvent(UUID id) {
        OutboxEventJpaEntity event = outboxRepository.findById(id)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sự kiện outbox id: " + id));

        event.resetForRetry();
        return outboxRepository.save(event);
    }
}
