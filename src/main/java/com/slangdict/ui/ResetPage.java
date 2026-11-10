package com.slangdict.ui;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.Theme;

/**
 * Màn F7: khôi phục từ điển về danh sách gốc (lịch sử tìm kiếm được giữ). Luôn hỏi xác nhận trước (nút mặc định là "Hủy").
 * Chạy trên luồng nền; thành công thì báo {@link DataEvents}. Không tìm thấy file gốc hoặc lỗi đọc/ghi thì báo lỗi và
 * từ điển không đổi (service đảm bảo).
 */
final class ResetPage extends JPanel {

    private final DictionaryService service;
    private final DataEvents events;
    private final Dialogs dialogs;
    private final ActionButton resetButton = ActionButton.danger(Messages.RESET_BUTTON);
    private final StatusLine status = new StatusLine();

    ResetPage(DictionaryService service, DataEvents events, Dialogs dialogs) {
        super(new BorderLayout());
        this.service = service;
        this.events = events;
        this.dialogs = dialogs;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));
        resetButton.addActionListener(e -> reset());

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.add(resetButton, BorderLayout.WEST);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        buttonRow.setAlignmentX(LEFT_ALIGNMENT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        column.add(buttonRow);
        column.add(Box.createVerticalStrut(Theme.SPACE_M));
        column.add(status);

        add(PageHeader.create(Page.RESET), BorderLayout.NORTH);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(column, BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);
    }

    private void reset() {
        if (!dialogs.confirm(this, Messages.RESET_CONFIRM_TITLE, Messages.RESET_CONFIRM_MESSAGE,
                Messages.RESET_CONFIRM_YES, Messages.RESET_CONFIRM_NO)) {
            return;
        }
        status.clear();
        resetButton.setEnabled(false);
        BackgroundCall.run("reset to original", () -> {
            service.resetToOriginal();
            return service.size();
        }, this::onReset, this::onFailure);
    }

    private void onReset(int entryCount) {
        resetButton.setEnabled(true);
        status.showSuccess(Messages.resetDoneMessage(entryCount));
        events.fire(entryCount, this);
    }

    private void onFailure(Throwable cause) {
        resetButton.setEnabled(true);
        if (cause instanceof FileNotFoundException) {
            status.showError(Messages.RESET_MISSING_ORIGINAL);
            return;
        }
        status.showError(Messages.RESET_ERROR);
        dialogs.error(this, Messages.RESET_CONFIRM_TITLE, Messages.RESET_ERROR);
    }
}