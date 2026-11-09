package com.slangdict.ui;

import com.slangdict.ui.components.NavButton;
import com.slangdict.ui.components.SlimScrollBarUI;
import com.slangdict.ui.components.Theme;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Thanh điều hướng bên trái: tên ứng dụng (cố định ở trên), danh sách nhóm mục F1–F10 (cuộn được khi cửa sổ thấp)
 * và dòng thông tin dữ liệu (cố định ở dưới).
 */
final class NavSidebar extends JPanel {

    private final Map<Page, NavButton> buttons = new EnumMap<>(Page.class);
    private final ButtonGroup group = new ButtonGroup();
    private JLabel footerLabel;
    private Consumer<Page> onSelect = page -> { };

    NavSidebar(String footerText, String footerTooltip) {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SPACE_L, Theme.SPACE_M, Theme.SPACE_M, Theme.SPACE_M)));

        add(header(), BorderLayout.NORTH);
        add(navigation(), BorderLayout.CENTER);
        add(footer(footerText, footerTooltip), BorderLayout.SOUTH);
    }

    /** Đăng ký hàm được gọi mỗi khi một mục được chọn (bằng chuột, bàn phím hoặc {@link #select}). */
    void setOnSelect(Consumer<Page> onSelect) {
        this.onSelect = onSelect;
    }

    /** Đổi dòng thông tin dữ liệu ở chân sidebar (ví dụ số từ lóng sau khi thêm/xóa). Gọi trên EDT. */
    void setFooter(String text, String tooltip) {
        footerLabel.setText(text);
        footerLabel.setToolTipText(tooltip);
    }

    void select(Page page) {
        buttons.get(page).setSelected(true);
        onSelect.accept(page);
    }

    private JComponent navigation() {
        NavList list = new NavList();
        for (Page.Group g : Page.Group.values()) {
            list.add(sectionLabel(Messages.groupLabel(g)));
            for (Page page : Page.values()) {
                if (page.group() == g) {
                    list.add(button(page));
                    list.add(Box.createVerticalStrut(2));
                }
            }
        }
        JScrollPane scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SlimScrollBarUI.apply(scroll);
        return scroll;
    }

    private JComponent header() {
        JLabel name = new JLabel(Messages.APP_NAME);
        name.setFont(Theme.FONT_WORDMARK);
        name.setForeground(Theme.TEXT);
        JLabel tagline = new JLabel(Messages.APP_TAGLINE);
        tagline.setFont(Theme.FONT_SMALL);
        tagline.setForeground(Theme.TEXT_MUTED);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_M, Theme.SPACE_S, 0));
        name.setAlignmentX(LEFT_ALIGNMENT);
        tagline.setAlignmentX(LEFT_ALIGNMENT);
        header.add(name);
        header.add(Box.createVerticalStrut(2));
        header.add(tagline);
        return header;
    }

    private JComponent sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_M, Theme.SPACE_M, Theme.SPACE_S, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private NavButton button(Page page) {
        NavButton button = new NavButton(Messages.navLabel(page));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(e -> select(page));
        // Tab tới một mục đang bị che bởi vùng cuộn thì cuộn để thấy mục đó
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                button.scrollRectToVisible(new Rectangle(0, 0, button.getWidth(), button.getHeight()));
            }
        });
        group.add(button);
        buttons.put(page, button);
        return button;
    }

    private JComponent footer(String text, String tooltip) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        label.setToolTipText(tooltip);
        footerLabel = label;
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SPACE_S, Theme.SPACE_M, 0, 0)));
        return label;
    }

    /** Danh sách mục dọc, luôn rộng bằng vùng nhìn (không cuộn ngang). */
    private static final class NavList extends JPanel implements Scrollable {

        NavList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Theme.NAV_ITEM_HEIGHT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(Theme.NAV_ITEM_HEIGHT, visibleRect.height - Theme.NAV_ITEM_HEIGHT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}