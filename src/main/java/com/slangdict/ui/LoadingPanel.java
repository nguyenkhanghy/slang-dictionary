package com.slangdict.ui;

import com.slangdict.ui.components.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/** Nội dung cửa sổ tải: tên ứng dụng, trạng thái, thanh tiến trình không xác định và một dòng gợi ý. */
final class LoadingPanel extends JPanel {

    private final JLabel status = new JLabel(Messages.LOADING_STATUS);

    LoadingPanel() {
        setBackground(Theme.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        JLabel name = new JLabel(Messages.APP_NAME);
        name.setFont(Theme.FONT_WORDMARK);
        name.setForeground(Theme.TEXT);

        status.setFont(Theme.FONT_BODY);
        status.setForeground(Theme.TEXT);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        JLabel hint = new JLabel(Messages.LOADING_HINT);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_MUTED);

        for (JComponent c : new JComponent[]{name, status, bar, hint}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        add(name);
        add(Box.createVerticalStrut(Theme.SPACE_L));
        add(status);
        add(Box.createVerticalStrut(Theme.SPACE_S));
        add(bar);
        add(Box.createVerticalStrut(Theme.SPACE_S));
        add(hint);
    }

    void showClosing() {
        status.setText(Messages.LOADING_CANCELLING);
    }
}