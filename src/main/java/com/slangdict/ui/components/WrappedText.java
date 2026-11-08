package com.slangdict.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * Đoạn văn tự xuống dòng theo một chiều rộng tối đa cố định (tự đo bằng {@link FontMetrics}, không dùng HTML:
 * HTML của Swing quy đổi px khác nhau giữa các hệ điều hành). Kích thước cố định nên bố cục không phụ thuộc cửa sổ.
 */
public final class WrappedText extends JComponent {

    private final List<String> lines;

    public WrappedText(String text, Font font, Color color, int maxWidth) {
        setFont(font);
        setForeground(color);
        setOpaque(false);
        FontMetrics metrics = getFontMetrics(font);
        lines = wrap(text, metrics, maxWidth);
        Dimension size = new Dimension(maxWidth, lines.size() * metrics.getHeight());
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                result.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            result.add(line.toString());
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics metrics = g2.getFontMetrics();
            int y = metrics.getAscent();
            for (String line : lines) {
                g2.drawString(line, 0, y);
                y += metrics.getHeight();
            }
        } finally {
            g2.dispose();
        }
    }
}