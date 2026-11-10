package com.slangdict.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.slangdict.model.SlangWord;

/**
 * Đọc file slang (UTF-8). Định dạng mỗi dòng: {@code SLANG`meaning1| meaning2}.
 *
 * <p>Quy tắc parse:
 * <ul>
 *   <li>Tách tại dấu {@code `} ĐẦU TIÊN; chỉ phần nghĩa mới tách tiếp bằng {@code |}
 *       (vì có slang như {@code 8-|} chứa chính ký tự '|');</li>
 *   <li>strip khoảng trắng của slang và từng nghĩa; bỏ dòng trống, bỏ BOM, bỏ header {@code Slag`Meaning};</li>
 *   <li>PHÒNG THỦ: dòng thiếu {@code `} (nghĩa bị xuống dòng) được nối vào slang của dòng liền trước;</li>
 *   <li>dòng hỏng khác bị bỏ qua và ghi log WARNING (tối đa {@value #MAX_LINE_WARNINGS} dòng đầu, không throw) - không làm crash ứng dụng.</li>
 * </ul>
 * Reader KHÔNG loại dòng trùng: người dùng có thể cố ý "duplicate" một slang (chức năng F4).
 */
public final class SlangFileReader {

    private static final Logger LOG = Logger.getLogger(SlangFileReader.class.getName());

    /** Chỉ log WARNING cho chừng này dòng lỗi đầu tiên; các dòng sau xuống FINE (tổng kết cuối vẫn là WARNING). */
    private static final int MAX_LINE_WARNINGS = 20;

    private SlangFileReader() {
    }

    public static List<SlangWord> read(Path file) throws IOException {
        List<SlangWord> words = new ArrayList<>(8192);
        int skipped = 0;
        int merged = 0;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // BOM do một số editor Windows thêm vào
                }
                if (line.isBlank()) {
                    continue;
                }

                int sep = line.indexOf(SlangWord.FIELD_SEPARATOR);
                if (sep < 0) {
                    // dòng "mồ côi": coi là phần nghĩa bị xuống dòng của entry trước
                    if (appendToPrevious(words, line)) {
                        merged++;
                        LOG.log(lineLevel(merged + skipped), "{0}:{1} line without ''`'', merged into previous entry: {2}",
                                new Object[]{file.getFileName(), String.valueOf(lineNo), line});
                    } else {
                        skipped++;
                        LOG.log(lineLevel(merged + skipped), "{0}:{1} line without ''`'' skipped: {2}",
                                new Object[]{file.getFileName(), String.valueOf(lineNo), line});
                    }
                    continue;
                }

                String slang = line.substring(0, sep);
                String meaningPart = line.substring(sep + 1);
                if (lineNo == 1 && isHeader(slang, meaningPart)) {
                    continue;
                }

                try {
                    words.add(new SlangWord(slang, splitMeanings(meaningPart)));
                } catch (IllegalArgumentException e) {
                    skipped++;
                    LOG.log(lineLevel(merged + skipped), "{0}:{1} invalid line skipped ({2}): {3}",
                            new Object[]{file.getFileName(), String.valueOf(lineNo), e.getMessage(), line});
                }
            }
        }

        if (skipped > 0 || merged > 0) {
            LOG.log(Level.WARNING, "{0}: {1} entries loaded, {2} lines merged, {3} lines skipped",
                    new Object[]{file.getFileName(), String.valueOf(words.size()), String.valueOf(merged), String.valueOf(skipped)});
        }
        return words;
    }

    private static Level lineLevel(int problemsSoFar) {
        return problemsSoFar <= MAX_LINE_WARNINGS ? Level.WARNING : Level.FINE;
    }

    private static boolean isHeader(String slang, String meaning) {
        String s = slang.strip();
        return (s.equalsIgnoreCase("slag") || s.equalsIgnoreCase("slang"))
                && meaning.strip().equalsIgnoreCase("meaning");
    }

    /** Tách phần nghĩa theo '|', strip từng nghĩa và bỏ nghĩa rỗng. */
    private static List<String> splitMeanings(String meaningPart) {
        List<String> result = new ArrayList<>();
        for (String m : meaningPart.split("\\|")) {
            String s = m.strip();
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    private static boolean appendToPrevious(List<SlangWord> words, String orphanLine) {
        if (words.isEmpty()) {
            return false;
        }
        int last = words.size() - 1;
        SlangWord previous = words.get(last);
        List<String> combined = new ArrayList<>(previous.getMeanings());
        combined.addAll(splitMeanings(orphanLine));
        try {
            words.set(last, new SlangWord(previous.getSlang(), combined));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}