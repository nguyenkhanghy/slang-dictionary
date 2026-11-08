package com.slangdict;

import com.slangdict.dev.DevConsole;
import com.slangdict.service.DictionaryService;
import com.slangdict.util.AppPaths;

/**
 * Điểm vào của ứng dụng.
 *
 * <p>TẠM THỜI: mở dữ liệu, in thống kê và chạy menu console để thử F1–F7.
 * Sẽ được thay bằng việc mở MainFrame ở Phase 5.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        DictionaryService service = DictionaryService.open();
        // Ghi index khi thoát, kể cả khi bị Ctrl+C (close() an toàn khi gọi nhiều lần)
        Runtime.getRuntime().addShutdownHook(new Thread(service::close));

        System.out.println("Data dir : " + AppPaths.dataDir());
        System.out.println("Entries  : " + service.size());
        System.out.println("Index    : " + (service.isIndexFromCache() ? "loaded from cache" : "rebuilt from text")
                + " in " + service.getOpenMillis() + " ms");

        DevConsole.run(service);
    }
}
