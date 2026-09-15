package com.bankx.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi nghiệp vụ chuẩn (Banking Error Codes) của hệ thống BankX.
 *
 * <p>Mỗi mã lỗi bao gồm:
 * <ul>
 *   <li><b>code</b>: Mã số duy nhất phục vụ phân loại và tra cứu ở client.</li>
 *   <li><b>message</b>: Thông điệp mô tả lỗi chuẩn bằng tiếng Việt.</li>
 *   <li><b>httpStatus</b>: Trạng thái HTTP tương ứng được trả về trong Response Header.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public enum ErrorCode {

    // Common Lỗi hệ thống & Validation (1000 - 1099)
    SUCCESS(0, "Thao tác thành công", HttpStatus.OK),
    INTERNAL_SERVER_ERROR(1000, "Lỗi hệ thống nội bộ, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST_PARAMETER(1001, "Tham số dữ liệu truyền vào không hợp lệ", HttpStatus.BAD_REQUEST),
    METHOD_NOT_SUPPORTED(1002, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    UNAUTHORIZED(1003, "Phiên làm việc hết hạn hoặc không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1004, "Tài khoản không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1005, "Không tìm thấy dữ liệu yêu cầu", HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS(1006, "Hệ thống đang quá tải, vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),

    // Auth & User (2000 - 2099)
    USER_NOT_FOUND(2001, "Người dùng không tồn tại trên hệ thống", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(2002, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(2003, "Tài khoản bị khóa do đăng nhập sai quá số lần cho phép", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN(2004, "Refresh token không hợp lệ hoặc đã bị vô hiệu hóa", HttpStatus.UNAUTHORIZED),
    OTP_INVALID_OR_EXPIRED(2005, "Mã OTP không chính xác hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS_EXCEEDED(2006, "Nhập sai OTP quá 3 lần, thao tác đã bị khóa tạm thời", HttpStatus.TOO_MANY_REQUESTS),

    // Banking Account (3000 - 3099)
    ACCOUNT_NOT_FOUND(3001, "Tài khoản thanh toán không tồn tại", HttpStatus.NOT_FOUND),
    ACCOUNT_FROZEN(3002, "Tài khoản hiện đang bị phong tỏa, không thể giao dịch", HttpStatus.BAD_REQUEST),
    ACCOUNT_CLOSED(3003, "Tài khoản đã đóng", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(3004, "Số dư tài khoản không đủ để thực hiện giao dịch", HttpStatus.BAD_REQUEST),
    TRANSFER_LIMIT_EXCEEDED(3005, "Giao dịch vượt quá hạn mức chuyển tiền trong ngày", HttpStatus.BAD_REQUEST),

    // Transfer & Transaction (4000 - 4099)
    DUPLICATE_TRANSACTION(4001, "Yêu cầu giao dịch trùng lặp (Idempotency check failed)", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT(4002, "Dữ liệu vừa bị thay đổi bởi giao dịch khác, vui lòng thử lại", HttpStatus.CONFLICT),
    TRANSFER_FAILED(4003, "Giao dịch chuyển tiền thất bại", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    /**
     * Khởi tạo một ErrorCode với các thuộc tính đi kèm.
     *
     * @param code Mã lỗi số nguyên
     * @param message Thông điệp hiển thị bằng tiếng Việt
     * @param httpStatus Mã trạng thái HTTP
     */
    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Lấy mã số lỗi nghiệp vụ.
     *
     * @return Mã lỗi integer
     */
    public int getCode() {
        return code;
    }

    /**
     * Lấy thông điệp lỗi tiếng Việt.
     *
     * @return Thông điệp lỗi
     */
    public String getMessage() {
        return message;
    }

    /**
     * Lấy trạng thái HTTP tương ứng.
     *
     * @return {@link HttpStatus}
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
