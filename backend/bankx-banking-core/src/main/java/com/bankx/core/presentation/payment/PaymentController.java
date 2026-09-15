package com.bankx.core.presentation.payment;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.payment.PaymentApplicationService;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.domain.payment.model.BillInquiryResponse;
import com.bankx.core.infrastructure.idempotency.annotation.Idempotent;
import com.bankx.core.infrastructure.persistence.payment.entity.BillPaymentJpaEntity;
import com.bankx.core.infrastructure.persistence.payment.entity.PaymentProviderJpaEntity;
import com.bankx.core.presentation.payment.dto.PayBillRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller xử lý các yêu cầu Thanh toán Hóa đơn Dịch vụ (Payment REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Tự động hóa quá trình serialize phản hồi sang JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/payments")}: Tiền tố API URL quản lý thanh toán hóa đơn.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentApplicationService paymentService;

    public PaymentController(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint lấy danh sách các Nhà cung cấp dịch vụ thanh toán đang hoạt động.
     *
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link PaymentProviderJpaEntity}
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<PaymentProviderJpaEntity>>> getProviders(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<PaymentProviderJpaEntity> providers = paymentService.getActiveProviders();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhà cung cấp dịch vụ thành công", providers, traceId));
    }

    /**
     * Endpoint tra cứu thông tin hóa đơn và nợ cước từ Nhà cung cấp dịch vụ (Bill Inquiry).
     *
     * @param providerCode Mã nhà cung cấp dịch vụ (ví dụ: "EVN_HN")
     * @param billNumber Mã khách hàng / Mã danh bạ
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link BillInquiryResponse}
     */
    @GetMapping("/bills")
    public ResponseEntity<ApiResponse<BillInquiryResponse>> inquireBill(
            @RequestParam String providerCode,
            @RequestParam String billNumber,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BillInquiryResponse inquiry = paymentService.inquireBill(providerCode, billNumber);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu hóa đơn dịch vụ thành công", inquiry, traceId));
    }

    /**
     * Endpoint thực thi thanh toán hóa đơn dịch vụ (tích hợp Idempotency Key bảo vệ giao dịch).
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param request Payload {@link PayBillRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link BillPaymentJpaEntity}
     */
    @Idempotent(headerName = "X-Idempotency-Key", ttlSeconds = 600, message = "Thanh toán hóa đơn đang được xử lý, vui lòng không gửi lặp lại!")
    @PostMapping("/bills")
    public ResponseEntity<ApiResponse<BillPaymentJpaEntity>> payBill(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PayBillRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        UUID userId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        BillPaymentJpaEntity payment = paymentService.payBill(
                userId,
                request.sourceAccountId(),
                request.providerCode(),
                request.customerBillCode(),
                request.amount()
        );
        return ResponseEntity.ok(ApiResponse.success("Thanh toán hóa đơn dịch vụ thành công!", payment, traceId));
    }

    /**
     * Endpoint lấy lịch sử thanh toán hóa đơn dịch vụ theo ID tài khoản.
     *
     * @param accountId ID tài khoản trích nợ
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link BillPaymentJpaEntity}
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BillPaymentJpaEntity>>> getPaymentHistory(
            @RequestParam UUID accountId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<BillPaymentJpaEntity> history = paymentService.getPaymentHistory(accountId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thanh toán hóa đơn thành công", history, traceId));
    }
}
