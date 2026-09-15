package com.bankx.core.domain.cqrs;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository JPA quản lý truy vấn dữ liệu CQRS Read Model Lịch sử giao dịch.
 * 
 * <p>Annotation {@link Repository} đánh dấu component thuộc tầng Data Access Layer của Spring Data JPA.
 * Cung cấp các câu lệnh truy vấn tối ưu hóa theo chỉ mục (Indexed queries) và con trỏ (Cursor-based pagination)
 * giúp hệ thống đạt hiệu năng truy vấn O(log N) cho các bảng dữ liệu hàng triệu bản ghi.</p>
 */
@Repository
public interface SpringDataTransactionHistoryViewRepository extends JpaRepository<TransactionHistoryViewJpaEntity, String> {

    /**
     * Truy vấn lịch sử giao dịch theo số tài khoản sắp xếp mới nhất (First page).
     *
     * @param accountNumber Số tài khoản cần tra cứu
     * @param pageable Tham số phân trang
     * @return Danh sách entity {@link TransactionHistoryViewJpaEntity}
     */
    List<TransactionHistoryViewJpaEntity> findByAccountNumberOrderByCreatedAtDescIdDesc(String accountNumber, Pageable pageable);

    /**
     * Truy vấn lịch sử giao dịch theo số tài khoản bằng con trỏ thời gian (Cursor-based Pagination).
     *
     * @param accountNumber Số tài khoản cần tra cứu
     * @param cursorCreatedAt Mốc thời gian của con trỏ (Trang trước)
     * @param pageable Tham số phân trang
     * @return Danh sách entity {@link TransactionHistoryViewJpaEntity}
     */
    @Query("SELECT t FROM TransactionHistoryViewJpaEntity t WHERE t.accountNumber = :accountNumber AND t.createdAt < :cursorCreatedAt ORDER BY t.createdAt DESC, t.id DESC")
    List<TransactionHistoryViewJpaEntity> findByAccountNumberWithCursor(
            @Param("accountNumber") String accountNumber,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            Pageable pageable
    );

    /**
     * Truy vấn lịch sử giao dịch theo Khách hàng kèm lọc theo loại giao dịch.
     *
     * @param customerId ID khách hàng
     * @param pageable Tham số phân trang
     * @return Danh sách entity {@link TransactionHistoryViewJpaEntity}
     */
    List<TransactionHistoryViewJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
}
