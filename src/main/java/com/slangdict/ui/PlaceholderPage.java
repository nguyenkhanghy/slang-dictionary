package com.slangdict.ui;

import com.slangdict.ui.components.RoundedPanel;
import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Trang giữ chỗ của một chức năng: tiêu đề, mô tả và dòng báo trạng thái. Sẽ được thay bằng màn thật ở U3/U4. */
final class PlaceholderPage extends JPanel {

    PlaceholderPage(Page page) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        JLabel title = new JLabel(Messages.pageTitle(page));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);

        WrappedText description = new WrappedText(Messages.pageDescription(page), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);

        RoundedPanel status = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_M, Theme.SPACE_M),
                Theme.SURFACE, Theme.BORDER);
        JLabel statusText = new JLabel(Messages.PLACEHOLDER_STATUS);
        statusText.setFont(Theme.FONT_BODY);
        statusText.setForeground(Theme.TEXT_MUTED);
        status.add(statusText);
        status.setMaximumSize(new Dimension(Theme.TEXT_MAX_WIDTH, status.getPreferredSize().height));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{title, description, status}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        column.add(title);
        column.add(Box.createVerticalStrut(Theme.SPACE_S));
        column.add(description);
        column.add(Box.createVerticalStrut(Theme.SPACE_L));
        column.add(status);
        add(column, BorderLayout.NORTH);
    }
}