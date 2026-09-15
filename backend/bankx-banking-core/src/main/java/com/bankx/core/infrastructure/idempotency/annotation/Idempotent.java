package com.bankx.core.infrastructure.idempotency.annotation;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu Endpoint / Method cần bảo vệ bởi cơ chế Chống Giao Dịch Trùng Lặp (Idempotency).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Target(ElementType.METHOD)}: Chỉ áp dụng trên mức phương thức Controller/Service.</li>
 *   <li>{@code @Retention(RetentionPolicy.RUNTIME)}: Cho phép AOP Aspect đọc thông tin annotation tại thời điểm Runtime.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Tên Header chứa Idempotency Key. Mặc định là {@code X-Idempotency-Key}.
     */
    String headerName() default "X-Idempotency-Key";

    /**
     * Thời gian sống (TTL) của Idempotency Key trong Redis (tính theo giây). Mặc định 600 giây (10 phút).
     */
    long ttlSeconds() default 600;

    /**
     * Thông điệp trả về khi phát hiện giao dịch trùng lặp đang được xử lý đồng thời.
     */
    String message() default "Yêu cầu giao dịch trùng lặp đang được hệ thống xử lý, vui lòng không bấm nhiều lần!";
}
