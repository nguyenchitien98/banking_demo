package com.bankx.core.domain.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA quản lý truy vấn các bước audit của Saga Instance.
 */
@Repository
public interface SpringDataSagaAuditStepRepository extends JpaRepository<SagaAuditStepJpaEntity, String> {

    /**
     * Tìm các bước chuyển trạng thái theo Saga ID.
     *
     * @param sagaId Mã Saga ID
     * @return Danh sách {@link SagaAuditStepJpaEntity} sắp xếp theo thời gian
     */
    List<SagaAuditStepJpaEntity> findBySagaIdOrderByCreatedAtAsc(String sagaId);
}
