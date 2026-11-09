package com.slangdict.ui;

import com.slangdict.model.SearchType;
import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.QuizService;
import com.slangdict.ui.components.ActionButton;
import com.slangdict.ui.components.RoundedPanel;
import com.slangdict.ui.components.SearchField;
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
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Màn tìm kiếm dùng chung cho F1 (theo từ lóng) và F2 (theo nghĩa); {@link SearchType} quyết định gọi hàm service nào.
 *
 * <p>Tìm chạy trên {@link SwingWorker} nhưng được đẩy vào {@link ServiceExecutor} (một luồng chung) nên không bao giờ có
 * hai thao tác service chạy cùng lúc. Nếu người dùng tìm tiếp khi một lượt đang chạy thì lượt đó được nhớ lại (chỉ giữ
 * yêu cầu mới nhất) và chạy ngay khi lượt hiện tại xong; kết quả của lượt cũ bị bỏ. Mọi trạng thái của trang (chữ trong ô,
 * kết quả) được giữ nguyên khi chuyển sang trang khác rồi quay lại, vì {@code CardLayout} giữ nguyên component.
 * Trang này không tự lọc/so khớp chữ: luôn nhờ service.
 */
final class SearchPage extends JPanel implements DataAware {

    private static final Logger LOG = Logger.getLogger(SearchPage.class.getName());

    /** Chỉ hiện "Đang tìm…" khi lượt tìm kéo dài quá mức này, để lượt tìm nhanh không làm màn hình nhấp nháy. */
    private static final int BUSY_DELAY_MS = 150;
    private static final String CARD_STATE = "state";
    private static final String CARD_RESULTS = "results";

    private final SearchType type;
    private final DictionaryService service;
    private final SearchField field;
    private final ActionButton searchButton = new ActionButton(Messages.SEARCH_BUTTON);
    private final JLabel countLabel = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JPanel stateContent = new JPanel();
    private final ResultTable table = new ResultTable();
    private final Timer busyTimer = new Timer(BUSY_DELAY_MS, e -> showBusy());

    private boolean running;
    private String queued;       // yêu cầu mới nhất nhận được khi đang có lượt chạy (null nếu không có)
    private String busyQuery = "";
    private int generation;      // tăng mỗi khi người dùng xóa ô: kết quả của lượt cũ bị bỏ

    SearchPage(SearchType type, DictionaryService service) {
        super(new BorderLayout());
        this.type = type;
        this.service = service;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        field = new SearchField(Messages.searchPlaceholder(type), Messages.SEARCH_CLEAR);
        field.addActionListener(e -> submit());
        field.setOnClear(this::clear);
        searchButton.addActionListener(e -> submit());
        busyTimer.setRepeats(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        showIdle();
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JComponent buildHeader() {
        Page page = type == SearchType.SLANG ? Page.SEARCH_SLANG : Page.SEARCH_DEFINITION;
        JLabel title = new JLabel(Messages.pageTitle(page));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);
        WrappedText description = new WrappedText(Messages.pageDescription(page), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);

        JPanel searchRow = new JPanel(new BorderLayout(Theme.SPACE_S, 0));
        searchRow.setOpaque(false);
        searchRow.add(field, BorderLayout.CENTER);
        searchRow.add(searchButton, BorderLayout.EAST);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));

