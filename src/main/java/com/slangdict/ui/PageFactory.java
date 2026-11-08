package com.slangdict.ui;

import javax.swing.JComponent;

/** Tạo màn hình cho từng {@link Page}. Hiện mọi mục là trang giữ chỗ; U3/U4 sẽ thay từng mục bằng màn thật. */
final class PageFactory {

    private PageFactory() {
    }

    static JComponent create(Page page) {
        return new PlaceholderPage(page);
    }
}