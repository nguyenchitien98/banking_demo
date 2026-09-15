package com.bankx.core.application.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO yêu cầu đổi biệt danh (nickname) cho người thụ hưởng.
 * 
 * @param nickname Biệt danh mới
 */
public record UpdateBeneficiaryRequest(
        @NotBlank(message = "Biệt danh không được để trống")
        String nickname
) {
}
