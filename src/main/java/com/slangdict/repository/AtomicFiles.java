package com.slangdict.repository;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ghi file AN TOÀN: ghi ra file tạm cùng thư mục rồi đổi tên đè lên file đích,
 * nên nếu chương trình bị tắt giữa chừng thì file cũ vẫn nguyên vẹn.
 */
final class AtomicFiles {

    @FunctionalInterface
    interface Body {
        void write(OutputStream out) throws IOException;
    }

    private AtomicFiles() {
    }

    static void write(Path file, Body body) throws IOException {
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());

        Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp), 1 << 16)) {
                body.write(out);
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
