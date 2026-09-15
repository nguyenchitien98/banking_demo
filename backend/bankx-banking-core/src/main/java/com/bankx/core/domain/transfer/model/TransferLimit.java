package com.bankx.core.domain.transfer.model;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.domain.account.model.Money;

import java.util.UUID;

/**
 * Domain Model quản lý Hạn mức giao dịch chuyển tiền (Transfer Limit).
 *
 * <p>Kiểm tra hạn mức tối đa cho 1 giao dịch (Single Limit) và tổng hạn mức trong ngày (Daily Limit).</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public class TransferLimit {

    private final UUID id;
    private final UUID customerId;
    private final Money singleLimit;
    private final Money dailyLimit;

    public TransferLimit(UUID id, UUID customerId, Money singleLimit, Money dailyLimit) {
        this.id = id != null ? id : UUID.randomUUID();
        this.customerId = customerId;
        this.singleLimit = singleLimit != null ? singleLimit : Money.ofVnd(50_000_000); // 50 triệu
        this.dailyLimit = dailyLimit != null ? dailyLimit : Money.ofVnd(500_000_000);   // 500 triệu
    }

    /**
     * Kiểm tra xem giao dịch có vượt quá hạn mức chuyển tiền quy định hay không.
     *
     * @param amount Số tiền giao dịch
     * @param dailyAccumulatedAmount Tổng số tiền đã chuyển trong ngày hiện tại
     * @throws BankingException Nếu số tiền vượt hạn mức
     */
    public void validateTransferLimit(Money amount, Money dailyAccumulatedAmount) {
        if (amount.isGreaterThanOrEqual(singleLimit) && !amount.equals(singleLimit)) {
            throw new BankingException(ErrorCode.TRANSFER_LIMIT_EXCEEDED,
                    String.format("Vượt hạn mức 1 lần giao dịch (%s VND)", singleLimit.getAmount().stripTrailingZeros().toPlainString()));
        }

        Money totalAfter = dailyAccumulatedAmount != null ? dailyAccumulatedAmount.add(amount) : amount;
        if (totalAfter.isGreaterThanOrEqual(dailyLimit) && !totalAfter.equals(dailyLimit)) {
            throw new BankingException(ErrorCode.TRANSFER_LIMIT_EXCEEDED,
                    String.format("Vượt hạn mức chuyển tiền tối đa trong ngày (%s VND)", dailyLimit.getAmount().stripTrailingZeros().toPlainString()));
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Money getSingleLimit() {
        return singleLimit;
    }

    public Money getDailyLimit() {
        return dailyLimit;
    }
}
