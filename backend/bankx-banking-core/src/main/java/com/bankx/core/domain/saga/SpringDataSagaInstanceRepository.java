package com.bankx.core.domain.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA quản lý lưu trữ và truy vấn Saga Instance trong cơ sở dữ liệu.
 */
@Repository
public interface SpringDataSagaInstanceRepository extends JpaRepository<SagaInstanceJpaEntity, String> {

    /**
     * Tìm Saga Instance theo mã chuyển tiền.
     *
     * @param transferCode Mã chuyển tiền giao dịch
     * @return {@link Optional} chứa {@link SagaInstanceJpaEntity}
     */
    Optional<SagaInstanceJpaEntity> findByTransferCode(String transferCode);

    /**
     * Lấy tất cả các Saga Instance sắp xếp theo thời gian tạo mới nhất.
     *
     * @return Danh sách entity Saga Instance
     */
    List<SagaInstanceJpaEntity> findAllByOrderByCreatedAtDesc();
}
