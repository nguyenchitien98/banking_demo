package com.bankx.common.exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy thực thể dữ liệu trong cơ sở dữ liệu.
 *
 * <p>Ví dụ: Không tìm thấy Customer, User, BankAccount với ID hoặc thông tin tra cứu truyền vào.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class EntityNotFoundException extends BankingException {

    /**
     * Khởi tạo ngoại lệ EntityNotFoundException với thông điệp mặc định của mã lỗi {@link ErrorCode#RESOURCE_NOT_FOUND}.
     */
    public EntityNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * Khởi tạo ngoại lệ EntityNotFoundException với thông điệp chi tiết mô tả rõ tài nguyên nào không tìm thấy.
     *
     * @param message Thông điệp chi tiết tiếng Việt
     */
    public EntityNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
