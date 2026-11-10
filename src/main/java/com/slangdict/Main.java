package com.slangdict;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.slangdict.ui.AppLauncher;
import com.slangdict.ui.Messages;

/**
 * Điểm vào của ứng dụng: mở giao diện Swing ({@link AppLauncher}).
 *
 * <p>Tham số dòng lệnh không được dùng và bị bỏ qua. Nếu môi trường không có đồ họa (headless) thì in thông báo
 * rõ ràng ra stderr và thoát với mã 1.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AppLauncher.configurePlatform();
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(Messages.HEADLESS);
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(AppLauncher::start);
    }
}