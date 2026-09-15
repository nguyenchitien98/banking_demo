package com.bankx.core.presentation.notification;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.notification.NotificationApplicationService;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller xử lý các yêu cầu Quản lý Thông báo In-App (Notification REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Tự động hóa quá trình serialize phản hồi sang JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/notifications")}: Tiền tố API URL quản lý thông báo.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationService;

    public NotificationController(NotificationApplicationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Endpoint lấy danh sách tất cả thông báo của người dùng hiện tại.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link NotificationJpaEntity}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationJpaEntity>>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        UUID customerId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        List<NotificationJpaEntity> list = notificationService.getCustomerNotifications(customerId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", list, traceId));
    }

    /**
     * Endpoint đếm số lượng thông báo chưa đọc.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa số lượng unreadCount
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        UUID customerId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        long unreadCount = notificationService.getUnreadCount(customerId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", Map.of("unreadCount", unreadCount), traceId));
    }

    /**
     * Endpoint đánh dấu 1 thông báo là đã đọc.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param id ID thông báo
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link NotificationJpaEntity}
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationJpaEntity>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        UUID customerId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        NotificationJpaEntity updated = notificationService.markAsRead(customerId, id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thông báo thành công", updated, traceId));
    }

    /**
     * Endpoint đánh dấu TẤT CẢ thông báo là đã đọc.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity}
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        UUID customerId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc", null, traceId));
    }
}
