package com.slangdict;

import java.util.List;

import com.slangdict.model.SlangWord;
import com.slangdict.repository.DataBootstrap;
import com.slangdict.repository.SlangFileReader;
import com.slangdict.util.AppPaths;

/**
 * Điểm vào của ứng dụng.
 *
 * <p>TẠM THỜI: chỉ nạp dữ liệu và in thống kê để kiểm tra Phase 0-1 (build jar + load file).
 * Sẽ được thay bằng việc mở MainFrame ở Phase 5.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        DataBootstrap.ensureCurrentFile();

        long start = System.nanoTime();
        List<SlangWord> words = SlangFileReader.read(AppPaths.currentFile());
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Data dir : " + AppPaths.dataDir());
        System.out.println("Loaded   : " + words.size() + " entries in " + millis + " ms");
        if (!words.isEmpty()) {
            System.out.println("First    : " + words.get(0));
            System.out.println("Last     : " + words.get(words.size() - 1));
        }
    }
}
