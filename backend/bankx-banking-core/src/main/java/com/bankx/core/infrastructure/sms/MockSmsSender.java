package com.bankx.core.infrastructure.sms;

import com.bankx.common.util.MaskingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Giả lập dịch vụ gửi tin nhắn SMS (Mock SMS Sender) cho môi trường phát triển & kiểm thử.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Khai báo làm Spring Bean để tự động inject vào {@code OtpApplicationService}.</li>
 * </ul>
 * </p>
 *
 * <p><b>An toàn thông tin:</b> Tuân thủ quy tắc che giấu số điện thoại bằng {@link MaskingUtils#maskPhone(String)} khi ghi log.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void sendSms(String phone, String message) {
        String maskedPhone = MaskingUtils.maskPhone(phone);
        log.info("[MOCK SMS PROVIDER] Gửi tin nhắn SMS thành công tới sđt: [{}]. Độ dài nội dung: {} ký tự",
                maskedPhone, message != null ? message.length() : 0);
    }
}
