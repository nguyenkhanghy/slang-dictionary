package com.slangdict.repository;

import com.slangdict.util.AppPaths;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Khởi tạo file dữ liệu cho lần chạy đầu tiên: nếu chưa có {@code slang.current.txt}
 * thì sao chép từ {@code slang.original.txt}. File gốc không bao giờ bị sửa.
 */
public final class DataBootstrap {

    private DataBootstrap() {
    }

    public static void ensureCurrentFile() throws IOException {
        Path original = AppPaths.originalFile();
        Path current = AppPaths.currentFile();

        Files.createDirectories(AppPaths.dataDir());
        if (Files.exists(current)) {
            return;
        }
        if (!Files.exists(original)) {
            throw new FileNotFoundException("Original data file not found: " + original);
        }
        Files.copy(original, current);
    }

    /** Sao chép {@code source} đè lên {@code target} một cách an toàn (file tạm + move). */
    public static void replaceWithCopy(Path source, Path target) throws IOException {
        AtomicFiles.write(target, out -> Files.copy(source, out));
    }
}
