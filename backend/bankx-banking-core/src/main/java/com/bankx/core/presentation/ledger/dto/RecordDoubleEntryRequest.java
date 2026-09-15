package com.bankx.core.presentation.ledger.dto;

import com.bankx.core.domain.ledger.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Yêu cầu DTO để thử nghiệm hạch toán bút toán ghi sổ kép (Double-Entry Bookkeeping Test Request).
 *
 * @param debitAccountId ID tài khoản trích nợ (tài khoản gửi)
 * @param creditAccountId ID tài khoản ghi có (tài khoản nhận)
 * @param amount Số tiền giao dịch (tối thiểu 1,000 VND)
 * @param description Diễn giải nội dung giao dịch
 * @param type Loại giao dịch {@link TransactionType}
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public record RecordDoubleEntryRequest(
        @NotNull(message = "Tài khoản trích nợ không được để trống")
        UUID debitAccountId,

        @NotNull(message = "Tài khoản ghi có không được để trống")
        UUID creditAccountId,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1000.00", message = "Số tiền tối thiểu là 1,000 VND")
        BigDecimal amount,

        String description,

        TransactionType type
) {}
