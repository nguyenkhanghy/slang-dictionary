package com.slangdict.ui;

import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Phần đầu chuẩn của một trang: tiêu đề lớn và mô tả (cả hai lấy từ {@link Messages} theo {@link Page}). */
final class PageHeader {

    private PageHeader() {
    }

    static JComponent create(Page page) {
        JLabel title = new JLabel(Messages.pageTitle(page));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);
        WrappedText description = new WrappedText(Messages.pageDescription(page), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);
        title.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        description.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        header.add(description);
        header.add(Box.createVerticalStrut(Theme.SPACE_L));
        return header;
    }
}
