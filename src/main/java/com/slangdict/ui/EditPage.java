package com.slangdict.ui;

import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.EditResult;
import com.slangdict.service.InputValidator;
import com.slangdict.service.QuizService;
import com.slangdict.service.ValidationException;
import com.slangdict.ui.components.Theme;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

/**
 * Màn F5: tìm một từ lóng, chọn một dòng trong kết quả rồi sửa từ lóng và/hoặc nghĩa của dòng đó. Biểu mẫu chỉ mở khi có
 * dòng được chọn và được điền sẵn nội dung hiện tại. Dữ liệu nhập được kiểm tra trên EDT; ghi chạy trên luồng nền. Sau khi sửa
 * thành công, bảng tra lại theo tên mới và {@link DataEvents} báo các trang khác.
 */
final class EditPage extends JPanel implements DataAware {

    private record Outcome(EditResult result, int size) {
    }

    private final DictionaryService service;
    private final DataEvents events;
    private final Dialogs dialogs;
    private final EntryPicker picker;
    private final EntryForm form = new EntryForm(Messages.EDIT_SAVE_BUTTON);
    private final StatusLine status = new StatusLine();
    private boolean busy;

    EditPage(DictionaryService service, DataEvents events, Dialogs dialogs) {
        super(new BorderLayout());
        this.service = service;
        this.events = events;
        this.dialogs = dialogs;
        this.picker = new EntryPicker(service, Messages.EDIT_IDLE_DETAIL);
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        picker.setOnSelectionChange(this::onSelectionChanged);
        form.setOnSubmit(this::submit);
        form.setFormEnabled(false);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        form.setAlignmentX(LEFT_ALIGNMENT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        bottom.add(Box.createVerticalStrut(Theme.SPACE_M));
        bottom.add(form);
        bottom.add(Box.createVerticalStrut(Theme.SPACE_S));
        bottom.add(status);

        add(PageHeader.create(Page.EDIT), BorderLayout.NORTH);
        add(picker, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Dữ liệu vừa đổi ở trang khác: kết quả đang chọn có thể đã cũ nên về trạng thái "chưa tìm". */
    @Override
    public void dataChanged() {
        picker.reset();
        status.clear();
    }

    private void onSelectionChanged() {
        Optional<SlangWord> selected = picker.selected();
        if (selected.isPresent()) {
            form.setValues(selected.get().getSlang(), QuizService.meaningText(selected.get()));
            form.setFormEnabled(!busy);
            status.clear();
        } else {
            form.clearValues();
            form.setFormEnabled(false);
        }
    }

    private void submit() {
        Optional<SlangWord> target = picker.selected();
        if (busy || target.isEmpty()) {
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
        busy = true;
        form.setFormEnabled(false);
        BackgroundCall.run("edit slang", () -> {
            EditResult result = service.editSlang(target.get(), slang, meanings);
            return new Outcome(result, service.size());
        }, outcome -> onSaved(slang, outcome), this::onFailure);
    }

    private void onSaved(String slang, Outcome outcome) {
        busy = false;
        switch (outcome.result()) {
            case EDITED -> {
                status.showSuccess(Messages.editedMessage(slang));
                events.fire(outcome.size(), this);
                picker.searchFor(slang);
            }
            case UNCHANGED -> {
                form.setFormEnabled(true);
                status.showNote(Messages.EDIT_UNCHANGED);
            }
            case NOT_FOUND -> {
                status.showError(Messages.EDIT_NOT_FOUND);
                picker.refresh();
            }
            case CONFLICT -> {
                form.setFormEnabled(true);
                status.showError(Messages.editConflictMessage(slang));
            }
        }
    }

    private void onFailure(Throwable cause) {
        busy = false;
        form.setFormEnabled(picker.selected().isPresent());
        if (cause instanceof ValidationException e) {
            status.showError(Messages.validationMessage(e.getReason()));
            return;
        }
        status.showError(Messages.SAVE_ERROR_TITLE);
        dialogs.error(this, Messages.SAVE_ERROR_TITLE, Messages.SAVE_ERROR_DETAIL);
    }
}
