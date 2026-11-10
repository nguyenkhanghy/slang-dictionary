package com.slangdict.ui;

import com.slangdict.util.LogSupport;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Lưới an toàn cuối cùng cho ngoại lệ không ai bắt (đặc biệt trên EDT): log MỘT dòng (stack trace chỉ khi debug, xem
 * {@link LogSupport}) và hiện hộp thoại lỗi chung tối đa MỘT lần mỗi lần chạy, để lỗi lặp lại không làm spam cửa sổ.
 * Chỉ ngoại lệ trên EDT mới hiện hộp thoại; luồng khác chỉ được log. Handler này tự nó không bao giờ ném ngoại lệ.
 *
 * <p>Lỗi của việc nền (service) không đi qua đây: {@link LatestTask}/{@link BackgroundCall} đã bắt và báo cho từng trang.
 */
final class GlobalErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalErrorHandler.class.getName());

    private final Dialogs dialogs;
    private final AtomicBoolean dialogShown = new AtomicBoolean();

    private GlobalErrorHandler(Dialogs dialogs) {
        this.dialogs = dialogs;
    }

    /** Đăng ký làm handler mặc định cho mọi luồng. Gọi một lần ở {@code AppLauncher.start()}. */
    static void install(Dialogs dialogs) {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalErrorHandler(dialogs));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        try {
            LogSupport.severe(LOG, "Uncaught exception in thread '" + thread.getName() + "'", error);
            if (SwingUtilities.isEventDispatchThread() && dialogShown.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(this::showDialog);
            }
        } catch (Throwable ignored) {
            // handler không được ném tiếp: không còn gì an toàn hơn để làm
        }
    }

    private void showDialog() {
        try {
            dialogs.error(null, Messages.ERROR_UNEXPECTED_TITLE, Messages.ERROR_UNEXPECTED_MESSAGE);
        } catch (Throwable ignored) {
            // không dựng được hộp thoại (vd. môi trường đồ họa hỏng): lỗi đã được log ở trên
        }
    }
}