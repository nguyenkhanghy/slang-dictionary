package com.slangdict.repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.slangdict.model.SlangWord;

/**
 * Ghi danh sách slang ra file UTF-8, mỗi entry một dòng ({@code SLANG`meaning1| meaning2}), xuống dòng LF.
 *
 * <p>Ghi AN TOÀN: ghi ra file tạm cùng thư mục rồi đổi tên đè lên file đích, để nếu chương trình
 * bị tắt giữa chừng thì file cũ vẫn còn nguyên vẹn. Không ghi dòng header.
 */
public final class SlangFileWriter {

    private SlangFileWriter() {
    }

    public static void write(Path file, List<SlangWord> words) throws IOException {
        Path target = file.toAbsolutePath();
        Path dir = target.getParent();
        Files.createDirectories(dir);

        Path temp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (SlangWord word : words) {
                    writer.write(word.toLine());
                    writer.write('\n');
                }
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp); // chỉ còn nếu có lỗi giữa chừng
        }
    }
}
