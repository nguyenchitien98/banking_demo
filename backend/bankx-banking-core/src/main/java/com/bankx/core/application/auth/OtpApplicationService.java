package com.bankx.core.application.auth;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.common.util.MaskingUtils;
import com.bankx.core.infrastructure.sms.SmsSender;
import com.bankx.core.presentation.auth.dto.SendOtpRequest;
import com.bankx.core.presentation.auth.dto.VerifyOtpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Application Service quản lý quy trình tạo, lưu trữ và xác thực mã OTP qua Redis.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Khai báo Spring Service Bean chứa các luồng xử lý nghiệp vụ OTP.</li>
 * </ul>
 * </p>
 *
 * <p><b>Các quy tắc an toàn OTP:</b>
 * <ul>
 *   <li>Thời gian sống (TTL) của OTP trong Redis: Chính xác 120 giây.</li>
 *   <li>Đếm số lần nhập sai (Attempt Counter): Tối đa 3 lần sai. Nếu quá 3 lần, khóa thao tác 5 phút (300 giây).</li>
 *   <li>Thu hồi OTP: Xóa ngay mã OTP khỏi Redis khi xác thực thành công để chống Replay Attack.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class OtpApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OtpApplicationService.class);
    private static final SecureRandom random = new SecureRandom();

    public static final long OTP_TTL_SECONDS = 120;
    public static final long ATTEMPT_BLOCK_SECONDS = 300;
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;

    public OtpApplicationService(StringRedisTemplate redisTemplate, SmsSender smsSender) {
        this.redisTemplate = redisTemplate;
        this.smsSender = smsSender;
    }

    /**
     * Tạo và gửi mã OTP 6 chữ số qua SMS.
     *
     * @param request {@link SendOtpRequest}
     * @return Chuỗi thông báo thành công
     */
    public String sendOtp(SendOtpRequest request) {
        String phone = request.phone();
        String purpose = request.purpose();
        String attemptsKey = buildAttemptsKey(purpose, phone);

        // 1. Kiểm tra xem người dùng có đang bị tạm khóa do nhập sai quá 3 lần không
        String currentAttemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        if (currentAttemptsStr != null && Integer.parseInt(currentAttemptsStr) >= MAX_FAILED_ATTEMPTS) {
            log.warn("Yêu cầu OTP bị từ chối: Số điện thoại [{}] bị khóa 5 phút do nhập sai quá 3 lần", MaskingUtils.maskPhone(phone));
            throw new BankingException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        // 2. Sinh mã OTP 6 chữ số ngẫu nhiên an toàn (100000 -> 999999)
        String otpCode = String.valueOf(100000 + random.nextInt(900000));
        String otpKey = buildOtpKey(purpose, phone);

        // 3. Lưu OTP vào Redis với TTL 120s
        redisTemplate.opsForValue().set(otpKey, otpCode, Duration.ofSeconds(OTP_TTL_SECONDS));
        log.info("Đã lưu OTP vào Redis cho sđt [{}] mục đích [{}], TTL 120s", MaskingUtils.maskPhone(phone), purpose);

        // 4. Gửi SMS (Mock SMS Provider)
        String smsContent = String.format("[BankX] Ma OTP xac thuc giao dich %s cua ban la: %s. Ma co hieu luc trong 2 phut. Khong chia se ma nay cho bat ky ai.", purpose, otpCode);
        smsSender.sendSms(phone, smsContent);

        return "Mã OTP đã được gửi tới số điện thoại " + MaskingUtils.maskPhone(phone);
    }

    /**
     * Xác thực mã OTP người dùng nhập.
     *
     * @param request {@link VerifyOtpRequest}
     * @return {@code true} nếu OTP hợp lệ
     */
    public boolean verifyOtp(VerifyOtpRequest request) {
        String phone = request.phone();
        String purpose = request.purpose();
        String otpKey = buildOtpKey(purpose, phone);
        String attemptsKey = buildAttemptsKey(purpose, phone);

        // 1. Kiểm tra xem bị khóa chưa
        String currentAttemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = currentAttemptsStr != null ? Integer.parseInt(currentAttemptsStr) : 0;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            throw new BankingException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        // 2. Lấy OTP từ Redis
        String savedOtp = redisTemplate.opsForValue().get(otpKey);

        if (savedOtp == null) {
            log.warn("Xác thực OTP thất bại: Mã OTP đã hết hạn hoặc không tồn tại cho sđt [{}]", MaskingUtils.maskPhone(phone));
            throw new BankingException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        // 3. So sánh mã OTP
        if (!savedOtp.equals(request.otpCode())) {
            attempts++;
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofSeconds(ATTEMPT_BLOCK_SECONDS));
            log.warn("Xác thực OTP thất bại: Sai mã OTP cho sđt [{}]. Số lần sai: {}/{}", MaskingUtils.maskPhone(phone), attempts, MAX_FAILED_ATTEMPTS);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                throw new BankingException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
            }
            throw new BankingException(ErrorCode.OTP_INVALID_OR_EXPIRED, "Mã OTP không chính xác. Bạn còn " + (MAX_FAILED_ATTEMPTS - attempts) + " lần thử.");
        }

        // 4. OTP chính xác -> Xóa OTP và reset đếm lỗi trong Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);
        log.info("Xác thực OTP THÀNH CÔNG cho sđt [{}] mục đích [{}]", MaskingUtils.maskPhone(phone), purpose);
        return true;
    }

    private String buildOtpKey(String purpose, String phone) {
        return "otp:" + purpose.toLowerCase() + ":" + phone;
    }

    private String buildAttemptsKey(String purpose, String phone) {
        return "otp_attempts:" + purpose.toLowerCase() + ":" + phone;
    }
}
