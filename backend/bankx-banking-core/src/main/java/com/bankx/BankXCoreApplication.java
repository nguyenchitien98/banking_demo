package com.bankx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi động của BankX Banking Core — Modular Monolith Phase 1.
 *
 * <p>Đây là service trọng tâm chứa toàn bộ nghiệp vụ ngân hàng trong 1 ứng dụng Spring Boot.
 * Theo kiến trúc Modular Monolith: code được tách rõ theo module/package nhưng chạy cùng process.
 *
 * <p><b>Tại sao Modular Monolith thay vì Microservices ngay?</b>
 * Xem ADR-001 tại banking/docs/adr/ADR-001-003_Architecture_Decisions.md
 *
 * <p><b>Tại sao @EnableJpaAuditing?</b>
 * Tự động điền `createdAt`, `updatedAt`, `createdBy` vào mọi JPA Entity có
 * @EntityListeners(AuditingEntityListener.class). Giảm boilerplate, đảm bảo
 * audit trail nhất quán.
 *
 * <p><b>Tại sao @EnableKafka?</b>
 * Kích hoạt Kafka consumer annotations (@KafkaListener). Cần thiết cho
 * Notification Module consume events từ Outbox Pattern.
 *
 * <p><b>Tại sao @EnableScheduling?</b>
 * Kích hoạt @Scheduled annotations. Cần thiết cho OutboxPollingService
 * chạy định kỳ mỗi 1 giây để publish Kafka events từ outbox_events table.
 *
 * @since Sprint 00
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
@EnableKafka
@EnableScheduling
public class BankXCoreApplication {

    /**
     * Entry point của BankX Banking Core.
     *
     * @param args tham số dòng lệnh (ví dụ: --spring.profiles.active=local)
     */
    public static void main(String[] args) {
        SpringApplication.run(BankXCoreApplication.class, args);
    }
}
