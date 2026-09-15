package com.bankx.core.presentation.ledger;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.ledger.LedgerApplicationService;
import com.bankx.core.domain.ledger.model.LedgerEntry;
import com.bankx.core.domain.ledger.model.Transaction;
import com.bankx.core.presentation.ledger.dto.LedgerEntryResponse;
import com.bankx.core.presentation.ledger.dto.RecordDoubleEntryRequest;
import com.bankx.core.presentation.ledger.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller xử lý các yêu cầu truy vấn và hạch toán Bút toán Ghi sổ kép (Ledger REST API).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @RestController}: Tự động hóa quá trình serialize phản hồi sang JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1")}: Tiền tố API URL tiêu chuẩn của hệ thống.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1")
public class LedgerController {

    private final LedgerApplicationService ledgerService;

    public LedgerController(LedgerApplicationService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * Lấy lịch sử bút toán giao dịch ghi sổ của một tài khoản ngân hàng.
     *
     * @param id ID tài khoản ngân hàng
     * @param limit Số lượng bản ghi tối đa (mặc định 20)
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa danh sách {@link LedgerEntryResponse}
     */
    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getAccountTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        List<LedgerEntry> entries = ledgerService.getAccountEntries(id, limit);
        List<LedgerEntryResponse> response = entries.stream()
                .map(this::mapEntryToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử bút toán thành công", response, traceId));
    }

    /**
     * Lấy thông tin chi tiết một giao dịch và các bút toán ghi sổ kép đính kèm.
     *
     * @param id ID giao dịch
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link TransactionResponse}
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionDetail(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        Transaction transaction = ledgerService.getTransactionDetail(id);
        TransactionResponse response = mapTransactionToResponse(transaction);

        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết giao dịch thành công", response, traceId));
    }

    /**
     * Endpoint thử nghiệm hạch toán bút toán ghi sổ kép (Double-Entry Bookkeeping).
     *
     * @param request Payload {@link RecordDoubleEntryRequest}
     * @param traceId Header {@code X-Trace-Id}
     * @return {@link ResponseEntity} chứa {@link TransactionResponse}
     */
    @PostMapping("/ledger/record")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordDoubleEntry(
            @Valid @RequestBody RecordDoubleEntryRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
    ) {
        Transaction transaction = ledgerService.recordDoubleEntry(
                request.debitAccountId(),
                request.creditAccountId(),
                request.amount(),
                request.description(),
                request.type()
        );
        TransactionResponse response = mapTransactionToResponse(transaction);

        return ResponseEntity.ok(ApiResponse.success("Hạch toán bút toán ghi sổ kép thành công", response, traceId));
    }

    private TransactionResponse mapTransactionToResponse(Transaction tx) {
        List<LedgerEntryResponse> entryResponses = tx.getEntries().stream()
                .map(this::mapEntryToResponse)
                .collect(Collectors.toList());

        return new TransactionResponse(
                tx.getId(),
                tx.getTransactionReference(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getAmount().getAmount(),
                tx.getAmount().getCurrency(),
                tx.getDescription(),
                tx.getCreatedAt(),
                entryResponses
        );
    }

    private LedgerEntryResponse mapEntryToResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.id(),
                entry.transactionId(),
                entry.accountId(),
                entry.entryType().name(),
                entry.amount().getAmount(),
                entry.amount().getCurrency(),
                entry.balanceAfter().getAmount(),
                entry.createdAt()
        );
    }
}
