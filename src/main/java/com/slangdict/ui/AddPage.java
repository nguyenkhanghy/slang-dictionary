package com.slangdict.ui;

import com.slangdict.service.AddResult;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.DuplicatePolicy;
import com.slangdict.service.InputValidator;
import com.slangdict.service.ValidationException;
import com.slangdict.ui.components.Theme;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Màn F4: thêm một từ lóng mới. Dữ liệu được kiểm tra ngay trên EDT bằng {@link InputValidator} (báo lỗi tại chỗ, không gọi
 * service); hợp lệ thì ghi trên luồng nền. Nếu khóa đã tồn tại thì hỏi: ghi đè, thêm bản trùng hoặc hủy. Thành công thì xóa
 * biểu mẫu, hiện thông báo và báo {@link DataEvents} để sidebar và các trang khác cập nhật.
 */
final class AddPage extends JPanel {

    private record Outcome(AddResult result, int size) {
    }

    private final DictionaryService service;
    private final DataEvents events;
    private final Dialogs dialogs;
    private final EntryForm form = new EntryForm(Messages.ADD_BUTTON);
    private final StatusLine status = new StatusLine();
    private boolean busy;

    AddPage(DictionaryService service, DataEvents events, Dialogs dialogs) {
        super(new BorderLayout());
        this.service = service;
        this.events = events;
        this.dialogs = dialogs;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        form.setOnSubmit(this::submit);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        form.setAlignmentX(LEFT_ALIGNMENT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        column.add(form);
        column.add(Box.createVerticalStrut(Theme.SPACE_M));
        column.add(status);

        add(PageHeader.create(Page.ADD), BorderLayout.NORTH);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(column, BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);
    }

    private void submit() {
        if (busy) {
            return;
        }
        String slang;
        List<String> meanings;
        try {
            slang = InputValidator.validateSlang(form.slang());
            meanings = InputValidator.validateMeanings(InputValidator.parseMeanings(form.meanings()));
        } catch (ValidationException e) {
            status.showError(Messages.validationMessage(e.getReason()));
            return;
        }
        status.clear();
        write(slang, meanings, DuplicatePolicy.REJECT);
    }

    private void write(String slang, List<String> meanings, DuplicatePolicy policy) {
        setBusy(true);
        BackgroundCall.run("add slang", () -> {
            AddResult result = service.addSlang(slang, meanings, policy);
            return new Outcome(result, service.size());
        }, outcome -> onWritten(slang, meanings, outcome), this::onFailure);
    }

    private void onWritten(String slang, List<String> meanings, Outcome outcome) {
        setBusy(false);
        switch (outcome.result()) {
            case ADDED -> succeed(Messages.addedMessage(slang), outcome.size());
            case OVERWRITTEN -> succeed(Messages.overwrittenMessage(slang), outcome.size());
            case CONFLICT -> resolveDuplicate(slang, meanings);
        }
    }

    private void resolveDuplicate(String slang, List<String> meanings) {
        switch (dialogs.askDuplicate(this, slang)) {
            case OVERWRITE -> write(slang, meanings, DuplicatePolicy.OVERWRITE);
            case ADD_DUPLICATE -> write(slang, meanings, DuplicatePolicy.DUPLICATE);
            case CANCEL -> status.clear();
        }
    }

    private void succeed(String message, int entryCount) {
        status.showSuccess(message);
        form.clearValues();
        events.fire(entryCount, this);
    }

    private void onFailure(Throwable cause) {
        setBusy(false);
        if (cause instanceof ValidationException e) {
            status.showError(Messages.validationMessage(e.getReason()));
            return;
        }
        status.showError(Messages.SAVE_ERROR_TITLE);
        dialogs.error(this, Messages.SAVE_ERROR_TITLE, Messages.SAVE_ERROR_DETAIL);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        form.setFormEnabled(!busy);
    }
}
