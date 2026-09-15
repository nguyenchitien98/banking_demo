package com.bankx.core.domain.account.repository;

import com.bankx.core.domain.account.model.BankAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port Interface quản lý tương tác lưu trữ cho Aggregate Root {@link BankAccount}.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface BankAccountRepository {

    /**
     * Tìm danh sách tài khoản thuộc sở hữu của một khách hàng.
     *
     * @param customerId ID khách hàng
     * @return Danh sách {@link BankAccount}
     */
    List<BankAccount> findByCustomerId(UUID customerId);

    /**
     * Tìm tài khoản theo số tài khoản (Account Number).
     *
     * @param accountNumber Số tài khoản (ví dụ: "000123456789")
     * @return {@link Optional} chứa {@link BankAccount} nếu tìm thấy
     */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * Tìm tài khoản theo ID.
     *
     * @param id ID tài khoản
     * @return {@link Optional} chứa {@link BankAccount} nếu tìm thấy
     */
    Optional<BankAccount> findById(UUID id);

    /**
     * Lưu thông tin tài khoản ngân hàng (Tự động tăng @Version cho Optimistic Lock).
     *
     * @param account Đối tượng {@link BankAccount}
     * @return Đối tượng {@link BankAccount} đã lưu
     */
    BankAccount save(BankAccount account);
}
