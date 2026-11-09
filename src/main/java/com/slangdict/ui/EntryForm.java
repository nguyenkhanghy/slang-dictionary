package com.slangdict.ui;

import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.SearchField;
import com.slangdict.ui.components.Theme;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Biểu mẫu một hàng để nhập một từ lóng: ô "Từ lóng", ô "Nghĩa" (nhiều nghĩa ngăn bằng '|') và nút gửi. Enter trong một
 * trong hai ô cũng gửi. Không tự kiểm tra hay gọi service: trang chứa nó làm việc đó.
 */
final class EntryForm extends JPanel {

    private final SearchField slangField = new SearchField(Messages.FORM_SLANG_HINT, Messages.SEARCH_CLEAR);
    private final SearchField meaningsField = new SearchField(Messages.FORM_MEANINGS_HINT, Messages.SEARCH_CLEAR);
    private final ActionButton submitButton;
    private Runnable onSubmit = () -> { };

    EntryForm(String submitLabel) {
        super(new BorderLayout(Theme.SPACE_S, 0));
        setOpaque(false);
        submitButton = new ActionButton(submitLabel);

        JPanel slangHolder = new JPanel(new BorderLayout());
        slangHolder.setOpaque(false);
        slangHolder.setPreferredSize(new Dimension(Theme.FORM_SLANG_FIELD_WIDTH, Theme.INPUT_HEIGHT));
        slangHolder.add(slangField, BorderLayout.CENTER);
        add(slangHolder, BorderLayout.WEST);
        add(meaningsField, BorderLayout.CENTER);
        add(submitButton, BorderLayout.EAST);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));

        slangField.addActionListener(e -> onSubmit.run());
        meaningsField.addActionListener(e -> onSubmit.run());
        submitButton.addActionListener(e -> onSubmit.run());
    }

    void setOnSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
    }

    String slang() {
        return slangField.getText();
    }

    /** Nội dung ô "Nghĩa" (chưa tách). */
    String meanings() {
        return meaningsField.getText();
    }

    void setValues(String slang, String meanings) {
        slangField.setText(slang);
        meaningsField.setText(meanings);
    }

    void clearValues() {
        setValues("", "");
    }

    void setFormEnabled(boolean enabled) {
        slangField.setEnabled(enabled);
        meaningsField.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }
}
