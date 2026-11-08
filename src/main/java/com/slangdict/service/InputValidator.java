package com.slangdict.service;

import java.util.ArrayList;
import java.util.List;

import com.slangdict.model.SlangWord;
import com.slangdict.service.ValidationException.Reason;

/**
 * Kiểm tra dữ liệu người dùng nhập TRƯỚC khi tạo {@link SlangWord}, để báo lỗi rõ ràng
 * ({@link ValidationException}) thay vì để {@code IllegalArgumentException} chung chung.
 *
 * <p>Quy tắc: slang không rỗng, không chứa {@code `} hoặc xuống dòng (được phép chứa {@code |});
 * có ít nhất một nghĩa; mỗi nghĩa không rỗng, không chứa {@code `}, {@code |} hoặc xuống dòng.
 */
public final class InputValidator {

    private InputValidator() {
    }

    /** Kiểm tra rồi tạo SlangWord (đã strip). */
    public static SlangWord toSlangWord(String slang, List<String> meanings) {
        return new SlangWord(validateSlang(slang), validateMeanings(meanings));
    }

    /** @return slang đã strip. */
    public static String validateSlang(String slang) {
        if (slang == null || slang.isBlank()) {
            throw new ValidationException(Reason.SLANG_EMPTY, "Slang must not be empty");
        }
        String s = slang.strip();
        if (hasAny(s, SlangWord.FIELD_SEPARATOR, '\n', '\r')) {
            throw new ValidationException(Reason.SLANG_INVALID_CHAR, "Slang must not contain '`' or line breaks");
        }
        return s;
    }

    /** @return danh sách nghĩa đã strip. */
    public static List<String> validateMeanings(List<String> meanings) {
        if (meanings == null || meanings.isEmpty()) {
            throw new ValidationException(Reason.NO_MEANING, "At least one meaning is required");
        }
        List<String> result = new ArrayList<>(meanings.size());
        for (String m : meanings) {
            if (m == null || m.isBlank()) {
                throw new ValidationException(Reason.MEANING_EMPTY, "A meaning must not be empty");
            }
            String s = m.strip();
            if (hasAny(s, SlangWord.FIELD_SEPARATOR, SlangWord.MEANING_SEPARATOR, '\n', '\r')) {
                throw new ValidationException(Reason.MEANING_INVALID_CHAR,
                        "A meaning must not contain '`', '|' or line breaks");
            }
            result.add(s);
        }
        return result;
    }

    /** Tách ô nhập nhiều nghĩa (ngăn bằng '|'), strip và bỏ nghĩa rỗng. Không có nghĩa nào → NO_MEANING. */
    public static List<String> parseMeanings(String raw) {
        List<String> result = new ArrayList<>();
        if (raw != null) {
            for (String part : raw.split("\\|")) {
                String s = part.strip();
                if (!s.isEmpty()) {
                    result.add(s);
                }
            }
        }
        if (result.isEmpty()) {
            throw new ValidationException(Reason.NO_MEANING, "At least one meaning is required");
        }
        return result;
    }

    private static boolean hasAny(String s, char... chars) {
        for (char c : chars) {
            if (s.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
