package com.bankx.common.util;

/**
 * Lớp tiện ích thực hiện che giấu thông tin nhạy cảm (Data Masking) cho dữ liệu ngân hàng.
 *
 * <p>Tuân thủ nghiêm ngặt Banking Security Rules: Tuyệt đối không ghi log số tài khoản đầy đủ,
 * số thẻ credit/debit, số điện thoại hay email cá nhân để đảm bảo an toàn thông tin theo tiêu chuẩn PCI-DSS.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public final class MaskingUtils {

    private MaskingUtils() {
        // Class tiện ích không khởi tạo instance
    }

    /**
     * Che giấu số tài khoản ngân hàng (Giữ lại 3 số đầu và 3 số cuối, ở giữa thay bằng ***).
     * <p>Ví dụ: {@code "000123456789" -> "000***789"}</p>
     *
     * @param accountNumber Số tài khoản nguyên bản
     * @return Số tài khoản đã được che giấu
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 6) {
            return "***";
        }
        int len = accountNumber.length();
        return accountNumber.substring(0, 3) + "***" + accountNumber.substring(len - 3);
    }

    /**
     * Che giấu số điện thoại (Giữ lại 3 số đầu và 2 số cuối).
     * <p>Ví dụ: {@code "0912345678" -> "091***78"}</p>
     *
     * @param phone Số điện thoại nguyên bản
     * @return Số điện thoại đã được che giấu
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 5) {
            return "***";
        }
        int len = phone.length();
        return phone.substring(0, 3) + "***" + phone.substring(len - 2);
    }

    /**
     * Che giấu email (Giữ ký tự đầu của username và domain).
     * <p>Ví dụ: {@code "nguyen.van.a@gmail.com" -> "n***a@gmail.com"}</p>
     *
     * @param email Địa chỉ email nguyên bản
     * @return Email đã được che giấu
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 2) {
            return "*@" + parts[1];
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + parts[1];
    }

    /**
     * Che giấu số thẻ ngân hàng (PAN Masking theo PCI-DSS: Giữ 6 số đầu BIN và 4 số cuối).
     * <p>Ví dụ: {@code "4123456789012345" -> "412345******2345"}</p>
     *
     * @param cardNumber Số thẻ đầy đủ 16 số
     * @return Số thẻ đã được che giấu chuẩn PCI-DSS
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 12) {
            return "****";
        }
        int len = cardNumber.length();
        return cardNumber.substring(0, 6) + "******" + cardNumber.substring(len - 4);
    }
}
