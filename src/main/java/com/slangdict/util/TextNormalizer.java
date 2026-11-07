package com.slangdict.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chuẩn hóa văn bản dùng chung cho index và tìm kiếm. Mọi so khớp không phân biệt hoa/thường
 * phải đi qua class này để index và truy vấn luôn nhất quán.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Khóa tra cứu slang: strip + IN HOA (không phụ thuộc locale máy). */
    public static String normalizeSlang(String slang) {
        return slang.strip().toUpperCase(Locale.ROOT);
    }

    /** Văn bản nghĩa/keyword: strip + in thường. */
    public static String normalizeText(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }

    /** Tách các cụm chữ/số liên tiếp (token) từ chuỗi ĐÃ in thường. Ký tự khác chữ/số là dấu ngăn. */
    public static List<String> tokenize(String lowerText) {
        List<String> tokens = new ArrayList<>(8);
        int start = -1;
        for (int i = 0; i < lowerText.length(); i++) {
            if (Character.isLetterOrDigit(lowerText.charAt(i))) {
                if (start < 0) start = i;
            } else if (start >= 0) {
                tokens.add(lowerText.substring(start, i));
                start = -1;
            }
        }
        if (start >= 0) tokens.add(lowerText.substring(start));
        return tokens;
    }
}
