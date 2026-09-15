package com.bankx.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Đóng gói phản hồi lỗi API chuẩn (API Error Response Envelope) cho hệ thống BankX.
 *
 * <p>Được sử dụng bởi {@code GlobalExceptionHandler} để chuyển đổi tất cả ngoại lệ (Exceptions)
 * thành định dạng phản hồi lỗi thống nhất, giúp client dễ dàng bắt lỗi và hiển thị thông báo
 * phù hợp cho người dùng.</p>
 *
 * @param code Mã lỗi nghiệp vụ duy nhất (ví dụ: 1001, 2004, 4001...)
 * @param message Thông báo lỗi chính bằng tiếng Việt dễ hiểu
 * @param errors Danh sách mô tả chi tiết các lỗi (như validation fields lỗi)
 * @param timestamp Thời điểm xảy ra lỗi (ISO-8601 UTC)
 * @param traceId Mã định danh phân vết request (Correlation ID) để tra cứu log khi sự cố xảy ra
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record ApiErrorResponse(
        int code,
        String message,
        List<String> errors,
        Instant timestamp,
        String traceId
) {
    /**
     * Tạo đối tượng phản hồi lỗi chỉ có mã lỗi và thông điệp ngắn.
     *
     * @param code Mã lỗi nghiệp vụ
     * @param message Thông báo lỗi bằng tiếng Việt
     * @return Đối tượng {@link ApiErrorResponse}
     */
    public static ApiErrorResponse of(int code, String message) {
        return new ApiErrorResponse(
                code,
                message,
                List.of(),
                Instant.now(),
                null
        );
    }

    /**
     * Tạo đối tượng phản hồi lỗi có đầy đủ mã lỗi, thông điệp, danh sách chi tiết lỗi và traceId.
     *
     * @param code Mã lỗi nghiệp vụ
     * @param message Thông báo lỗi chính bằng tiếng Việt
     * @param errors Danh sách chi tiết các lỗi (field validation errors)
     * @param traceId Correlation ID để tra cứu log
     * @return Đối tượng {@link ApiErrorResponse} đầy đủ
     */
    public static ApiErrorResponse of(int code, String message, List<String> errors, String traceId) {
        return new ApiErrorResponse(
                code,
                message,
                errors != null ? errors : List.of(),
                Instant.now(),
                traceId
        );
    }
}
