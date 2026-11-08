package com.slangdict.ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/** Thanh cuộn mảnh, nền trong suốt, con trượt bo tròn theo màu của {@link Theme}. */
public final class SlimScrollBarUI extends BasicScrollBarUI {

    private static final int WIDTH = 12;

    /** Bỏ khung viền/nền của Nimbus cho scroll pane và dùng thanh cuộn mảnh. */
    public static void apply(JScrollPane scrollPane) {
        scrollPane.setUI(new BasicScrollPaneUI());
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        scrollPane.getHorizontalScrollBar().setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(WIDTH, WIDTH);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private static JButton emptyButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // nền trong suốt
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumb) {
        if (!c.isEnabled() || thumb.isEmpty()) {
            return;
        }
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            g2.setColor(isThumbRollover() || isDragging ? Theme.SCROLL_THUMB_HOVER : Theme.SCROLL_THUMB);
            int inset = 2;
            int arc = WIDTH - 2 * inset;
            g2.fillRoundRect(thumb.x + inset, thumb.y + inset, thumb.width - 2 * inset, thumb.height - 2 * inset,
                    arc, arc);
        } finally {
            g2.dispose();
        }
    }
}