        countLabel.setFont(Theme.FONT_BODY);
        countLabel.setForeground(Theme.TEXT_MUTED);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{title, description, searchRow, countLabel}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        header.add(title);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        header.add(description);
        header.add(Box.createVerticalStrut(Theme.SPACE_L));
        header.add(searchRow);
        header.add(Box.createVerticalStrut(Theme.SPACE_M));
        header.add(countLabel);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        return header;
    }

    private JComponent buildBody() {
        body.setOpaque(false);

        stateContent.setOpaque(false);
        stateContent.setLayout(new BoxLayout(stateContent, BoxLayout.Y_AXIS));
        stateContent.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L));
        RoundedPanel stateCard = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        stateCard.add(stateContent, BorderLayout.CENTER);
        JPanel stateHolder = new JPanel(new BorderLayout());
        stateHolder.setOpaque(false);
        stateHolder.add(stateCard, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        SlimScrollBarUI.apply(scroll);
        scroll.getViewport().setBackground(Theme.SURFACE);
        JPanel padding = new JPanel(new BorderLayout());
        padding.setOpaque(false);
        padding.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS));
        padding.add(scroll, BorderLayout.CENTER);
        RoundedPanel resultsCard = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        resultsCard.add(padding, BorderLayout.CENTER);

        body.add(stateHolder, CARD_STATE);
        body.add(resultsCard, CARD_RESULTS);
        return body;
    }

    // ------------------------------------------------------------------ hành động của người dùng

    private void submit() {
        search(field.getText());
    }

    /** Tìm {@code text}. Đang có lượt chạy thì chỉ nhớ yêu cầu mới nhất. Ô rỗng: hiện gợi ý, KHÔNG gọi service. */
    private void search(String text) {
        String query = text == null ? "" : text.strip();
        if (running) {
            queued = query;
            return;
        }
        if (query.isEmpty()) {
            showState(Messages.SEARCH_EMPTY_INPUT_TITLE, Messages.SEARCH_EMPTY_INPUT_DETAIL, false);
            return;
        }
        running = true;
        busyQuery = query;
        busyTimer.restart();
        ServiceExecutor.shared().execute(new SearchWorker(query, generation));
    }

    /** Nút ×: về trạng thái "chưa tìm" (không ghi history); kết quả của lượt đang chạy (nếu có) sẽ bị bỏ. */
    private void clear() {
        generation++;
        queued = null;
        busyTimer.stop();
        searchButton.setEnabled(true);
        showIdle();
    }

    /**
     * Dữ liệu từ điển vừa đổi (thêm/sửa/xóa/reset ở trang khác): kết quả đang hiện có thể đã cũ nên bỏ đi và về trạng
     * thái "chưa tìm" (giữ nguyên chữ trong ô). Không tìm lại tự động vì mỗi lần tìm sẽ ghi thêm vào lịch sử.
     */
    @Override
    public void dataChanged() {
        generation++;
        queued = null;
        busyTimer.stop();
        searchButton.setEnabled(true);
        showIdle();
    }

    // ------------------------------------------------------------------ kết thúc một lượt tìm

    private void onSearchDone(SearchWorker worker) {
        running = false;
        busyTimer.stop();
        searchButton.setEnabled(true);
        String next = queued;
        queued = null;
        if (next != null) {
            search(next); // có yêu cầu mới hơn: bỏ kết quả của lượt này
            return;
        }
        if (worker.generation != generation) {
            return;
        }
        try {
            showOutcome(worker.get());
        } catch (ExecutionException e) {
            LOG.log(Level.WARNING, "Search failed (" + type + "): " + e.getCause(), e.getCause());
            showState(Messages.SEARCH_ERROR_TITLE, Messages.SEARCH_ERROR_DETAIL, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showState(Messages.SEARCH_ERROR_TITLE, Messages.SEARCH_ERROR_DETAIL, true);
        }
    }

    private void showOutcome(Outcome outcome) {
        if (outcome.words().isEmpty()) {
            showState(Messages.notFoundTitle(outcome.query()), Messages.notFoundDetail(type), false);
            return;
        }
        table.showResults(outcome.words(), outcome.meanings());
        countLabel.setText(Messages.resultCount(outcome.words().size()));
        cards.show(body, CARD_RESULTS);
    }

    // ------------------------------------------------------------------ các trạng thái hiển thị

    private void showIdle() {
        showState(Messages.searchIdleTitle(type), Messages.searchIdleDetail(type), false);
    }

    private void showBusy() {
        if (running) {
            searchButton.setEnabled(false);
            showState(Messages.searchingTitle(busyQuery), Messages.SEARCHING_DETAIL, false);
        }
    }

    private void showState(String title, String detail, boolean error) {
        stateContent.removeAll();
        WrappedText titleText = new WrappedText(title, Theme.FONT_BODY_BOLD,
                error ? Theme.DANGER : Theme.TEXT, Theme.STATE_TEXT_MAX_WIDTH);
        WrappedText detailText = new WrappedText(detail, Theme.FONT_BODY, Theme.TEXT_MUTED, Theme.STATE_TEXT_MAX_WIDTH);
        titleText.setAlignmentX(LEFT_ALIGNMENT);
        detailText.setAlignmentX(LEFT_ALIGNMENT);
        stateContent.add(titleText);
        stateContent.add(Box.createVerticalStrut(Theme.SPACE_XS));
        stateContent.add(detailText);
        stateContent.revalidate();
        stateContent.repaint();
        countLabel.setText(" ");
        cards.show(body, CARD_STATE);
    }

    // ------------------------------------------------------------------ luồng nền

    /** Kết quả một lượt tìm, dựng sẵn trên luồng nền để EDT chỉ việc hiển thị. */
    private record Outcome(String query, List<SlangWord> words, String[] meanings) {
    }

    private final class SearchWorker extends SwingWorker<Outcome, Void> {

        private final String query;
        private final int generation;

        SearchWorker(String query, int generation) {
            this.query = query;
            this.generation = generation;
        }

        @Override
        protected Outcome doInBackground() {
            List<SlangWord> found = type == SearchType.SLANG
                    ? service.searchBySlang(query)
                    : service.searchByDefinition(query);
            List<SlangWord> words = new ArrayList<>(found); // bản sao riêng, không giữ view của service
            String[] meanings = new String[words.size()];
            for (int i = 0; i < meanings.length; i++) {
                meanings[i] = QuizService.meaningText(words.get(i));
            }
            return new Outcome(query, words, meanings);
        }

        @Override
        protected void done() {
            onSearchDone(this);
        }
    }
}
