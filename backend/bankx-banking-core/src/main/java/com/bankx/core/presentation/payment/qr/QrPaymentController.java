package com.bankx.core.presentation.payment.qr;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.infrastructure.idempotency.annotation.Idempotent;
import com.bankx.core.application.payment.qr.QrPaymentApplicationService;
import com.bankx.core.application.payment.qr.dto.GenerateQrRequest;
import com.bankx.core.application.payment.qr.dto.GenerateQrResponse;
import com.bankx.core.application.payment.qr.dto.ParseQrRequest;
import com.bankx.core.application.payment.qr.dto.ParseQrResponse;
import com.bankx.core.application.payment.qr.dto.PayQrRequest;
import com.bankx.core.application.payment.qr.dto.QrPaymentResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST Controller tiếp nhận các yêu cầu xử lý mã QR (VietQR Standard).
 * 
 * <p>Sử dụng annotation {@link RestController} để định nghĩa các RESTful APIs,
 * {@link RequestMapping} định tuyến tới nhóm URL {@code /api/v1/payments/qr},
 * và {@link Validated} để kiểm tra tính hợp lệ dữ liệu đầu vào.</p>
 */
@RestController
@RequestMapping("/api/v1/payments/qr")
@RequiredArgsConstructor
@Validated
@Slf4j
public class QrPaymentController {

    private final QrPaymentApplicationService qrPaymentApplicationService;

    /**
     * API Phân tích (Parse) dữ liệu chuỗi mã VietQR EMVCo.
     * 
     * @param request DTO chứa chuỗi QR
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin chi tiết mã QR
     */
    @PostMapping("/parse")
    public ApiResponse<ParseQrResponse> parseQr(@Valid @RequestBody ParseQrRequest request) {
        log.info("REST request: Phân tích mã QR VietQR");
        ParseQrResponse response = qrPaymentApplicationService.parseQr(request);
        return ApiResponse.success("Giải mã VietQR thành công", response);
    }

    /**
     * API Tạo mã VietQR (động hoặc tĩnh) cho tài khoản thụ hưởng.
     * 
     * @param request Thông số tài khoản, số tiền, nội dung
     * @return Phản hồi chuẩn {@link ApiResponse} chứa chuỗi VietQR chuẩn EMVCo
     */
    @PostMapping("/generate")
    public ApiResponse<GenerateQrResponse> generateQr(@Valid @RequestBody GenerateQrRequest request) {
        log.info("REST request: Tạo mã QR VietQR cho tài khoản [{}]", request.accountNumber());
        GenerateQrResponse response = qrPaymentApplicationService.generateQr(request);
        return ApiResponse.success("Tạo mã VietQR thành công", response);
    }

    /**
     * API Lấy mã VietQR tĩnh nhận tiền của chính tài khoản người dùng.
     * 
     * @param accountNumber Số tài khoản nhận
     * @param amount Số tiền (tùy chọn)
     * @param description Nội dung (tùy chọn)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa mã QR nhận tiền
     */
    @GetMapping("/my-qr")
    public ApiResponse<GenerateQrResponse> getMyQr(
            @RequestParam String accountNumber,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String description) {
        log.info("REST request: Lấy mã QR nhận tiền cá nhân cho tài khoản [{}]", accountNumber);
        GenerateQrRequest request = new GenerateQrRequest(accountNumber, "970400", amount, description);
        GenerateQrResponse response = qrPaymentApplicationService.generateQr(request);
        return ApiResponse.success("Lấy mã QR nhận tiền thành công", response);
    }

    /**
     * API Thực thi thanh toán/chuyển tiền bằng mã QR VietQR.
     * 
     * <p>Sử dụng custom annotation {@link Idempotent} với header {@code X-Idempotency-Key}
     * nhằm ngăn ngừa giao dịch thanh toán QR trùng lặp.</p>
     * 
     * @param request DTO thực thi thanh toán QR
     * @return Phản hồi chuẩn {@link ApiResponse} chứa kết quả giao dịch
     */
    @PostMapping("/pay")
    @Idempotent(headerName = "X-Idempotency-Key", ttlSeconds = 600, message = "Giao dịch thanh toán QR trùng lặp đã được xử lý")
    public ApiResponse<QrPaymentResult> payQr(@Valid @RequestBody PayQrRequest request) {
        log.info("REST request: Thanh toán QR từ tài khoản [{}]", request.sourceAccountId());
        QrPaymentResult result = qrPaymentApplicationService.payQr(request);
        return ApiResponse.success("Thanh toán mã QR thành công", result);
    }
}
