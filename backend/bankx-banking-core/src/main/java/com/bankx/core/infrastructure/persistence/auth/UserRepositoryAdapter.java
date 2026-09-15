package com.bankx.core.infrastructure.persistence.auth;

import com.bankx.core.domain.auth.model.User;
import com.bankx.core.domain.auth.model.UserRole;
import com.bankx.core.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter triển khai Port Interface {@link UserRepository} trong Clean Architecture.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đăng ký lớp triển khai này thành một Spring Repository Component
 *       được nạp tự động vào ApplicationContext để đáp ứng dependency injection cho Domain layer.</li>
 * </ul>
 * </p>
 *
 * <p><b>Vai trò:</b>
 * Thực hiện chuyển đổi hai chiều (Mapping) giữa Domain Model {@link User} và JPA Persistence Entity {@link UserJpaEntity}.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    /**
     * Chuyển đổi từ JPA Entity sang Domain Model.
     */
    private User toDomain(UserJpaEntity entity) {
        Set<UserRole> roles = entity.getRoles().stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());

        User user = new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getStatus(),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil(),
                roles
        );
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }

    /**
     * Chuyển đổi từ Domain Model sang JPA Entity.
     */
    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setStatus(user.getStatus());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        return entity;
    }
}
