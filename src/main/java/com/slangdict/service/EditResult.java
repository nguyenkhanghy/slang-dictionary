package com.slangdict.service;

/** Kết quả của {@link DictionaryService#editSlang}. */
public enum EditResult {
    /** Đã sửa và lưu. */
    EDITED,
    /** Nội dung mới giống hệt nội dung cũ: không ghi gì. */
    UNCHANGED,
    /** Không tìm thấy entry cần sửa (đã bị xóa/đổi). */
    NOT_FOUND,
    /** Slang mới trùng khóa với một entry khác: không có gì thay đổi. */
    CONFLICT
}
