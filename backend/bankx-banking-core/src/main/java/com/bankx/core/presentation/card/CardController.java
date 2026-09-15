package com.bankx.core.presentation.card;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.card.CardApplicationService;
import com.bankx.core.application.card.dto.CardDetailResponse;
import com.bankx.core.application.card.dto.CreateVirtualCardRequest;
import com.bankx.core.application.card.dto.UpdateCardLimitsRequest;
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
 * REST Controller quản lý các dịch vụ Thẻ ngân hàng (Virtual Cards & Tokenization).
 * 
 * <p>Sử dụng các annotation {@link RestController} để phát triển RESTful APIs,
 * {@link RequestMapping} định tuyến đường dẫn gốc {@code /api/v1/cards},
 * và {@link Validated} để kích hoạt tự động kiểm tra tính hợp lệ dữ liệu truyền vào.</p>
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CardController {

    private final CardApplicationService cardApplicationService;

    /**
     * API Lấy danh sách thẻ ngân hàng của khách hàng.
     * 
     * @param customerId ID khách hàng (mặc định CUST-001)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách thẻ
     */
    @GetMapping
    public ApiResponse<List<CardDetailResponse>> getCards(@RequestParam(defaultValue = "CUST-001") String customerId) {
        log.info("REST request: Lấy danh sách thẻ cho khách hàng [{}]", customerId);
        List<CardDetailResponse> cards = cardApplicationService.getCardsByCustomerId(customerId);
        return ApiResponse.success("Lấy danh sách thẻ thành công", cards);
    }

    /**
     * API Lấy thông tin chi tiết một thẻ ngân hàng theo ID.
     * 
     * @param id ID thẻ
     * @return Phản hồi chuẩn {@link ApiResponse} chứa chi tiết thẻ
     */
    @GetMapping("/{id}")
    public ApiResponse<CardDetailResponse> getCardById(@PathVariable String id) {
        log.info("REST request: Lấy thông tin chi tiết thẻ [{}]", id);
        CardDetailResponse card = cardApplicationService.getCardById(id);
        return ApiResponse.success("Lấy chi tiết thẻ thành công", card);
    }

    /**
     * API Phát hành Thẻ Ảo mới (Virtual Card Issuance).
     * 
     * @param request DTO mở thẻ ảo
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thẻ vừa tạo
     */
    @PostMapping("/virtual")
    public ApiResponse<CardDetailResponse> createVirtualCard(@Valid @RequestBody CreateVirtualCardRequest request) {
        log.info("REST request: Phát hành Thẻ Ảo cho khách hàng [{}]", request.customerId());
        CardDetailResponse card = cardApplicationService.createVirtualCard(request);
        return ApiResponse.success("Phát hành Thẻ Ảo thành công", card);
    }

    /**
     * API Tạm khóa thẻ (FSM Transition: ACTIVE -> FROZEN).
     * 
     * @param id ID thẻ
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin thẻ sau khi tạm khóa
     */
    @PatchMapping("/{id}/freeze")
    public ApiResponse<CardDetailResponse> freezeCard(@PathVariable String id) {
        log.info("REST request: Tạm khóa thẻ [{}]", id);
        CardDetailResponse card = cardApplicationService.freezeCard(id);
        return ApiResponse.success("Tạm khóa thẻ thành công", card);
    }

    /**
     * API Mở khóa thẻ tạm thời (FSM Transition: FROZEN -> ACTIVE).
     * 
     * @param id ID thẻ
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin thẻ sau khi mở khóa
     */
    @PatchMapping("/{id}/unfreeze")
    public ApiResponse<CardDetailResponse> unfreezeCard(@PathVariable String id) {
        log.info("REST request: Mở khóa thẻ [{}]", id);
        CardDetailResponse card = cardApplicationService.unfreezeCard(id);
        return ApiResponse.success("Mở khóa thẻ thành công", card);
    }

    /**
     * API Khóa thẻ vĩnh viễn / Báo mất thẻ (FSM Transition: ACTIVE/FROZEN -> BLOCKED).
     * 
     * @param id ID thẻ
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin thẻ sau khi khóa vĩnh viễn
     */
    @PatchMapping("/{id}/block")
    public ApiResponse<CardDetailResponse> blockCard(@PathVariable String id) {
        log.warn("REST request: Khóa vĩnh viễn / Báo mất thẻ [{}]", id);
        CardDetailResponse card = cardApplicationService.blockCard(id);
        return ApiResponse.success("Khóa vĩnh viễn thẻ thành công", card);
    }

    /**
     * API Cập nhật hạn mức thanh toán online và hạn mức ngày của thẻ.
     * 
     * @param id ID thẻ
     * @param request DTO hạn mức mới
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin thẻ sau khi đổi hạn mức
     */
    @PatchMapping("/{id}/limits")
    public ApiResponse<CardDetailResponse> updateCardLimits(
            @PathVariable String id,
            @Valid @RequestBody UpdateCardLimitsRequest request) {
        log.info("REST request: Cập nhật hạn mức thẻ [{}]", id);
        CardDetailResponse card = cardApplicationService.updateCardLimits(id, request);
        return ApiResponse.success("Cập nhật hạn mức thẻ thành công", card);
    }
}
