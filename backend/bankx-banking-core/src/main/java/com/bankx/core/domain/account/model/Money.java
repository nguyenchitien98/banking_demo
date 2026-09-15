package com.bankx.core.domain.account.model;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object biểu diễn Số tiền trong hệ thống ngân hàng BankX (Money Value Object).
 *
 * <p>Nghiêm cấm dùng {@code double}/{@code float} để tính toán tiền bạc. {@link Money} sử dụng
 * {@link BigDecimal} với độ chính xác 4 chữ số thập phân và cơ chế làm tròn {@link RoundingMode#HALF_UP}
 * nhằm đảm bảo tính toàn vẹn tài chính theo chuẩn ngân hàng.</p>

 * @author BankX Engineering Team
 * @version 1.0
 */
public final class Money {

    public static final String DEFAULT_CURRENCY = "VND";
    public static final Money ZERO = new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Số tiền không được null");
        }
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
        this.currency = currency != null ? currency.toUpperCase() : DEFAULT_CURRENCY;
    }

    /**
     * Khởi tạo đối tượng Money từ BigDecimal và Currency.
     *
     * @param amount Số tiền
     * @param currency Loại tiền tệ (VND, USD...)
     * @return Đối tượng {@link Money}
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Khởi tạo đối tượng Money dạng VND từ số nguyên long.
     *
     * @param amountVnd Số tiền VND
     * @return Đối tượng {@link Money}
     */
    public static Money ofVnd(long amountVnd) {
        return new Money(BigDecimal.valueOf(amountVnd), DEFAULT_CURRENCY);
    }

    /**
     * Cộng thêm số tiền.
     *
     * @param other Số tiền cộng
     * @return Đối tượng {@link Money} mới sau khi cộng
     */
    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Trừ đi số tiền.
     *
     * @param other Số tiền trừ
     * @return Đối tượng {@link Money} mới sau khi trừ
     */
    public Money subtract(Money other) {
        checkSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        return new Money(result, this.currency);
    }

    /**
     * Kiểm tra xem số tiền có lớn hơn hoặc bằng số tiền khác không.
     *
     * @param other Số tiền so sánh
     * @return {@code true} nếu lớn hơn hoặc bằng
     */
    public boolean isGreaterThanOrEqual(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Kiểm tra xem số tiền có âm hay không.
     *
     * @return {@code true} nếu số tiền < 0
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private void checkSameCurrency(Money other) {
        if (!this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException("Không thể thực hiện phép tính trên 2 loại tiền tệ khác nhau: " + this.currency + " vs " + other.currency);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.stripTrailingZeros().toPlainString() + " " + currency;
    }
}
