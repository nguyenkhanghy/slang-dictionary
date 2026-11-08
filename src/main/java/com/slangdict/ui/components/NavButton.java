package com.slangdict.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;

/**
 * Mục điều hướng trong sidebar: nút bật/tắt tự vẽ (bo góc, nổi bật khi được chọn, có vòng focus khi dùng phím).
 * Dùng chung với {@link javax.swing.ButtonGroup} để chỉ một mục được chọn.
 */
public final class NavButton extends JToggleButton {

    public NavButton(String text) {
        super(text);
        setFont(Theme.FONT_NAV);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_M, 0, Theme.SPACE_M));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH / 2, Theme.NAV_ITEM_HEIGHT));
        setMinimumSize(new Dimension(0, Theme.NAV_ITEM_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_ITEM_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            boolean selected = isSelected();
            boolean hover = getModel().isRollover();
            int arc = Theme.RADIUS * 2;

            Color background = null;
            if (selected) {
                background = hover ? Theme.ACCENT_DARK : Theme.ACCENT;
            } else if (hover) {
                background = Theme.ACCENT_SOFT;
            }
            if (background != null) {
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            }

            g2.setFont(getFont());
            g2.setColor(selected ? Theme.ON_ACCENT : Theme.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);

            if (isFocusOwner()) {
                int inset = selected ? 3 : 1;
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(selected ? Theme.ON_ACCENT : Theme.ACCENT);
                g2.drawRoundRect(inset, inset, getWidth() - 1 - 2 * inset, getHeight() - 1 - 2 * inset,
                        arc - inset, arc - inset);
            }
        } finally {
            g2.dispose();
        }
    }
}