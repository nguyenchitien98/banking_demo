package com.bankx.common.dto;

import java.time.Instant;

/**
 * Đóng gói phản hồi API chuẩn (API Response Envelope) cho toàn bộ hệ thống BankX.
 *
 * <p>Mọi REST Controller trong hệ thống đều phải trả về đối tượng này thay vì trả về
 * dữ liệu trực tiếp hoặc JPA Entity, nhằm đảm bảo tính nhất quán về cấu trúc dữ liệu
 * phản hồi cho client (Angular Web / Mobile / Third-party).</p>
 *
 * @param <T> Kiểu dữ liệu của phần thân (payload) phản hồi
 * @param code Mã phản hồi nghiệp vụ (0 đại diện cho SUCCESS, các mã khác 0 là mã lỗi)
 * @param message Thông điệp mô tả kết quả xử lý bằng tiếng Việt
 * @param data Dữ liệu phản hồi thực sự (payload)
 * @param timestamp Thời điểm tạo phản hồi (ISO-8601 UTC)
 * @param traceId Mã định danh phân vết request (Correlation ID) truyền từ Gateway
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        Instant timestamp,
        String traceId
) {
    /**
     * Tạo phản hồi thành công chứa dữ liệu payload.
     *
     * @param <T> Kiểu dữ liệu payload
     * @param data Dữ liệu cần phản hồi cho client
     * @return Đối tượng {@link ApiResponse} chứa data thành công với code = 0
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                0,
                "Thao tác thành công",
                data,
                Instant.now(),
                null
        );
    }

    /**
     * Tạo phản hồi thành công kèm thông điệp tùy chỉnh và data payload.
     *
     * @param <T> Kiểu dữ liệu payload
     * @param message Thông điệp phản hồi tiếng Việt
     * @param data Dữ liệu cần phản hồi cho client
     * @return Đối tượng {@link ApiResponse} chứa data thành công
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                0,
                message,
                data,
                Instant.now(),
                null
        );
    }

    /**
     * Tạo phản hồi thành công kèm thông điệp và traceId phân vết.
     *
     * @param <T> Kiểu dữ liệu payload
     * @param message Thông điệp phản hồi
     * @param data Dữ liệu payload
     * @param traceId Correlation ID từ Gateway
     * @return Đối tượng {@link ApiResponse} chứa thông tin phản hồi đầy đủ
     */
    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(
                0,
                message,
                data,
                Instant.now(),
                traceId
        );
    }
}
