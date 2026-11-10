package com.slangdict.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.slangdict.util.LogSupport;

/**
 * Chạy một việc nền (gọi service) qua {@link ServiceExecutor} và đưa kết quả về EDT, với quy tắc "yêu cầu mới nhất thắng":
 * chỉ một lượt chạy tại một thời điểm; {@link #request()} khi đang chạy chỉ nhớ rằng cần chạy lại, lượt đang chạy xong thì
 * kết quả của nó bị bỏ và lượt mới được chạy. Nhờ vậy bấm liên tiếp nhanh luôn cho ra kết quả của lần bấm cuối.
 * Mọi phương thức và callback chạy trên EDT. Lỗi của việc nền được log một dòng (WARNING, qua {@link LogSupport}) rồi báo qua {@code onFailure}.
 */
final class LatestTask<T> {

    private static final Logger LOG = Logger.getLogger(LatestTask.class.getName());

    private final String name;
    private final Callable<T> work;
    private final Consumer<T> onSuccess;
    private final Consumer<Throwable> onFailure;
    private boolean running;
    private boolean again;

    /** @param name tên việc, chỉ dùng trong log (tiếng Anh) */
    LatestTask(String name, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        this.name = name;
        this.work = work;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    /** Yêu cầu chạy việc nền (gọi trên EDT). */
    void request() {
        if (running) {
            again = true;
            return;
        }
        running = true;
        ServiceExecutor.shared().execute(new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                finish(this);
            }
        });
    }

    private void finish(SwingWorker<T, Void> worker) {
        running = false;
        if (again) {
            again = false;
            request(); // có yêu cầu mới hơn: bỏ kết quả của lượt này
            return;
        }
        try {
            onSuccess.accept(worker.get());
        } catch (ExecutionException e) {
            fail(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
    }

    private void fail(Throwable cause) {
        LogSupport.warn(LOG, "Background task failed (" + name + ")", cause);
        onFailure.accept(cause);
    }
}