package com.slangdict.ui;

import com.slangdict.service.ValidationException;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Một lời gọi service chạy MỘT LẦN trên luồng nền chung ({@link ServiceExecutor}), kết quả về EDT. Khác {@link LatestTask}:
 * mỗi lời gọi đều được chạy (không bị gộp), dùng cho thao tác ghi (thêm, sửa, xóa, reset) mà không được bỏ sót.
 * Lỗi được log (dữ liệu người dùng không hợp lệ chỉ log mức thấp) rồi báo qua {@code onFailure}.
 */
final class BackgroundCall {

    private static final Logger LOG = Logger.getLogger(BackgroundCall.class.getName());

    private BackgroundCall() {
    }

    static <T> void run(String name, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        ServiceExecutor.shared().execute(new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    fail(name, e.getCause(), onFailure);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail(name, e, onFailure);
                }
            }
        });
    }

    private static void fail(String name, Throwable cause, Consumer<Throwable> onFailure) {
        Level level = cause instanceof ValidationException ? Level.FINE : Level.WARNING;
        LOG.log(level, "Background call failed (" + name + "): " + cause, cause);
        onFailure.accept(cause);
    }
}
