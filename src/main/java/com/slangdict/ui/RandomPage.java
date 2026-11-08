package com.slangdict.ui;

import com.slangdict.model.SlangWord;
import com.slangdict.service.QuizService;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Optional;

/**
 * Màn F8: hiện một từ lóng ngẫu nhiên và nghĩa của nó, nút "Từ khác" để bốc lại.
 *
 * <p>Từ đầu tiên được bốc tự động lần đầu trang được hiện; sau đó từ đang hiện được giữ khi chuyển trang. Việc bốc từ gọi
 * {@link QuizService#randomSlang()} trên luồng nền chung ({@link ServiceExecutor}); bấm liên tiếp nhanh thì luôn hiện kết quả
 * của lần bấm cuối ({@link LatestTask}). Không ghi history.
 */
final class RandomPage extends JPanel {

    private static final String CARD_BLANK = "blank";
    private static final String CARD_STATE = "state";
    private static final String CARD_WORD = "word";

    /** Từ đã chọn, đã dựng sẵn chuỗi nghĩa trên luồng nền. */
    private record Shown(String slang, String meaning) {
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final StateCard stateCard = new StateCard();
    private final WrapLabel slangLabel = new WrapLabel("", Theme.FONT_TITLE, Theme.TEXT, Theme.STATE_TEXT_MAX_WIDTH);
    private final WrapLabel meaningLabel = new WrapLabel("", Theme.FONT_BODY, Theme.TEXT, Theme.STATE_TEXT_MAX_WIDTH);
    private final LatestTask<Optional<Shown>> task;
    private boolean loadedOnce;

    RandomPage(QuizService quiz) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        task = new LatestTask<>("random slang", () -> pick(quiz), this::show, this::showError);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        // CardLayout phát "shown" khi mục này được chọn trên sidebar: bốc từ đầu tiên ở lần hiện đầu tiên.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (!loadedOnce) {
                    loadedOnce = true;
                    task.request();
                }
            }
        });
    }

    /** Chạy trên luồng nền. */
    private static Optional<Shown> pick(QuizService quiz) {
        Optional<SlangWord> word = quiz.randomSlang();
        return word.map(w -> new Shown(w.getSlang(), QuizService.meaningText(w)));
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JComponent buildHeader() {
        JLabel title = new JLabel(Messages.pageTitle(Page.RANDOM));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);
        WrappedText description = new WrappedText(Messages.pageDescription(Page.RANDOM), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        description.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        header.add(description);
        header.add(Box.createVerticalStrut(Theme.SPACE_L));
        header.add(buildNextButtonRow());
        header.add(Box.createVerticalStrut(Theme.SPACE_M));
        return header;
    }

    /** Nút luôn hiện (không nằm trong thẻ từ) để vẫn thử lại được khi từ điển trống hoặc bốc từ bị lỗi. */
    private JComponent buildNextButtonRow() {
        ActionButton next = new ActionButton(Messages.RANDOM_BUTTON);
        next.addActionListener(e -> task.request());
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(next, BorderLayout.WEST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));
        return row;
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
        body.add(buildWordCard(), CARD_WORD);
        cards.show(body, CARD_BLANK);
        return body;
    }

    private JComponent buildWordCard() {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));
        JLabel slangCaption = caption(Messages.RANDOM_LABEL_SLANG);
        JLabel meaningCaption = caption(Messages.RANDOM_LABEL_MEANING);
        for (JComponent c : new JComponent[]{slangCaption, slangLabel, meaningCaption, meaningLabel}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        inner.add(slangCaption);
        inner.add(Box.createVerticalStrut(Theme.SPACE_XS));
        inner.add(slangLabel);
        inner.add(Box.createVerticalStrut(Theme.SPACE_L));
        inner.add(meaningCaption);
        inner.add(Box.createVerticalStrut(Theme.SPACE_XS));
        inner.add(meaningLabel);

        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        card.add(inner, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(card, BorderLayout.NORTH);

        // Từ có nhiều nghĩa rất dài thì thẻ cao hơn cửa sổ: cho cuộn.
        JScrollPane scroll = new JScrollPane(top, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        SlimScrollBarUI.apply(scroll);
        scroll.setBorder(BorderFactory.createEmptyBorder()); // không để lại khung Nimbus
        scroll.setViewportBorder(null);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    // ------------------------------------------------------------------ hiển thị (EDT)

    private void show(Optional<Shown> shown) {
        if (shown.isEmpty()) {
            stateCard.show(Messages.RANDOM_EMPTY_TITLE, Messages.randomEmptyDetail(), false);
            cards.show(body, CARD_STATE);
            return;
        }
        slangLabel.setText(shown.get().slang());
        meaningLabel.setText(shown.get().meaning());
        cards.show(body, CARD_WORD);
    }

    private void showError(Throwable cause) {
        stateCard.show(Messages.RANDOM_ERROR_TITLE, Messages.UNEXPECTED_ERROR_DETAIL, true);
        cards.show(body, CARD_STATE);
    }
}
