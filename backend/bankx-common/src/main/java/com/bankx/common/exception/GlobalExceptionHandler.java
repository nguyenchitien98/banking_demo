package com.bankx.common.exception;

import com.bankx.common.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp xử lý ngoại lệ tập trung (Global Exception Handler) cho toàn bộ REST Controller trong BankX.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestControllerAdvice}: Đánh dấu lớp này là một Spring Component chuyên lắng nghe
 *       và bắt tất cả các ngoại lệ bị ném ra từ các lớp {@code @RestController} trong ứng dụng.
 *       Nó tự động đóng gói giá trị trả về của các handler method thành dữ liệu phản hồi JSON
 *       (tương đương kết hợp với {@code @ResponseBody}).</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bắt và xử lý ngoại lệ nghiệp vụ ngân hàng {@link BankingException}.
     *
     * @param ex Ngoại lệ nghiệp vụ bị ném ra từ Service / Domain layer
     * @return {@link ResponseEntity} chứa {@link ApiErrorResponse} phù hợp với HTTP status của lỗi
     */
    @ExceptionHandler(BankingException.class)
    public ResponseEntity<ApiErrorResponse> handleBankingException(BankingException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Ngoại lệ nghiệp vụ BankX [code={}]: {}", errorCode.getCode(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode.getCode(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * Bắt và xử lý ngoại lệ validation DTO khi sử dụng {@code @Valid} trên tham số Controller.
     *
     * @param ex Ngoại lệ validation bị ném ra do vi phạm ràng buộc DTO
     * @return {@link ResponseEntity} chứa danh sách chi tiết các trường bị lỗi với HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }

        log.warn("Lỗi validation dữ liệu đầu vào: {}", errors);

        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.INVALID_REQUEST_PARAMETER.getCode(),
                ErrorCode.INVALID_REQUEST_PARAMETER.getMessage(),
                errors,
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Bắt và xử lý tất cả các ngoại lệ hệ thống chưa được dự trù trước (Unhandled Exceptions).
     *
     * @param ex Ngoại lệ hệ thống không mong muốn
     * @return {@link ResponseEntity} chứa thông báo lỗi hệ thống chung (tránh rò rỉ stack trace sensitive cho client)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống chưa được phân loại: ", ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
