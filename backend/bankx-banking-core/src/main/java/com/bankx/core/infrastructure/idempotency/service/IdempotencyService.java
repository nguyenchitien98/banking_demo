package com.bankx.core.infrastructure.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service quản lý trạng thái Idempotency Key và Caching phản hồi kết quả trên Redis.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đăng ký Service Bean quản lý thao tác Redis cho Idempotency.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String LOCK_PREFIX = "idempotency:lock:";
    private static final String RESPONSE_PREFIX = "idempotency:resp:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Thử lấy khóa Idempotency bằng lệnh Redis SETNX (setIfAbsent).
     *
     * @param key Idempotency Key từ client
     * @param ttlSeconds Thời gian giữ khóa
     * @return {@code true} nếu lấy khóa thành công (lần đầu gọi), {@code false} nếu khóa đã tồn tại
     */
    public boolean tryAcquireLock(String key, long ttlSeconds) {
        String lockKey = LOCK_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Xóa khóa Idempotency nếu giao dịch gặp lỗi để client có thể thử lại.
     *
     * @param key Idempotency Key
     */
    public void releaseLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
        log.info("[Idempotency] Released lock for key: {}", key);
    }

    /**
     * Kiểm tra và lấy phản hồi đã cache từ giao dịch trước đó.
     *
     * @param key Idempotency Key
     * @param targetType Lớp DTO cần deserialize
     * @param <T> Kiểu dữ liệu DTO
     * @return Đối tượng DTO đã cache hoặc {@code null}
     */
    public <T> T getCachedResponse(String key, Class<T> targetType) {
        String respKey = RESPONSE_PREFIX + key;
        String json = redisTemplate.opsForValue().get(respKey);
        if (json == null) {
            return null;
        }

        try {
            log.info("[Idempotency] Cache HIT for key: {}. Returning cached response.", key);
            return objectMapper.readValue(json, targetType);
        } catch (Exception e) {
            log.error("[Idempotency] Failed to deserialize cached response for key: {}", key, e);
            return null;
        }
    }

    /**
     * Lưu phản hồi kết quả giao dịch vào Redis cache.
     *
     * @param key Idempotency Key
     * @param response Payload phản hồi
     * @param ttlSeconds Thời gian sống
     */
    public void cacheResponse(String key, Object response, long ttlSeconds) {
        String respKey = RESPONSE_PREFIX + key;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(respKey, json, Duration.ofSeconds(ttlSeconds));
            log.info("[Idempotency] Successfully cached response for key: {}", key);
        } catch (Exception e) {
            log.error("[Idempotency] Failed to cache response for key: {}", key, e);
        }
    }
}
