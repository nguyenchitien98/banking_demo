package com.bankx.core.presentation.cqrs;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.cqrs.TransactionHistoryQueryService;
import com.bankx.core.application.cqrs.dto.CursorPageResponse;
import com.bankx.core.application.cqrs.dto.TransactionHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller cung cấp các endpoint truy vấn Lịch sử Giao dịch theo mô hình CQRS Read Model.
 * 
 * <p>Annotation {@link RestController} khai báo RESTful Controller tiếp nhận request HTTP.
 * Annotation {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/cqrs/history}.
 * Annotation {@link RequiredArgsConstructor} hỗ trợ tiêm dependency qua constructor.
 * Annotation {@link Validated} kích hoạt kiểm tra tính hợp lệ của tham số.
 * Annotation {@link Slf4j} tự động ghi nhật ký hệ thống.</p>
 */
@RestController
@RequestMapping("/api/v1/cqrs/history")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TransactionHistoryQueryController {

    private final TransactionHistoryQueryService queryService;

    /**
     * API Truy vấn lịch sử giao dịch theo số tài khoản với Phân trang theo Con trỏ (Cursor-based Pagination).
     *
     * @param accountNumber Số tài khoản cần tra cứu lịch sử
     * @param cursor Giá trị con trỏ mốc thời gian từ trang trước (Null nếu là trang đầu)
     * @param size Kích thước số bản ghi trên mỗi trang (Mặc định 10)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa {@link CursorPageResponse}
     */
    @GetMapping("/accounts/{accountNumber}")
    public ApiResponse<CursorPageResponse<TransactionHistoryResponse>> getHistoryByAccount(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request: CQRS Read Model query cho tài khoản [{}] với cursor [{}], size [{}]", accountNumber, cursor, size);
        CursorPageResponse<TransactionHistoryResponse> page = queryService.getHistoryByAccount(accountNumber, cursor, size);
        return ApiResponse.success("Truy vấn lịch sử giao dịch CQRS thành công", page);
    }

    /**
     * API Truy vấn lịch sử giao dịch theo Khách hàng.
     *
     * @param customerId ID của khách hàng (Mặc định CUST-001)
     * @param limit Số lượng giao dịch tối đa (Mặc định 20)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách {@link TransactionHistoryResponse}
     */
    @GetMapping("/customers/{customerId}")
    public ApiResponse<List<TransactionHistoryResponse>> getHistoryByCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("REST request: CQRS Read Model query cho khách hàng [{}]", customerId);
        List<TransactionHistoryResponse> list = queryService.getHistoryByCustomer(customerId, limit);
        return ApiResponse.success("Truy vấn lịch sử giao dịch theo khách hàng thành công", list);
    }
}
