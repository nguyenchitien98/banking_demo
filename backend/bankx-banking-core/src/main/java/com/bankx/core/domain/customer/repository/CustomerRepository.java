package com.bankx.core.domain.customer.repository;

import com.bankx.core.domain.customer.model.Customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Port Interface quản lý tương tác lưu trữ cho Aggregate Root {@link Customer}.
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface CustomerRepository {

    /**
     * Tìm kiếm thông tin khách hàng theo userId sở hữu.
     *
     * @param userId ID tài khoản người dùng
     * @return {@link Optional} chứa {@link Customer} nếu tìm thấy
     */
    Optional<Customer> findByUserId(UUID userId);

    /**
     * Tìm kiếm thông tin khách hàng theo ID.
     *
     * @param id ID khách hàng
     * @return {@link Optional} chứa {@link Customer} nếu tìm thấy
     */
    Optional<Customer> findById(UUID id);

    /**
     * Tìm kiếm thông tin khách hàng theo mã CIF.
     *
     * @param cifNumber Mã CIF
     * @return {@link Optional} chứa {@link Customer} nếu tìm thấy
     */
    Optional<Customer> findByCifNumber(String cifNumber);

    /**
     * Lưu thông tin khách hàng vào cơ sở dữ liệu.
     *
     * @param customer Đối tượng domain {@link Customer}
     * @return Đối tượng {@link Customer} đã lưu
     */
    Customer save(Customer customer);
}
