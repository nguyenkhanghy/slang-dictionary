package com.slangdict.ui;

import java.awt.FontMetrics;
import java.util.Arrays;
import java.util.List;

/**
 * Ngắt dòng theo chiều rộng pixel. Đếm dòng (tính chiều cao hàng) và cắt dòng (vẽ chữ) dùng CHUNG một thuật toán
 * nên chiều cao hàng luôn khớp với nét vẽ. Chiều rộng từng ký tự được nhớ lại, nên đếm hàng chục nghìn hàng vẫn nhanh.
 * Không an toàn khi dùng từ nhiều luồng (chỉ dùng trên EDT).
 */
final class TextWrapper {

    private static final int UNKNOWN = -1;

    private final FontMetrics metrics;
    private final int[] bmpWidths = new int[0x10000];

    TextWrapper(FontMetrics metrics) {
        this.metrics = metrics;
        Arrays.fill(bmpWidths, UNKNOWN);
    }

    int lineHeight() {
        return metrics.getHeight();
    }

    int ascent() {
        return metrics.getAscent();
    }

    private int charWidth(int codePoint) {
        if (codePoint >= bmpWidths.length) {
            return metrics.charWidth(codePoint);
        }
        int width = bmpWidths[codePoint];
        if (width == UNKNOWN) {
            width = metrics.charWidth((char) codePoint);
            bmpWidths[codePoint] = width;
        }
        return width;
    }

    /**
     * Ngắt {@code text} thành các dòng rộng tối đa {@code maxWidth} px: ưu tiên ngắt ở dấu cách, từ dài hơn một dòng
     * thì ngắt cứng. Trả về số dòng (≥ 1). Nếu {@code out} khác null thì thêm từng dòng vào đó (dấu cách ở chỗ ngắt bị bỏ).
     */
    int wrap(String text, int maxWidth, List<String> out) {
        int length = text.length();
        int lines = 0;
        int lineStart = 0;
        int lastSpace = -1;
        int width = 0;
        int i = 0;
        while (i < length) {
            int codePoint = text.codePointAt(i);
            int charWidth = charWidth(codePoint);
            if (width + charWidth > maxWidth && i > lineStart) {
                int breakAt = i;
                int next = i;
                if (codePoint == ' ') {
                    next = i + 1;
                } else if (lastSpace > lineStart) {
                    breakAt = lastSpace;
                    next = lastSpace + 1;
                }
                emit(text, lineStart, breakAt, out);
                lines++;
                lineStart = next;
                i = next;
                width = 0;
                lastSpace = -1;
                continue;
            }
            if (codePoint == ' ') {
                lastSpace = i;
            }
            width += charWidth;
            i += Character.charCount(codePoint);
        }
        emit(text, lineStart, length, out);
        return lines + 1;
    }

    private static void emit(String text, int from, int to, List<String> out) {
        if (out != null) {
            out.add(text.substring(from, to));
        }
    }
}
