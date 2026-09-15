package com.bankx.core.domain.card;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;

/**
 * Trạng thái của Thẻ Ngân hàng và Máy trạng thái hữu hạn FSM (Finite State Machine).
 * 
 * <p>Quy tắc chuyển đổi trạng thái vòng đời Thẻ (Card Lifecycle FSM):</p>
 * <ul>
 *   <li><b>ACTIVE</b> $\leftrightarrow$ <b>FROZEN</b>: Khóa / Mở khóa thẻ tạm thời (Cho phép khôi phục).</li>
 *   <li><b>ACTIVE</b> $\rightarrow$ <b>BLOCKED</b>: Báo mất / Khóa thẻ vĩnh viễn (KHÔNG cho phép mở lại).</li>
 *   <li><b>FROZEN</b> $\rightarrow$ <b>BLOCKED</b>: Chuyển từ tạm khóa sang khóa vĩnh viễn.</li>
 *   <li>Bất kỳ trạng thái $\rightarrow$ <b>EXPIRED</b>: Khi thẻ hết hạn sử dụng.</li>
 * </ul>
 */
public enum CardStatus {
    /** Thẻ đang hoạt động bình thường */
    ACTIVE,

    /** Thẻ đang bị khóa tạm thời (Có thể mở lại) */
    FROZEN,

    /** Thẻ bị khóa vĩnh viễn/Báo mất (Không thể khôi phục) */
    BLOCKED,

    /** Thẻ đã hết hạn */
    EXPIRED;

    /**
     * Kiểm tra và thực thi chuyển đổi trạng thái theo quy tắc FSM.
     * 
     * @param targetStatus Trạng thái đích muốn chuyển sang
     * @throws BankingException Ném lỗi nếu giao dịch chuyển trạng thái không hợp lệ
     */
    public void validateTransitionTo(CardStatus targetStatus) {
        if (this == targetStatus) {
            return;
        }

        if (this == BLOCKED) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Thẻ đã bị khóa vĩnh viễn, không thể chuyển sang trạng thái " + targetStatus);
        }

        if (this == EXPIRED) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Thẻ đã hết hạn sử dụng, không thể chuyển sang trạng thái " + targetStatus);
        }

        boolean valid = switch (this) {
            case ACTIVE -> targetStatus == FROZEN || targetStatus == BLOCKED || targetStatus == EXPIRED;
            case FROZEN -> targetStatus == ACTIVE || targetStatus == BLOCKED || targetStatus == EXPIRED;
            default -> false;
        };

        if (!valid) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, 
                    "Không thể chuyên trạng thái thẻ từ " + this + " sang " + targetStatus);
        }
    }
}
