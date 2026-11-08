package com.slangdict.service;

/** Kết quả của {@link DictionaryService#addSlang}. */
public enum AddResult {
    /** Đã thêm entry mới (khóa chưa tồn tại, hoặc chính sách DUPLICATE). */
    ADDED,
    /** Khóa đã tồn tại và chính sách OVERWRITE đã thay thế các entry cũ. */
    OVERWRITTEN,
    /** Khóa đã tồn tại và chính sách REJECT: không có gì thay đổi. */
    CONFLICT
}
