package com.slangdict.ui;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.Theme;
import com.slangdict.util.AppPaths;

/**
 * Khởi động giao diện: hiện cửa sổ tải ngay, mở {@link DictionaryService} ở luồng nền (không chặn EDT),
 * rồi dựng {@link MainFrame}. Xử lý lỗi mở dữ liệu bằng hộp thoại thân thiện và việc đóng cửa sổ khi đang tải.
 */
public final class AppLauncher {

    private static final Logger LOG = Logger.getLogger(AppLauncher.class.getName());

    private final LoadingWindow loading;
    private final Dialogs dialogs = new SwingDialogs();
    private boolean closeRequested; // chỉ đọc/ghi trên EDT

    private AppLauncher() {
        loading = new LoadingWindow(this::onLoadingClosed);
    }

    /** Đặt thuộc tính nền tảng (tên ứng dụng trên menu bar của macOS). Gọi trước khi AWT khởi tạo. */
    public static void configurePlatform() {
        System.setProperty("apple.awt.application.name", Messages.APP_NAME);
    }

    /** Dựng giao diện. Phải gọi trên EDT (qua {@code SwingUtilities.invokeLater}). */
    public static void start() {
        Theme.install();
        new AppLauncher().launch();
    }

    private void launch() {
        loading.setVisible(true);
        new OpenWorker().execute();
    }

    private void onLoadingClosed() {
        closeRequested = true;
        loading.showClosing();
    }

    private void fail(Throwable cause) {
        loading.dispose();
        LOG.log(Level.SEVERE, "Cannot open dictionary data: " + cause, cause);
        if (closeRequested) {
            System.exit(0);
        }
        String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        dialogs.error(null, Messages.ERROR_OPEN_TITLE, Messages.errorOpenMessage(dataDirText(), reason));
        System.exit(1);
    }

    private static String dataDirText() {
        try {
            return String.valueOf(AppPaths.dataDir());
        } catch (RuntimeException e) {
            return "?";
        }
    }

    private final class OpenWorker extends SwingWorker<DictionaryService, Void> {

        @Override
        protected DictionaryService doInBackground() throws Exception {
            DictionaryService service = DictionaryService.open();
            // Lưới an toàn cho Cmd+Q / Ctrl+C / tắt máy; close() idempotent nên gọi thêm ở MainFrame không sao.
            Runtime.getRuntime().addShutdownHook(new Thread(service::close, "dictionary-close"));
            return service;
        }

        @Override
        protected void done() {
            DictionaryService service;
            try {
                service = get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
                return;
            } catch (ExecutionException e) {
                fail(e.getCause() != null ? e.getCause() : e);
                return;
            }
            loading.dispose();
            if (closeRequested) {
                service.close();
                System.exit(0);
                return;
            }
            new MainFrame(service).setVisible(true);
        }
    }
}