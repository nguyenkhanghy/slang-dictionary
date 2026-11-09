package com.slangdict.ui;

import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Dòng thông báo kết quả thao tác dưới biểu mẫu: thành công (xanh), lỗi (đỏ) hoặc ghi chú (xám). Luôn giữ một chiều cao
 * tối thiểu nên bố cục không nhảy khi thông báo hiện hoặc mất.
 */
final class StatusLine extends JPanel {

    private String text = "";

    StatusLine() {
        super(new BorderLayout());
        setOpaque(false);
    }

    void showSuccess(String message) {
        show(message, Theme.SUCCESS);
    }

    void showError(String message) {
        show(message, Theme.DANGER);
    }

    void showNote(String message) {
        show(message, Theme.TEXT_MUTED);
    }

    void clear() {
        text = "";
        removeAll();
        revalidate();
        repaint();
    }

    private void show(String message, Color color) {
        text = message;
        removeAll();
        add(new WrappedText(message, Theme.FONT_BODY, color, Theme.TEXT_MAX_WIDTH), BorderLayout.WEST);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, Theme.STATUS_LINE_MIN_HEIGHT));
    }
}
