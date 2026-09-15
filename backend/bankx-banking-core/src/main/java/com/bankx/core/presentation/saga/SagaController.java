package com.bankx.core.presentation.saga;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.saga.TransferSagaOrchestrator;
import com.bankx.core.application.saga.dto.SagaExecutionRequest;
import com.bankx.core.application.saga.dto.SagaInstanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller quản lý Saga Orchestration & Giả lập Bút toán Đảo (Compensating Transactions).
 * 
 * <p>Annotation {@link RestController} đánh dấu RESTful Controller tiếp nhận HTTP requests dạng JSON.
 * Annotation {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/sagas}.
 * Annotation {@link RequiredArgsConstructor} tự động tiêm dependency qua constructor.
 * Annotation {@link Validated} kích hoạt kiểm tra tính hợp lệ của dữ liệu đầu vào.
 * Annotation {@link Slf4j} hỗ trợ ghi nhật ký log hệ thống.</p>
 */
@RestController
@RequestMapping("/api/v1/sagas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SagaController {

    private final TransferSagaOrchestrator sagaOrchestrator;

    /**
     * API Khởi chạy luồng Saga Chuyển tiền phân tán.
     *
     * @param request DTO chứa tài khoản nguồn, đích, số tiền và cờ giả lập lỗi (NONE, CREDIT_FAILED, LEDGER_FAILED)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa {@link SagaInstanceResponse}
     */
    @PostMapping("/execute")
    public ApiResponse<SagaInstanceResponse> executeSaga(@RequestBody SagaExecutionRequest request) {
        log.info("REST request: Khởi chạy Saga Orchestrator từ [{}] -> [{}] với cờ giả lập lỗi [{}]",
                request.getSourceAccountNumber(), request.getTargetAccountNumber(), request.getForceFailureStep());
        SagaInstanceResponse response = sagaOrchestrator.executeSaga(request);
        return ApiResponse.success("Thực thi Saga Orchestration thành công", response);
    }

    /**
     * API Tra cứu chi tiết một Saga Instance và lịch sử State Machine các bước.
     *
     * @param sagaId Mã Saga ID
     * @return Phản hồi chuẩn {@link ApiResponse} chứa đối tượng {@link SagaInstanceResponse}
     */
    @GetMapping("/{sagaId}")
    public ApiResponse<SagaInstanceResponse> getSagaDetails(@PathVariable String sagaId) {
        log.info("REST request: Tra cứu chi tiết Saga [{}]", sagaId);
        SagaInstanceResponse response = sagaOrchestrator.getSagaDetails(sagaId);
        return ApiResponse.success("Lấy chi tiết Saga thành công", response);
    }

    /**
     * API Lấy danh sách tất cả các Saga Instances.
     *
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách các Saga Instances
     */
    @GetMapping
    public ApiResponse<List<SagaInstanceResponse>> listSagas() {
        log.info("REST request: Lấy danh sách toàn bộ các Saga Instances");
        List<SagaInstanceResponse> list = sagaOrchestrator.listSagas();
        return ApiResponse.success("Lấy danh sách Sagas thành công", list);
    }
}
