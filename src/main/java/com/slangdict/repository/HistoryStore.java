package com.slangdict.repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.slangdict.model.HistoryEntry;
import com.slangdict.model.SearchType;

/**
 * Lưu history tìm kiếm vào MỘT FILE RIÊNG (yêu cầu của GV), tách khỏi file dữ liệu slang.
 * Mỗi dòng: {@code epochMillis<TAB>TYPE<TAB>query}, UTF-8. Thêm mới bằng append (O(1)); dòng hỏng bị bỏ qua.
 */
public final class HistoryStore {

    private final Path file;

    public HistoryStore(Path file) {
        this.file = file;
    }

    /** Đọc toàn bộ history, cũ nhất trước. File chưa tồn tại -> list rỗng. */
    public List<HistoryEntry> loadAll() throws IOException {
        List<HistoryEntry> entries = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return entries;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                HistoryEntry entry = parse(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public void append(HistoryEntry entry) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, format(entry) + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Ghi đè toàn bộ file bằng danh sách đã cho (dùng khi cắt bớt history cũ). */
    public void rewrite(List<HistoryEntry> entries) throws IOException {
        AtomicFiles.write(file, out -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            for (HistoryEntry e : entries) {
                writer.write(format(e));
                writer.write('\n');
            }
            writer.flush();
        });
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }

    private static String format(HistoryEntry e) {
        return e.time().toEpochMilli() + "\t" + e.type().name() + "\t" + e.query();
    }

    private static HistoryEntry parse(String line) {
        String[] parts = line.split("\t", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new HistoryEntry(Instant.ofEpochMilli(Long.parseLong(parts[0])),
                    SearchType.valueOf(parts[1]), parts[2]);
        } catch (RuntimeException e) {
            return null; // dòng hỏng: bỏ qua
        }
    }
}
