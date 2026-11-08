package com.slangdict.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.slangdict.ui.components.Theme;

/**
 * Nhãn chữ tự xuống dòng, rộng cố định, dành cho dữ liệu người dùng/từ điển có độ dài bất kỳ (slang, nghĩa): dùng
 * {@link TextWrapper} nên từ quá dài vẫn bị ngắt cứng thay vì tràn ra ngoài, và chiều cao luôn khớp nét vẽ.
 * Không dùng HTML. Chỉ dùng trên EDT.
 */
final class WrapLabel extends JComponent {

    private final TextWrapper wrapper;
    private final Font font;
    private final Color color;
    private final int width;
    private final List<String> lines = new ArrayList<>();

    WrapLabel(String text, Font font, Color color, int width) {
        this.font = font;
        this.color = color;
        this.width = width;
        this.wrapper = new TextWrapper(getFontMetrics(font));
        setText(text);
    }

    void setText(String text) {
        lines.clear();
        wrapper.wrap(text, width, lines);
        Dimension size = new Dimension(width, lines.size() * wrapper.lineHeight());
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            g2.setFont(font);
            g2.setColor(color);
            int y = wrapper.ascent();
            for (String line : lines) {
                g2.drawString(line, 0, y);
                y += wrapper.lineHeight();
            }
        } finally {
            g2.dispose();
        }
    }
}
