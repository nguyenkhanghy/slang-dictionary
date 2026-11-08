package com.slangdict.service;

/** Cách xử lý khi thêm một slang mà khóa (không phân biệt hoa/thường) đã tồn tại. */
public enum DuplicatePolicy {
    /** Không thêm; trả {@link AddResult#CONFLICT} để UI hỏi người dùng. */
    REJECT,
    /** Thay TẤT CẢ entry cùng khóa bằng entry mới (đặt tại vị trí của entry đầu tiên). */
    OVERWRITE,
    /** Vẫn thêm một entry mới (cuối danh sách), giữ các entry cũ. */
    DUPLICATE
}
