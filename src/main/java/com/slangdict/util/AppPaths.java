package com.slangdict.util;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Nơi DUY NHẤT định nghĩa đường dẫn các file dữ liệu. Không hard-code đường dẫn ở nơi khác.
 *
 * <p>Thư mục dữ liệu được tìm theo thứ tự:
 * <ol>
 *   <li>System property {@code -Dslang.dataDir=...} (dùng để test/debug);</li>
 *   <li>{@code ./data} (thư mục làm việc hiện tại) nếu trong đó có {@code slang.original.txt}
 *       -> tiện khi chạy từ IDE hoặc từ thư mục gốc repo;</li>
 *   <li>{@code data/} nằm cạnh file jar -> dùng khi giảng viên chạy {@code java -jar} hoặc double-click.</li>
 * </ol>
 */
public final class AppPaths {

    public static final String ORIGINAL_NAME = "slang.original.txt";
    public static final String CURRENT_NAME = "slang.current.txt";
    public static final String INDEX_NAME = "slang.index.bin";
    public static final String HISTORY_NAME = "history.txt";

    private static final Path DATA_DIR = resolveDataDir();

    private AppPaths() {
    }

    public static Path dataDir() {
        return DATA_DIR;
    }

    /** Dữ liệu gốc (đã làm sạch) - CHỈ ĐỌC, không bao giờ ghi đè. */
    public static Path originalFile() {
        return DATA_DIR.resolve(ORIGINAL_NAME);
    }

    /** Dữ liệu đang dùng - add/edit/delete ghi vào đây. */
    public static Path currentFile() {
        return DATA_DIR.resolve(CURRENT_NAME);
    }

    /** Index đã serialize. */
    public static Path indexFile() {
        return DATA_DIR.resolve(INDEX_NAME);
    }

    /** Lịch sử tìm kiếm (file riêng, theo yêu cầu GV). */
    public static Path historyFile() {
        return DATA_DIR.resolve(HISTORY_NAME);
    }

    private static Path resolveDataDir() {
        String override = System.getProperty("slang.dataDir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        Path cwdData = Paths.get("data").toAbsolutePath().normalize();
        if (Files.exists(cwdData.resolve(ORIGINAL_NAME))) {
            return cwdData;
        }
        return applicationDir().resolve("data").normalize();
    }

    /** Thư mục chứa file jar; nếu chạy từ IDE (thư mục classes) thì dùng thư mục làm việc. */
    private static Path applicationDir() {
        try {
            Path location = Paths.get(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location) && location.getParent() != null) {
                return location.getParent().toAbsolutePath();
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // rơi xuống thư mục làm việc
        }
        return Paths.get("").toAbsolutePath();
    }
}
