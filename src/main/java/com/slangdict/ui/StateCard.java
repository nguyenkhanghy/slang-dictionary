package com.slangdict.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.slangdict.ui.components.RoundedPanel;
import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

/**
 * Thẻ thông báo trạng thái của một màn (trống, đang tải, lỗi...): một tiêu đề đậm và một dòng hướng dẫn.
 * Chỉ dùng cho chuỗi ngắn do {@link Messages} cung cấp (không dùng cho dữ liệu người dùng).
 */
final class StateCard extends JPanel {

    private final JPanel content = new JPanel();

    StateCard() {
        super(new BorderLayout());
        setOpaque(false);
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L));
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        card.add(content, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    void show(String title, String detail, boolean error) {
        content.removeAll();
        WrappedText titleText = new WrappedText(title, Theme.FONT_BODY_BOLD,
                error ? Theme.DANGER : Theme.TEXT, Theme.STATE_TEXT_MAX_WIDTH);
        WrappedText detailText = new WrappedText(detail, Theme.FONT_BODY, Theme.TEXT_MUTED, Theme.STATE_TEXT_MAX_WIDTH);
        titleText.setAlignmentX(LEFT_ALIGNMENT);
        detailText.setAlignmentX(LEFT_ALIGNMENT);
        content.add(titleText);
        content.add(Box.createVerticalStrut(Theme.SPACE_XS));
        content.add(detailText);
        content.revalidate();
        content.repaint();
    }
}
