package com.bankx.core.domain.auth.repository;

import com.bankx.core.domain.auth.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Port Interface quản lý tương tác lưu trữ cho Aggregate Root {@link User}.
 *
 * <p>Theo Clean Architecture, đây là cổng (Port) khai báo trong Domain layer,
 * việc thực thi (Adapter) sẽ nằm ở Infrastructure layer.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
public interface UserRepository {

    /**
     * Tìm kiếm người dùng theo tên đăng nhập (Username).
     *
     * @param username Tên đăng nhập
     * @return {@link Optional} chứa {@link User} nếu tìm thấy
     */
    Optional<User> findByUsername(String username);

    /**
     * Tìm kiếm người dùng theo ID.
     *
     * @param id ID người dùng
     * @return {@link Optional} chứa {@link User} nếu tìm thấy
     */
    Optional<User> findById(UUID id);

    /**
     * Kiểm tra xem tên đăng nhập đã tồn tại trên hệ thống chưa.
     *
     * @param username Tên đăng nhập cần kiểm tra
     * @return {@code true} nếu đã tồn tại
     */
    boolean existsByUsername(String username);

    /**
     * Lưu thông tin người dùng vào cơ sở dữ liệu.
     *
     * @param user Đối tượng domain {@link User}
     * @return Đối tượng {@link User} đã được lưu
     */
    User save(User user);
}
