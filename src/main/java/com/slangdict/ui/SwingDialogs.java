package com.slangdict.ui;

import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.awt.Component;

/** Cài đặt {@link Dialogs} bằng {@link JOptionPane}; chữ trong hộp thoại bọc dòng bằng {@link WrappedText}. */
final class SwingDialogs implements Dialogs {

    @Override
    public boolean confirm(Component parent, String title, String message, String yesLabel, String noLabel) {
        Object[] options = {yesLabel, noLabel};
        int choice = JOptionPane.showOptionDialog(parent, content(message), title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, noLabel);
        return choice == 0;
    }

    @Override
    public DuplicateChoice askDuplicate(Component parent, String slang) {
        Object[] options = {Messages.DUPLICATE_OVERWRITE, Messages.DUPLICATE_ADD, Messages.DUPLICATE_CANCEL};
        int choice = JOptionPane.showOptionDialog(parent, content(Messages.duplicateMessage(slang)),
                Messages.DUPLICATE_TITLE, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                Messages.DUPLICATE_CANCEL);
        return switch (choice) {
            case 0 -> DuplicateChoice.OVERWRITE;
            case 1 -> DuplicateChoice.ADD_DUPLICATE;
            default -> DuplicateChoice.CANCEL;
        };
    }

    @Override
    public void error(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, content(message), title, JOptionPane.ERROR_MESSAGE);
    }

    private static JComponent content(String message) {
        return new WrappedText(message, Theme.FONT_BODY, Theme.TEXT, Theme.DIALOG_TEXT_WIDTH);
    }
}
