package com.slangdict;

import com.slangdict.dev.DevConsole;
import com.slangdict.service.DictionaryService;
import com.slangdict.ui.AppLauncher;
import com.slangdict.ui.Messages;
import com.slangdict.util.AppPaths;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Điểm vào của ứng dụng.
 *
 * <p>Mặc định mở giao diện Swing ({@link AppLauncher}). Tham số {@code --console} mở menu console TẠM THỜI
 * (để thử tay các chức năng); sẽ bị xóa cùng {@code dev/DevConsole} khi U4 xong.
 */
public final class Main {

    private static final String CONSOLE_FLAG = "--console";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains(CONSOLE_FLAG)) {
            runConsole();
            return;
        }
        AppLauncher.configurePlatform();
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(Messages.HEADLESS);
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(AppLauncher::start);
    }

    /** TẠM THỜI: hành vi cũ của Phase 0–4 (mở dữ liệu, in thống kê, chạy menu console). */
    private static void runConsole() throws Exception {
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