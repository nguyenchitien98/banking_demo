package com.bankx.core.infrastructure.sms;

/**
 * Interface dịch vụ gửi tin nhắn SMS thông báo và OTP.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface SmsSender {

    /**
     * Gửi tin nhắn SMS tới số điện thoại người dùng.
     *
     * @param phone Số điện thoại người nhận
     * @param message Nội dung tin nhắn SMS
     */
    void sendSms(String phone, String message);
}
