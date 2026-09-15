package com.bankx.core.infrastructure.persistence.auth;

import com.bankx.core.domain.auth.model.RefreshToken;
import com.bankx.core.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter triển khai Port Interface {@link RefreshTokenRepository}.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Đánh dấu Spring Repository Bean quản lý persistence cho RefreshToken.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        jpaRepository.revokeAllByUserId(userId);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = toEntity(refreshToken);
        RefreshTokenJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getExpiryDate(),
                entity.isRevoked(),
                entity.getCreatedAt()
        );
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(refreshToken.getId());
        entity.setUserId(refreshToken.getUserId());
        entity.setToken(refreshToken.getToken());
        entity.setExpiryDate(refreshToken.getExpiryDate());
        entity.setRevoked(refreshToken.isRevoked());
        entity.setCreatedAt(refreshToken.getCreatedAt());
        return entity;
    }
}
