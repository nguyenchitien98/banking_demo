package com.bankx.core.presentation.fraud;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.fraud.FraudApplicationService;
import com.bankx.core.application.fraud.dto.EvaluateTransactionRequest;
import com.bankx.core.application.fraud.dto.ReviewAlertRequest;
import com.bankx.core.domain.fraud.FraudAlertJpaEntity;
import com.bankx.core.domain.fraud.FraudEvaluationResult;
import com.bankx.core.domain.fraud.FraudRuleJpaEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller tiếp nhận các yêu cầu Đánh giá Gian lận và Quản trị Cảnh báo (Fraud Detection REST API).
 * 
 * <p>Sử dụng các annotation {@link RestController} để tạo RESTful APIs,
 * {@link RequestMapping} chỉ định tiền tố URL {@code /api/v1/fraud},
 * và {@link Validated} để kiểm tra tham số request.</p>
 */
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Validated
@Slf4j
public class FraudController {

    private final FraudApplicationService fraudApplicationService;

    /**
     * API Thực thi đánh giá rủi ro giao dịch (Fraud Risk Engine Evaluation).
     * 
     * @param request DTO tham số giao dịch
     * @return Phản hồi chuẩn {@link ApiResponse} chứa điểm số Risk Score và hành động đề xuất
     */
    @PostMapping("/evaluate")
    public ApiResponse<FraudEvaluationResult> evaluateTransaction(@Valid @RequestBody EvaluateTransactionRequest request) {
        log.info("REST request: Đánh giá rủi ro giao dịch cho tài khoản [{}]", request.sourceAccountId());
        FraudEvaluationResult result = fraudApplicationService.evaluateTransaction(request);
        return ApiResponse.success("Đánh giá điểm rủi ro gian lận thành công", result);
    }

    /**
     * API Lấy danh sách Cảnh báo Gian lận (Admin Fraud Alerts Center).
     * 
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách cảnh báo
     */
    @GetMapping("/alerts")
    public ApiResponse<List<FraudAlertJpaEntity>> getAlerts() {
        log.info("REST request: Lấy danh sách Cảnh báo Gian lận");
        List<FraudAlertJpaEntity> alerts = fraudApplicationService.getAlerts();
        return ApiResponse.success("Lấy danh sách cảnh báo thành công", alerts);
    }

    /**
     * API Phê duyệt / Xử lý Cảnh báo Gian lận.
     * 
     * @param id ID cảnh báo
     * @param request DTO trạng thái mới và ghi chú
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin sau cập nhật
     */
    @PatchMapping("/alerts/{id}/review")
    public ApiResponse<FraudAlertJpaEntity> reviewAlert(
            @PathVariable String id,
            @Valid @RequestBody ReviewAlertRequest request) {
        log.info("REST request: Phê duyệt cảnh báo gian lận [{}]", id);
        FraudAlertJpaEntity alert = fraudApplicationService.reviewAlert(id, request);
        return ApiResponse.success("Duyệt cảnh báo gian lận thành công", alert);
    }

    /**
     * API Lấy danh sách các Quy tắc Gian lận (Fraud Rules Management).
     * 
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách quy tắc
     */
    @GetMapping("/rules")
    public ApiResponse<List<FraudRuleJpaEntity>> getRules() {
        log.info("REST request: Lấy danh sách Quy tắc Gian lận");
        List<FraudRuleJpaEntity> rules = fraudApplicationService.getRules();
        return ApiResponse.success("Lấy danh sách quy tắc thành công", rules);
    }

    /**
     * API Bật / Tắt trạng thái kích hoạt của một Quy tắc Gian lận.
     * 
     * @param id ID quy tắc
     * @param isActive Trạng thái bật/tắt
     * @return Phản hồi chuẩn {@link ApiResponse} chứa quy tắc sau cập nhật
     */
    @PatchMapping("/rules/{id}/toggle")
    public ApiResponse<FraudRuleJpaEntity> toggleRule(
            @PathVariable String id,
            @RequestParam boolean isActive) {
        log.info("REST request: Đổi trạng thái quy tắc [{}]: isActive={}", id, isActive);
        FraudRuleJpaEntity rule = fraudApplicationService.toggleRule(id, isActive);
        return ApiResponse.success("Cập nhật trạng thái quy tắc thành công", rule);
    }
}
