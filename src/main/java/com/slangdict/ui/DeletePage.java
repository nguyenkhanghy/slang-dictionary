package com.slangdict.ui;

import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.Theme;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Optional;

/**
 * Màn F6: tìm một từ lóng, chọn một dòng trong kết quả rồi xóa dòng đó. Luôn hỏi xác nhận trước khi xóa (nút mặc định là
 * "Hủy"). Xóa chạy trên luồng nền; thành công thì bảng tra lại và {@link DataEvents} báo các trang khác.
 */
final class DeletePage extends JPanel implements DataAware {

    private record Outcome(boolean removed, int size) {
    }

    private final DictionaryService service;
    private final DataEvents events;
    private final Dialogs dialogs;
    private final EntryPicker picker;
    private final ActionButton deleteButton = new ActionButton(Messages.DELETE_BUTTON);
    private final StatusLine status = new StatusLine();
    private boolean busy;

    DeletePage(DictionaryService service, DataEvents events, Dialogs dialogs) {
        super(new BorderLayout());
        this.service = service;
        this.events = events;
        this.dialogs = dialogs;
        this.picker = new EntryPicker(service, Messages.DELETE_IDLE_DETAIL);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        picker.setOnSelectionChange(this::onSelectionChanged);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> delete());

        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.add(deleteButton, BorderLayout.WEST);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        buttonRow.setAlignmentX(LEFT_ALIGNMENT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        bottom.add(Box.createVerticalStrut(Theme.SPACE_M));
        bottom.add(buttonRow);
        bottom.add(Box.createVerticalStrut(Theme.SPACE_S));
        bottom.add(status);

        add(PageHeader.create(Page.DELETE), BorderLayout.NORTH);
        add(picker, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    public void dataChanged() {
        picker.reset();
        status.clear();
    }

    private void onSelectionChanged() {
        boolean selected = picker.selected().isPresent();
        deleteButton.setEnabled(selected && !busy);
        if (selected) {
            status.clear();
        }
    }

    private void delete() {
        Optional<SlangWord> target = picker.selected();
        if (busy || target.isEmpty()) {
            return;
        }
        String slang = target.get().getSlang();
        if (!dialogs.confirm(this, Messages.DELETE_CONFIRM_TITLE, Messages.deleteConfirmMessage(slang),
                Messages.DELETE_CONFIRM_YES, Messages.DELETE_CONFIRM_NO)) {
            return;
        }
        setBusy(true);
        BackgroundCall.run("delete slang", () -> {
            boolean removed = service.deleteSlang(target.get());
            return new Outcome(removed, service.size());
        }, outcome -> onDeleted(slang, outcome), this::onFailure);
    }

    private void onDeleted(String slang, Outcome outcome) {
        setBusy(false);
        if (outcome.removed()) {
            status.showSuccess(Messages.deletedMessage(slang));
            events.fire(outcome.size(), this);
        } else {
            status.showError(Messages.DELETE_NOT_FOUND);
        }
        picker.refresh();
    }

    private void onFailure(Throwable cause) {
        setBusy(false);
        status.showError(Messages.SAVE_ERROR_TITLE);
        dialogs.error(this, Messages.SAVE_ERROR_TITLE, Messages.SAVE_ERROR_DETAIL);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        deleteButton.setEnabled(!busy && picker.selected().isPresent());
    }
}
