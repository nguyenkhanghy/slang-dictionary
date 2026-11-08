package com.slangdict.ui;

import com.slangdict.ui.components.Theme;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Cửa sổ nhỏ "Đang tải từ điển…" hiển thị ngay khi khởi động, trong lúc dữ liệu được mở ở luồng nền. */
final class LoadingWindow extends JFrame {

    private final LoadingPanel panel = new LoadingPanel();

    /** @param onCloseRequested gọi khi người dùng đóng cửa sổ này trong lúc đang tải (cửa sổ không tự đóng) */
    LoadingWindow(Runnable onCloseRequested) {
        super(Messages.APP_NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseRequested.run();
            }
        });
        setContentPane(panel);
        setResizable(false);
        setSize(Theme.loadingWindowSize());
        setLocationRelativeTo(null);
    }

    void showClosing() {
        panel.showClosing();
    }
}