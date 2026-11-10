package com.slangdict.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JButton;

/**
 * Nút hành động, tự vẽ theo {@link Theme}: nền màu nhấn (đậm hơn khi rê chuột/nhấn), chữ trắng, xám khi bị khóa,
 * vòng trắng bên trong khi có focus. Cao bằng ô nhập ({@link Theme#INPUT_HEIGHT}).
 *
 * <p>Hai kiểu: nút chính (xanh, {@code new ActionButton(text)}) và nút nguy hiểm cho thao tác phá hủy
 * (đỏ, {@link #danger(String)}). Hai kiểu chỉ khác màu nền.
 */
public final class ActionButton extends JButton {

    private final boolean danger;

    public ActionButton(String text) {
        this(text, false);
    }

    private ActionButton(String text, boolean danger) {
        super(text);
        this.danger = danger;
        setFont(Theme.FONT_BODY_BOLD);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Nút cho thao tác phá hủy (xóa, khôi phục): nền đỏ, hover đậm hơn. */
    public static ActionButton danger(String text) {
        return new ActionButton(text, true);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 2 * Theme.SPACE_L, Theme.INPUT_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            int w = getWidth();
            int h = getHeight();
            int arc = Theme.RADIUS * 2;
            boolean enabled = isEnabled();
            boolean hot = getModel().isRollover() || getModel().isArmed();
            g2.setColor(fill(enabled, hot));
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            if (enabled && isFocusOwner()) {
                g2.setColor(Theme.ON_ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(3, 3, w - 7, h - 7, arc - 6, arc - 6);
            }
            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());
            g2.setColor(enabled ? Theme.ON_ACCENT : Theme.TEXT_MUTED);
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }

    private Color fill(boolean enabled, boolean hot) {
        if (!enabled) {
            return Theme.BORDER;
        }
        if (danger) {
            return hot ? Theme.DANGER_DARK : Theme.DANGER;
        }
        return hot ? Theme.ACCENT_DARK : Theme.ACCENT;
    }
}