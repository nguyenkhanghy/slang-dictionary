package com.slangdict.ui;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.Theme;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * Cửa sổ chính (kích thước mặc định/tối thiểu theo {@link Theme}). Nội dung nằm ở {@link MainPanel}.
 * Đóng cửa sổ → {@code service.close()} rồi thoát (close() idempotent; shutdown hook ở AppLauncher là lưới an toàn).
 */
public final class MainFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());

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
                service.close();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}