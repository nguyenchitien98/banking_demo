package com.bankx.core.infrastructure.idempotency.aspect;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.infrastructure.idempotency.annotation.Idempotent;
import com.bankx.core.infrastructure.idempotency.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * AOP Aspect can thiệp và bảo vệ các Endpoint được đánh dấu annotation {@link Idempotent}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Aspect}: Khai báo Aspect xử lý cross-cutting concern (Chống trùng lặp giao dịch).</li>
 *   <li>{@code @Component}: Đăng ký Aspect làm Spring Bean trong IoC Container.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private final IdempotencyService idempotencyService;

    public IdempotencyAspect(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Bắt các phương thức có đánh dấu {@link Idempotent} và xử lý Idempotency Key.
     *
     * @param joinPoint Joint point đại diện cho phương thức được gọi
     * @param idempotent Annotation {@link Idempotent}
     * @return Kết quả phản hồi của phương thức (hoặc kết quả cached từ Redis)
     * @throws Throwable Ngoại lệ xảy ra trong quá trình thực thi
     */
    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        // Nếu client không truyền Idempotency Key -> Cho phép thực thi bình thường
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("[Idempotency] Request to {} missing header '{}'. Proceeding without idempotency protection.",
                    request.getRequestURI(), idempotent.headerName());
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();

        // 1. Kiểm tra xem kết quả của Idempotency Key này đã được cache trong Redis hay chưa
        Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, returnType);
        if (cachedResponse != null) {
            log.info("[Idempotency] Returning cached response for duplicate request key: {}", idempotencyKey);
            return cachedResponse;
        }

        // 2. Thử lấy khóa LOCK từ Redis (SETNX)
        boolean lockAcquired = idempotencyService.tryAcquireLock(idempotencyKey, idempotent.ttlSeconds());
        if (!lockAcquired) {
            log.warn("[Idempotency] Concurrent duplicate request detected for key: {}", idempotencyKey);
            throw new BankingException(ErrorCode.DUPLICATE_TRANSACTION, idempotent.message());
        }

        try {
            // 3. Thực thi nghiệp vụ chuyển tiền chính
            Object result = joinPoint.proceed();

            // 4. Nếu thực thi thành công, cache kết quả phản hồi vào Redis
            idempotencyService.cacheResponse(idempotencyKey, result, idempotent.ttlSeconds());
            return result;
        } catch (Throwable ex) {
            // 5. Nếu xảy ra ngoại lệ, giải phóng khóa lock để client có thể thử lại
            idempotencyService.releaseLock(idempotencyKey);
            throw ex;
        }
    }
}
