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
 * Giữ ngắt đoạn (ký tự xuống dòng trong chuỗi) và bẻ cứng từ dài hơn một dòng (ví dụ đường dẫn thư mục).
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

    /** Mỗi đoạn (ngăn bằng xuống dòng) tự xuống dòng riêng; đoạn trống thành một dòng trống. Chuỗi rỗng: không có dòng. */
    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        List<String> result = new ArrayList<>();
        String stripped = text.strip();
        if (stripped.isEmpty()) {
            return result;
        }
        for (String paragraph : stripped.split("\\R", -1)) {
            wrapParagraph(paragraph.strip(), metrics, maxWidth, result);
        }
        return result;
    }

    private static void wrapParagraph(String paragraph, FontMetrics metrics, int maxWidth, List<String> result) {
        if (paragraph.isEmpty()) {
            result.add("");
            return;
        }
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                result.add(line.toString());
                candidate = word;
            }
            line = new StringBuilder(candidate);
            // Từ dài hơn một dòng: bẻ cứng, phần còn lại tiếp tục làm dòng hiện tại.
            while (line.length() > 1 && metrics.stringWidth(line.toString()) > maxWidth) {
                int cut = fittingLength(line, metrics, maxWidth);
                result.add(line.substring(0, cut));
                line = new StringBuilder(line.substring(cut));
            }
        }
        if (line.length() > 0) {
            result.add(line.toString());
        }
    }

    /** Số ký tự đầu của {@code text} còn vừa {@code maxWidth} (ít nhất 1 để luôn tiến lên). */
    private static int fittingLength(CharSequence text, FontMetrics metrics, int maxWidth) {
        int length = 1;
        while (length < text.length()
                && metrics.stringWidth(text.subSequence(0, length + 1).toString()) <= maxWidth) {
            length++;
        }
        return length;
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