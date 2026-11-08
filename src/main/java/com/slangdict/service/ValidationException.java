package com.slangdict.service;

/**
 * Dữ liệu người dùng nhập không hợp lệ. {@link #getReason()} cho phép UI hiển thị thông báo thân thiện
 * (message của exception viết bằng tiếng Anh, dùng cho log/debug).
 */
public final class ValidationException extends IllegalArgumentException {

    public enum Reason {
        SLANG_EMPTY,
        SLANG_INVALID_CHAR,
        NO_MEANING,
        MEANING_EMPTY,
        MEANING_INVALID_CHAR
    }

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public ValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
