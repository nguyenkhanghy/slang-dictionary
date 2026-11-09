package com.slangdict.ui;

import com.slangdict.model.HistoryEntry;
import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.RoundedPanel;
import com.slangdict.ui.components.SlimScrollBarUI;
import com.slangdict.ui.components.Theme;
import com.slangdict.ui.components.WrappedText;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn F3: các lần tìm gần đây (mới nhất ở trên) dạng bảng "Thời gian | Loại | Từ khóa", và nút xóa lịch sử (có xác nhận).
 *
 * <p>Lịch sử được tải lại MỖI LẦN trang được hiện (vì F1/F2 vừa ghi thêm). Mọi lời gọi service chạy trên luồng nền chung
 * ({@link ServiceExecutor}) qua {@link LatestTask}; từ khóa dài tự xuống dòng, không cắt chữ.
 */
final class HistoryPage extends JPanel {

    private static final String CARD_BLANK = "blank";
    private static final String CARD_STATE = "state";
    private static final String CARD_TABLE = "table";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(Messages.HISTORY_TIME_PATTERN).withZone(ZoneId.systemDefault());

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final StateCard stateCard = new StateCard();
    private final WrapTable table = new WrapTable(List.of(
            new WrapTable.Column(Messages.COLUMN_TIME, Theme.FONT_BODY, Theme.TEXT_MUTED, Theme.HISTORY_TIME_COLUMN_WIDTH),
            new WrapTable.Column(Messages.COLUMN_TYPE, Theme.FONT_BODY, Theme.TEXT_MUTED, Theme.HISTORY_TYPE_COLUMN_WIDTH),
            new WrapTable.Column(Messages.COLUMN_QUERY, Theme.FONT_BODY_BOLD, Theme.TEXT, 0)));
    private final JLabel countLabel = new JLabel(" ");
    private final ActionButton clearButton = new ActionButton(Messages.HISTORY_CLEAR_BUTTON);
    private final LatestTask<List<String[]>> loadTask;
    private final LatestTask<List<String[]>> clearTask;
    private final Dialogs dialogs;

    /** {@code dialogs} dùng để hỏi xác nhận xóa (tách riêng để kiểm thử không cần hộp thoại thật). */
    HistoryPage(DictionaryService service, Dialogs dialogs) {
        super(new BorderLayout());
        this.dialogs = dialogs;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        loadTask = new LatestTask<>("load history", () -> loadRows(service), this::showRows, this::showError);
        clearTask = new LatestTask<>("clear history", () -> {
            service.clearHistory();
            return loadRows(service);
        }, this::showRows, this::showError);
        clearButton.setEnabled(false);
        clearButton.addActionListener(e -> {
            if (this.dialogs.confirm(this, Messages.HISTORY_CONFIRM_TITLE, Messages.HISTORY_CONFIRM_MESSAGE,
                    Messages.HISTORY_CONFIRM_YES, Messages.HISTORY_CONFIRM_NO)) {
                clearTask.request();
            }
        });

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        // CardLayout phát "shown" mỗi lần mục này được chọn: tải lại để thấy các lần tìm vừa rồi.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadTask.request();
            }
        });
    }

    /** Chạy trên luồng nền: dựng sẵn chuỗi hiển thị. */
    private static List<String[]> loadRows(DictionaryService service) {
        List<HistoryEntry> entries = service.getHistory();
        List<String[]> rows = new ArrayList<>(entries.size());
        for (HistoryEntry entry : entries) {
            rows.add(new String[]{TIME_FORMAT.format(entry.time()), Messages.historyType(entry.type()), entry.query()});
        }
        return rows;
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JComponent buildHeader() {
        JLabel title = new JLabel(Messages.pageTitle(Page.HISTORY));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);
        WrappedText description = new WrappedText(Messages.pageDescription(Page.HISTORY), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);
        countLabel.setFont(Theme.FONT_BODY);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_M, 0));
        row.setOpaque(false);
        row.add(countLabel, BorderLayout.CENTER);
        row.add(clearButton, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{title, description, row}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        header.add(title);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        header.add(description);
        header.add(Box.createVerticalStrut(Theme.SPACE_L));
        header.add(row);
        header.add(Box.createVerticalStrut(Theme.SPACE_M));
        return header;
    }

    private JComponent buildBody() {
        body.setOpaque(false);
        JPanel blank = new JPanel();
        blank.setOpaque(false);
        body.add(blank, CARD_BLANK);

        JPanel stateHolder = new JPanel(new BorderLayout());
        stateHolder.setOpaque(false);
        stateHolder.add(stateCard, BorderLayout.NORTH);
        body.add(stateHolder, CARD_STATE);

        JScrollPane scroll = new JScrollPane(table);
        SlimScrollBarUI.apply(scroll);
        scroll.getViewport().setBackground(Theme.SURFACE);
        JPanel padding = new JPanel(new BorderLayout());
        padding.setOpaque(false);
        padding.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS));
        padding.add(scroll, BorderLayout.CENTER);
        RoundedPanel tableCard = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        tableCard.add(padding, BorderLayout.CENTER);
        body.add(tableCard, CARD_TABLE);

        cards.show(body, CARD_BLANK);
        return body;
    }

    // ------------------------------------------------------------------ hiển thị (EDT)

    private void showRows(List<String[]> rows) {
        if (rows.isEmpty()) {
            stateCard.show(Messages.HISTORY_EMPTY_TITLE, Messages.historyEmptyDetail(), false);
            countLabel.setText(" ");
            clearButton.setEnabled(false);
            cards.show(body, CARD_STATE);
            return;
        }
        table.setRows(rows);
        countLabel.setText(Messages.historyCount(rows.size()));
        clearButton.setEnabled(true);
        cards.show(body, CARD_TABLE);
    }

    private void showError(Throwable cause) {
        stateCard.show(Messages.HISTORY_ERROR_TITLE, Messages.UNEXPECTED_ERROR_DETAIL, true);
        countLabel.setText(" ");
        cards.show(body, CARD_STATE);
    }
}
