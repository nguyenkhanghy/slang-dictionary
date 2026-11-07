package com.slangdict.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Một slang word cùng danh sách nghĩa (meanings). Bất biến (immutable) và Serializable
 * để có thể lưu vào file index.
 *
 * <p>Ràng buộc định dạng file (xem PROJECT.md mục 3.5) được kiểm tra ngay trong constructor:
 * <ul>
 *   <li>slang: không rỗng, không chứa dấu {@code `} hay xuống dòng (được phép chứa {@code |});</li>
 *   <li>meaning: không rỗng, không chứa {@code `}, {@code |} hay xuống dòng;</li>
 *   <li>phải có ít nhất 1 meaning.</li>
 * </ul>
 * Vi phạm -> {@link IllegalArgumentException}.
 */
public final class SlangWord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Dấu ngăn giữa slang và phần nghĩa trong file. */
    public static final char FIELD_SEPARATOR = '`';
    /** Dấu ngăn giữa các nghĩa trong file. */
    public static final char MEANING_SEPARATOR = '|';
    /** Chuỗi nối các nghĩa khi ghi file (khớp định dạng của dữ liệu gốc: "a| b"). */
    private static final String MEANING_JOINER = MEANING_SEPARATOR + " ";

    private final String slang;
    // List.copyOf(...) trả về list Serializable; trình biên dịch không biết nên cần suppress
    @SuppressWarnings("serial")
    private final List<String> meanings;

    public SlangWord(String slang, List<String> meanings) {
        this.slang = cleanSlang(slang);
        this.meanings = cleanMeanings(meanings);
    }

    public static SlangWord of(String slang, String... meanings) {
        return new SlangWord(slang, List.of(meanings));
    }

    public String getSlang() {
        return slang;
    }

    /** Danh sách nghĩa, không thể sửa đổi. */
    public List<String> getMeanings() {
        return meanings;
    }

    /** Dòng tương ứng trong file: {@code SLANG`meaning1| meaning2}. */
    public String toLine() {
        return slang + FIELD_SEPARATOR + String.join(MEANING_JOINER, meanings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlangWord other)) return false;
        return slang.equals(other.slang) && meanings.equals(other.meanings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slang, meanings);
    }

    @Override
    public String toString() {
        return toLine();
    }

    // ---------------------------------------------------------------- validation

    private static String cleanSlang(String raw) {
        Objects.requireNonNull(raw, "slang must not be null");
        String s = raw.strip();
        if (s.isEmpty()) throw new IllegalArgumentException("slang must not be empty");
        if (hasAny(s, FIELD_SEPARATOR, '\n', '\r')) {
            throw new IllegalArgumentException("slang must not contain '`' or line breaks: " + s);
        }
        return s;
    }

    private static List<String> cleanMeanings(List<String> raw) {
        Objects.requireNonNull(raw, "meanings must not be null");
        List<String> result = new ArrayList<>(raw.size());
        for (String m : raw) {
            Objects.requireNonNull(m, "meaning must not be null");
            String s = m.strip();
            if (s.isEmpty()) throw new IllegalArgumentException("meaning must not be empty");
            if (hasAny(s, FIELD_SEPARATOR, MEANING_SEPARATOR, '\n', '\r')) {
                throw new IllegalArgumentException("meaning must not contain '`', '|' or line breaks: " + s);
            }
            result.add(s);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("at least one meaning is required");
        return List.copyOf(result);
    }

    private static boolean hasAny(String s, char... chars) {
        for (char c : chars) {
            if (s.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
