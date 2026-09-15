package com.bankx.common.exception;

/**
 * Lớp ngoại lệ gốc (Base Runtime Exception) cho toàn bộ các ngoại lệ nghiệp vụ trong BankX.
 *
 * <p>Mọi ngoại lệ nghiệp vụ cụ thể (như tài khoản không đủ tiền, hết hạn OTP, sai mật khẩu...)
 * đều phải mở rộng (extend) từ lớp này để {@link GlobalExceptionHandler} có thể bắt và đóng gói
 * trả về {@link com.bankx.common.dto.ApiErrorResponse} một cách tự động.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class BankingException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Khởi tạo ngoại lệ BankingException với đối tượng {@link ErrorCode}.
     *
     * @param errorCode Mã lỗi định nghĩa sẵn
     */
    public BankingException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo ngoại lệ BankingException với đối tượng {@link ErrorCode} và thông điệp ghi đè.
     *
     * @param errorCode Mã lỗi định nghĩa sẵn
     * @param customMessage Thông điệp ghi đè chi tiết hơn
     */
    public BankingException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * Lấy mã lỗi nghiệp vụ tương ứng.
     *
     * @return Đối tượng {@link ErrorCode}
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
