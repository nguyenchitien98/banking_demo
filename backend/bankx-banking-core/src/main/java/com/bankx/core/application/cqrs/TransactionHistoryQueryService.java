package com.bankx.core.application.cqrs;

import com.bankx.core.application.cqrs.dto.CursorPageResponse;
import com.bankx.core.application.cqrs.dto.TransactionHistoryResponse;
import com.bankx.core.domain.cqrs.SpringDataTransactionHistoryViewRepository;
import com.bankx.core.domain.cqrs.TransactionHistoryViewJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application Query Service cung cấp các API đọc dữ liệu lịch sử giao dịch từ CQRS Read Model.
 * 
 * <p>Annotation {@link Service} đăng ký class này thành một Spring Service Component phục vụ các truy vấn đọc (Query side).
 * Annotation {@link Transactional} với {@code readOnly = true} giúp tối ưu hóa kết nối CSDL PostgreSQL (không mở transaction ghi, không dirty checking Hibernate).</p>
 * 
 * <p><b>Phân trang theo Con trỏ (Cursor-based Pagination):</b>
 * Giải quyết triệt để vấn đề sụt giảm hiệu năng của {@code OFFSET N} khi truy vấn trên dữ liệu lớn (hàng triệu bản ghi).
 * Con trỏ {@code cursor} sử dụng mốc thời gian {@link Instant} của bản ghi cuối cùng ở trang trước.</p>
 */
@Service
public class TransactionHistoryQueryService {

    private final SpringDataTransactionHistoryViewRepository repository;

    public TransactionHistoryQueryService(SpringDataTransactionHistoryViewRepository repository) {
        this.repository = repository;
    }

    /**
     * Truy vấn lịch sử giao dịch theo số tài khoản sử dụng phân trang con trỏ (Cursor Pagination).
     *
     * @param accountNumber Số tài khoản cần xem lịch sử
     * @param cursor Mốc thời gian ISO Instant của trang trước (Null nếu lấy trang đầu tiên)
     * @param size Số lượng bản ghi cần lấy (mặc định 10)
     * @return {@link CursorPageResponse} chứa danh sách lịch sử và con trỏ cho trang kế tiếp
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<TransactionHistoryResponse> getHistoryByAccount(String accountNumber, String cursor, int size) {
        int fetchSize = size + 1; // Lấy dư 1 phần tử để kiểm tra hasNext
        Pageable pageable = PageRequest.of(0, fetchSize);

        List<TransactionHistoryViewJpaEntity> entities;
        if (cursor != null && !cursor.isBlank()) {
            Instant cursorInstant = Instant.parse(cursor);
            entities = repository.findByAccountNumberWithCursor(accountNumber, cursorInstant, pageable);
        } else {
            entities = repository.findByAccountNumberOrderByCreatedAtDescIdDesc(accountNumber, pageable);
        }

        boolean hasNext = entities.size() > size;
        List<TransactionHistoryViewJpaEntity> pagedEntities = hasNext ? entities.subList(0, size) : entities;

        List<TransactionHistoryResponse> items = pagedEntities.stream()
                .map(this::mapToResponse)
                .toList();

        String nextCursor = null;
        if (hasNext && !pagedEntities.isEmpty()) {
            TransactionHistoryViewJpaEntity lastItem = pagedEntities.get(pagedEntities.size() - 1);
            nextCursor = lastItem.getCreatedAt().toString();
        }

        return CursorPageResponse.<TransactionHistoryResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(items.size())
                .build();
    }

    /**
     * Lấy toàn bộ danh sách lịch sử giao dịch của khách hàng.
     *
     * @param customerId ID của khách hàng
     * @param limit Số lượng bản ghi tối đa
     * @return Danh sách DTO {@link TransactionHistoryResponse}
     */
    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getHistoryByCustomer(String customerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Chuyển đổi từ JPA Entity Read Model sang DTO Phản hồi.
     *
     * @param entity Entity {@link TransactionHistoryViewJpaEntity}
     * @return DTO {@link TransactionHistoryResponse}
     */
    private TransactionHistoryResponse mapToResponse(TransactionHistoryViewJpaEntity entity) {
        return TransactionHistoryResponse.builder()
                .id(entity.getId())
                .transactionReference(entity.getTransactionReference())
                .customerId(entity.getCustomerId())
                .accountNumber(entity.getAccountNumber())
                .oppositeAccountNumber(entity.getOppositeAccountNumber())
                .oppositeAccountName(entity.getOppositeAccountName())
                .amount(entity.getAmount())
                .direction(entity.getDirection())
                .transactionType(entity.getTransactionType())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
