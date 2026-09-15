package com.bankx.core.application.cqrs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic DTO chứa phản hồi dữ liệu phân trang theo con trỏ (Cursor-based Pagination).
 *
 * @param <T> Kiểu dữ liệu danh sách phần tử
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {
    /** Danh sách phần tử của trang hiện tại */
    private List<T> items;

    /** Giá trị con trỏ (Cursor) tiếp theo để lấy trang kế tiếp (Null nếu hết dữ liệu) */
    private String nextCursor;

    /** Cờ đánh dấu hệ thống còn dữ liệu ở các trang tiếp theo hay không */
    private boolean hasNext;

    /** Số lượng phần tử trả về trong trang này */
    private int pageSize;
}
