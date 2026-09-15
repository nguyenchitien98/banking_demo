package com.bankx.core.infrastructure.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface Spring Data JPA Repository cho thực thể {@link UserJpaEntity}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu Spring Data Repository Bean để Spring phát hiện
 *       và tự động tạo Proxy triển khai các truy vấn SQL.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * Tìm kiếm UserJpaEntity theo username.
     *
     * @param username Tên đăng nhập
     * @return {@link Optional} chứa {@link UserJpaEntity}
     */
    Optional<UserJpaEntity> findByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại trong DB chưa.
     *
     * @param username Tên đăng nhập
     * @return {@code true} nếu đã tồn tại
     */
    boolean existsByUsername(String username);
}
