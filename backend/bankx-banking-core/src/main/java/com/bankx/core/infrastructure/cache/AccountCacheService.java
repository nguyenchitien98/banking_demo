package com.bankx.core.infrastructure.cache;

import com.bankx.common.util.MaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Service quản lý Caching số dư tài khoản ngân hàng trên Redis (Balance Cache Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Spring Service Bean chịu trách nhiệm caching số dư.</li>
 * </ul>
 * </p>

 * <p><b>Tối ưu hiệu năng:</b>
 * Cache số dư tài khoản trong Redis với TTL 30 giây để giảm tải truy vấn trực tiếp vào PostgreSQL
 * khi người dùng liên tục làm mới (refresh) giao diện trang Dashboard.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class AccountCacheService {

    private static final Logger log = LoggerFactory.getLogger(AccountCacheService.class);
    public static final long BALANCE_CACHE_TTL_SECONDS = 30;

    private final StringRedisTemplate redisTemplate;

    public AccountCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Lấy số dư tài khoản từ Redis Cache nếu còn hiệu lực.
     *
     * @param accountNumber Số tài khoản
     * @return {@link BigDecimal} số dư hoặc {@code null} nếu cache miss
     */
    public BigDecimal getCachedBalance(String accountNumber) {
        String key = buildBalanceKey(accountNumber);
        String cachedVal = redisTemplate.opsForValue().get(key);
        if (cachedVal != null) {
            log.debug("HIT Redis Balance Cache cho STK [{}]: {} VND", MaskingUtils.maskAccountNumber(accountNumber), cachedVal);
            return new BigDecimal(cachedVal);
        }
        return null;
    }

    /**
     * Cập nhật số dư tài khoản vào Redis Cache với TTL 30 giây.
     *
     * @param accountNumber Số tài khoản
     * @param balance Số dư mới
     */
    public void cacheBalance(String accountNumber, BigDecimal balance) {
        String key = buildBalanceKey(accountNumber);
        redisTemplate.opsForValue().set(key, balance.toPlainString(), Duration.ofSeconds(BALANCE_CACHE_TTL_SECONDS));
        log.debug("Cập nhật Redis Balance Cache cho STK [{}] số dư: {} VND (TTL 30s)", MaskingUtils.maskAccountNumber(accountNumber), balance);
    }

    /**
     * Xóa cache số dư của tài khoản khi có giao dịch biến động số dư.
     *
     * @param accountNumber Số tài khoản
     */
    public void evictBalanceCache(String accountNumber) {
        String key = buildBalanceKey(accountNumber);
        redisTemplate.delete(key);
        log.debug("Xóa Redis Balance Cache cho STK [{}] do biến động số dư", MaskingUtils.maskAccountNumber(accountNumber));
    }

    private String buildBalanceKey(String accountNumber) {
        return "account_balance:" + accountNumber;
    }
}
