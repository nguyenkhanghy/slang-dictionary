package com.slangdict.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/** Panel bo góc có nền và viền, dùng cho thẻ/khối nội dung. Màu truyền từ {@link Theme}. */
public class RoundedPanel extends JPanel {

    private final Color fill;
    private final Color line;

    public RoundedPanel(LayoutManager layout, Color fill, Color line) {
        super(layout);
        this.fill = fill;
        this.line = line;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            int arc = Theme.RADIUS * 2;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}