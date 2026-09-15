package com.bankx.core.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA quản lý truy vấn dữ liệu Nhật ký Kiểm toán Admin trong cơ sở dữ liệu.
 * 
 * <p>Annotation {@link Repository} đăng ký interface này thành một Spring Bean thuộc tầng Data Access Layer,
 * tự động dịch chuyển các ngoại lệ SQL/Persistence thành các ngoại lệ chuẩn của Spring Framework.</p>
 */
@Repository
public interface SpringDataAdminAuditLogRepository extends JpaRepository<AdminAuditLogJpaEntity, String> {

    /**
     * Lấy danh sách nhật ký thao tác admin được sắp xếp theo thời gian tạo mới nhất.
     *
     * @return Danh sách entity {@link AdminAuditLogJpaEntity}
     */
    List<AdminAuditLogJpaEntity> findAllByOrderByCreatedAtDesc();
}
