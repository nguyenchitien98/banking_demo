package com.bankx.core.presentation.transfer;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.transfer.TransferApplicationService;
import com.bankx.core.domain.account.model.BankAccount;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.domain.transfer.model.BankTransfer;
import com.bankx.core.presentation.transfer.dto.CreateTransferRequest;
import com.bankx.core.presentation.transfer.dto.RecipientInquiryResponse;
import com.bankx.core.presentation.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller xử lý các yêu cầu Chuyển tiền Ngân hàng (Bank Transfer REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Tự động hóa quá trình serialize phản hồi sang JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/transfers")}: Tiền tố API URL quản lý giao dịch chuyển tiền.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferApplicationService transferService;

    public TransferController(TransferApplicationService transferService) {
        this.transferService = transferService;
    }

    /**
     * Endpoint truy vấn thông tin người thụ hưởng theo số tài khoản (Recipient Inquiry).
     *
     * @param accountNumber Số tài khoản thụ hưởng
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link RecipientInquiryResponse}
     */
    @GetMapping("/recipient-inquiry")
    public ResponseEntity<ApiResponse<RecipientInquiryResponse>> inquireRecipient(
            @RequestParam String accountNumber,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BankAccount targetAccount = transferService.inquireRecipient(accountNumber);
        RecipientInquiryResponse response = new RecipientInquiryResponse(
                targetAccount.getId(),
                targetAccount.getAccountNumber(),
                targetAccount.getAccountName(),
                targetAccount.getStatus()
        );

        return ResponseEntity.ok(ApiResponse.success("Truy vấn tài khoản thụ hưởng thành công", response, traceId));
    }

    /**
     * Endpoint thực thi giao dịch chuyển tiền nội bộ.
     *
     * @param currentUser Người dùng xác thực hiện tại
     * @param request Payload {@link CreateTransferRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link TransferResponse}
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<TransferResponse>> createInternalTransfer(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BankTransfer transfer = transferService.createInternalTransfer(
                currentUser.getId(),
                request.sourceAccountId(),
                request.targetAccountNumber(),
                request.amount(),
                request.description()
        );
        TransferResponse response = mapToResponse(transfer);

        return ResponseEntity.ok(ApiResponse.success("Chuyển tiền nội bộ thành công", response, traceId));
    }

    /**
     * Endpoint lấy chi tiết một lệnh chuyển tiền.
     *
     * @param id ID lệnh chuyển tiền
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link TransferResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferDetail(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        BankTransfer transfer = transferService.getTransferDetail(id);
        TransferResponse response = mapToResponse(transfer);

        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết lệnh chuyển tiền thành công", response, traceId));
    }

    /**
     * Endpoint lấy lịch sử chuyển tiền theo ID tài khoản.
     *
     * @param accountId ID tài khoản
     * @param limit Số lượng bản ghi tối đa
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link TransferResponse}
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getAccountTransfers(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<BankTransfer> transfers = transferService.getAccountTransfers(accountId, limit);
        List<TransferResponse> response = transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử chuyển tiền thành công", response, traceId));
    }

    private TransferResponse mapToResponse(BankTransfer t) {
        return new TransferResponse(
                t.getId(),
                t.getTransferCode(),
                t.getSourceAccountId(),
                t.getTargetAccountId(),
                t.getTargetAccountNumber(),
                t.getTargetAccountName(),
                t.getAmount().getAmount(),
                t.getAmount().getCurrency(),
                t.getFee().getAmount(),
                t.getDescription(),
                t.getTransferType().name(),
                t.getStatus().name(),
                t.getTransactionId(),
                t.getCreatedAt()
        );
    }
}
