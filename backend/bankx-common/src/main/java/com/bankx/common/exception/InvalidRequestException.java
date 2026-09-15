package com.bankx.common.exception;

/**
 * Ngoại lệ ném ra khi yêu cầu từ client không hợp lệ về mặt tham số hoặc logic điều kiện.
 *
 * <p>Ví dụ: Chuyển tiền tới chính tài khoản của mình, số tiền âm, sai định dạng tài khoản...</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class InvalidRequestException extends BankingException {

    /**
     * Khởi tạo ngoại lệ với mã lỗi mặc định INVALID_REQUEST_PARAMETER.
     */
    public InvalidRequestException() {
        super(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    /**
     * Khởi tạo ngoại lệ với thông điệp tùy chỉnh giải thích nguyên nhân không hợp lệ.
     *
     * @param message Thông điệp tiếng Việt
     */
    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST_PARAMETER, message);
    }
}
