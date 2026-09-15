package com.bankx.core.presentation.beneficiary;

import com.bankx.common.dto.ApiResponse;
import com.bankx.core.application.beneficiary.BeneficiaryApplicationService;
import com.bankx.core.application.beneficiary.dto.AccountLookupResponse;
import com.bankx.core.application.beneficiary.dto.AddBeneficiaryRequest;
import com.bankx.core.application.beneficiary.dto.UpdateBeneficiaryRequest;
import com.bankx.core.domain.beneficiary.BeneficiaryJpaEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller quản lý Danh bạ Người thụ hưởng và Tra cứu tài khoản (Beneficiary REST API).
 * 
 * <p>Sử dụng các annotation {@link RestController} chỉ định RESTful Controller,
 * {@link RequestMapping} định tuyến nhóm URL {@code /api/v1/beneficiaries},
 * và {@link Validated} để kiểm tra tự động dữ liệu đầu vào.</p>
 */
@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BeneficiaryController {

    private final BeneficiaryApplicationService beneficiaryApplicationService;

    /**
     * API Lấy danh sách người thụ hưởng thường xuyên (Sắp xếp theo tần suất giao dịch transferCount DESC).
     * 
     * @param customerId ID khách hàng (Mặc định CUST-001)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa danh sách thụ hưởng
     */
    @GetMapping
    public ApiResponse<List<BeneficiaryJpaEntity>> getBeneficiaries(@RequestParam(defaultValue = "CUST-001") String customerId) {
        log.info("REST request: Lấy danh sách thụ hưởng cho khách hàng [{}]", customerId);
        List<BeneficiaryJpaEntity> list = beneficiaryApplicationService.getBeneficiaries(customerId);
        return ApiResponse.success("Lấy danh sách thụ hưởng thành công", list);
    }

    /**
     * API Thêm người thụ hưởng mới vào danh bạ.
     * 
     * @param request DTO chi tiết thông tin thụ hưởng
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin thụ hưởng vừa lưu
     */
    @PostMapping
    public ApiResponse<BeneficiaryJpaEntity> addBeneficiary(@Valid @RequestBody AddBeneficiaryRequest request) {
        log.info("REST request: Thêm người thụ hưởng [{}]", request.accountNumber());
        BeneficiaryJpaEntity beneficiary = beneficiaryApplicationService.addBeneficiary(request);
        return ApiResponse.success("Thêm người thụ hưởng thành công", beneficiary);
    }

    /**
     * API Đổi biệt danh (nickname) cho người thụ hưởng.
     * 
     * @param id ID bản ghi thụ hưởng
     * @param request DTO biệt danh mới
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin sau cập nhật
     */
    @PutMapping("/{id}")
    public ApiResponse<BeneficiaryJpaEntity> updateBeneficiary(
            @PathVariable String id,
            @Valid @RequestBody UpdateBeneficiaryRequest request) {
        log.info("REST request: Đổi biệt danh cho người thụ hưởng [{}]", id);
        BeneficiaryJpaEntity beneficiary = beneficiaryApplicationService.updateBeneficiary(id, request);
        return ApiResponse.success("Cập nhật biệt danh thành công", beneficiary);
    }

    /**
     * API Xóa một người thụ hưởng khỏi danh bạ.
     * 
     * @param id ID người thụ hưởng
     * @return Phản hồi chuẩn {@link ApiResponse} xác nhận xóa thành công
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBeneficiary(@PathVariable String id) {
        log.info("REST request: Xóa người thụ hưởng [{}] khỏi danh bạ", id);
        beneficiaryApplicationService.deleteBeneficiary(id);
        return ApiResponse.success("Xóa người thụ hưởng khỏi danh bạ thành công", null);
    }

    /**
     * API Tra cứu thông tin tên chủ tài khoản nhận tiền (Account Lookup API).
     * 
     * @param accountNumber Số tài khoản cần tra cứu
     * @param bankBin Mã BIN ngân hàng (mặc định 970400)
     * @return Phản hồi chuẩn {@link ApiResponse} chứa thông tin tra cứu
     */
    @GetMapping("/lookup")
    public ApiResponse<AccountLookupResponse> lookupAccount(
            @RequestParam String accountNumber,
            @RequestParam(required = false, defaultValue = "970400") String bankBin) {
        log.info("REST request: Tra cứu số tài khoản [{}] ngân hàng BIN [{}]", accountNumber, bankBin);
        AccountLookupResponse response = beneficiaryApplicationService.lookupAccount(accountNumber, bankBin);
        return ApiResponse.success("Tra cứu tài khoản thành công", response);
    }
}
