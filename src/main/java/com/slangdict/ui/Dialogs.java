package com.slangdict.ui;

import java.awt.Component;

/**
 * Mọi hộp thoại xác nhận/thông báo của ứng dụng đi qua đây để thống nhất cách hỏi (nút, nút mặc định, chữ) và để kiểm thử
 * thay được bằng bản giả. Chỉ gọi trên EDT. Hành động phá hủy luôn có nút mặc định là "Hủy".
 */
interface Dialogs {

    /** Lựa chọn khi thêm một từ lóng mà khóa đã tồn tại. */
    enum DuplicateChoice { OVERWRITE, ADD_DUPLICATE, CANCEL }

    /** Hỏi có/không; trả true khi người dùng chọn {@code yesLabel} (đóng cửa sổ hoặc chọn {@code noLabel} → false). */
    boolean confirm(Component parent, String title, String message, String yesLabel, String noLabel);

    /** Hỏi cách xử lý khi {@code slang} đã tồn tại: ghi đè, thêm bản trùng hoặc hủy. */
    DuplicateChoice askDuplicate(Component parent, String slang);

    /** Báo lỗi nghiêm trọng (ví dụ không ghi được file). */
    void error(Component parent, String title, String message);
}
