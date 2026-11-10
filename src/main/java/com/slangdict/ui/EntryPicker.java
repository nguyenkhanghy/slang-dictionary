package com.slangdict.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.slangdict.model.SearchType;
import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.QuizService;
import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.RoundedPanel;
import com.slangdict.ui.components.SearchField;
import com.slangdict.ui.components.SlimScrollBarUI;
import com.slangdict.ui.components.Theme;

/**
 * Bộ chọn entry cho màn Sửa/Xóa: ô tìm theo từ lóng (khớp chính xác, KHÔNG ghi history), bảng kết quả và việc chọn một dòng.
 * Mọi lời gọi service chạy trên luồng nền chung; gõ/tìm liên tiếp thì kết quả cuối thắng ({@link LatestTask}).
 */
final class EntryPicker extends JPanel {

    private static final String CARD_STATE = "state";
    private static final String CARD_TABLE = "table";

    /** Kết quả một lượt tra, dựng sẵn trên luồng nền. */
    private record Outcome(String query, List<SlangWord> words, String[] meanings) {
    }

    private final DictionaryService service;
    private final String idleDetail;
    private final SearchField field = new SearchField(Messages.searchPlaceholder(SearchType.SLANG), Messages.SEARCH_CLEAR);
    private final JLabel countLabel = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final StateCard stateCard = new StateCard();
    private final ResultTable table = new ResultTable();
    private final AtomicReference<String> query = new AtomicReference<>("");
    private final LatestTask<Outcome> task;
    private Runnable onSelectionChange = () -> { };

    /** @param idleDetail câu hướng dẫn khi chưa tìm (khác nhau giữa màn Sửa và màn Xóa) */
    EntryPicker(DictionaryService service, String idleDetail) {
        super(new BorderLayout());
        this.service = service;
        this.idleDetail = idleDetail;
        setOpaque(false);
        task = new LatestTask<>("lookup slang", this::lookup, this::showOutcome, this::showError);

        ActionButton searchButton = new ActionButton(Messages.SEARCH_BUTTON);
        field.addActionListener(e -> submit());
        field.setOnClear(this::reset);
        searchButton.addActionListener(e -> submit());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChange.run();
            }
        });

        add(buildTop(searchButton), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        reset();
    }

    /** Entry đang được chọn trong bảng (rỗng nếu chưa chọn dòng nào). */
    Optional<SlangWord> selected() {
        int row = table.getSelectedRow();
        return row < 0 || row >= table.getRowCount() ? Optional.empty() : Optional.of(table.wordAt(row));
    }

    void setOnSelectionChange(Runnable onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    /** Đặt {@code text} vào ô tìm và tra ngay (dùng sau khi sửa để hiện lại đúng entry). */
    void searchFor(String text) {
        field.setText(text);
        submit();
    }

    /** Tra lại từ khóa hiện tại để bảng phản ánh dữ liệu mới nhất (không ghi history). */
    void refresh() {
        if (!query.get().isEmpty()) {
            task.request();
        }
    }

    /** Về trạng thái "chưa tìm"; giữ chữ trong ô tìm. */
    void reset() {
        query.set("");
        clearTable();
        countLabel.setText(" ");
        stateCard.show(Messages.PICKER_IDLE_TITLE, idleDetail, false);
        cards.show(body, CARD_STATE);
        onSelectionChange.run();
    }

    /** Làm rỗng bảng (và bỏ lựa chọn) mỗi khi hiện thẻ trạng thái: bảng bị ẩn không được còn dòng đang chọn. */
    private void clearTable() {
        table.showResults(List.of(), new String[0]);
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JComponent buildTop(ActionButton searchButton) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_S, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(searchButton, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));
        countLabel.setFont(Theme.FONT_BODY);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        countLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(row);
        top.add(Box.createVerticalStrut(Theme.SPACE_M)); // như SearchPage: ô tìm → dòng đếm cách SPACE_M
        top.add(countLabel);
        top.add(Box.createVerticalStrut(Theme.SPACE_S));
        return top;
    }

    private JComponent buildBody() {
        body.setOpaque(false);
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
        return body;
    }

    // ------------------------------------------------------------------ tra cứu

    private void submit() {
        String text = field.getText().strip();
        if (text.isEmpty()) {
            query.set("");
            clearTable();
            countLabel.setText(" ");
            stateCard.show(Messages.SEARCH_EMPTY_INPUT_TITLE, Messages.SEARCH_EMPTY_INPUT_DETAIL, false);
            cards.show(body, CARD_STATE);
            onSelectionChange.run();
            return;
        }
        query.set(text);
        task.request();
    }

    /** Chạy trên luồng nền; đọc từ khóa mới nhất. */
    private Outcome lookup() {
        String text = query.get();
        List<SlangWord> words = new ArrayList<>(service.lookupSlang(text));
        String[] meanings = new String[words.size()];
        for (int i = 0; i < meanings.length; i++) {
            meanings[i] = QuizService.meaningText(words.get(i));
        }
        return new Outcome(text, words, meanings);
    }

    private void showOutcome(Outcome outcome) {
        if (query.get().isEmpty()) {
            return; // người dùng đã xóa ô trong lúc chờ
        }
        if (outcome.words().isEmpty()) {
            clearTable();
            countLabel.setText(" ");
            stateCard.show(Messages.notFoundTitle(outcome.query()), Messages.notFoundDetail(SearchType.SLANG), false);
            cards.show(body, CARD_STATE);
        } else {
            table.showResults(outcome.words(), outcome.meanings());
            countLabel.setText(Messages.resultCount(outcome.words().size()));
            cards.show(body, CARD_TABLE);
        }
        onSelectionChange.run();
    }

    private void showError(Throwable cause) {
        clearTable();
        countLabel.setText(" ");
        stateCard.show(Messages.SEARCH_ERROR_TITLE, Messages.SEARCH_ERROR_DETAIL, true);
        cards.show(body, CARD_STATE);
        onSelectionChange.run();
    }
}