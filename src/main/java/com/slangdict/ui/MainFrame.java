package com.slangdict.ui;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.Theme;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cửa sổ chính (kích thước mặc định/tối thiểu theo {@link Theme}). Nội dung nằm ở {@link MainPanel}.
 * Đóng cửa sổ → {@code service.close()} rồi thoát (close() idempotent; shutdown hook ở AppLauncher là lưới an toàn).
 */
public final class MainFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());

    /** Chờ tối đa chừng này để các thao tác ghi đang chờ xong và index được lưu; quá hạn thì vẫn thoát (index sẽ tự build lại). */
    private static final int CLOSE_TIMEOUT_SECONDS = 10;

    public MainFrame(DictionaryService service) {
        super(Messages.APP_NAME);
        setContentPane(new MainPanel(service));

        setMinimumSize(Theme.windowMinimumSize());
        setSize(Theme.windowDefaultSize());
        setLocationRelativeTo(null);

        // Listener chạy trước thao tác mặc định EXIT_ON_CLOSE (JFrame xử lý listener rồi mới thoát).
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOG.info("Main window closing, saving index if needed");
                closeAfterPendingWrites(service);
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Gọi {@code service.close()} trên luồng service chung, SAU mọi thao tác đã xếp hàng (thêm/sửa/xóa/reset đang chạy
     * nền), vì service không thread-safe. Chờ xong rồi mới trả về để cửa sổ không đóng giữa chừng.
     */
    static void closeAfterPendingWrites(DictionaryService service) {
        FutureTask<Void> closing = new FutureTask<>(service::close, null);
        ServiceExecutor.shared().execute(closing);
        try {
            closing.get(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Interrupted while waiting for the dictionary to close: " + e, e);
        } catch (ExecutionException | TimeoutException e) {
            LOG.log(Level.WARNING, "Dictionary did not close cleanly (index will be rebuilt next time): " + e, e);
        }
    }
}