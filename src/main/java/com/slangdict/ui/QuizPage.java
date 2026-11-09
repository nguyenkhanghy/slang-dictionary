package com.slangdict.ui;

import com.slangdict.service.QuizQuestion;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Màn trắc nghiệm dùng chung cho F9 ("cho từ lóng, chọn nghĩa") và F10 ("cho nghĩa, chọn từ lóng"); {@link Kind} quyết
 * định hàm của {@link QuizService} và nhãn câu hỏi.
 *
 * <p>Câu đầu tiên tự hiện khi trang được hiện lần đầu. Bấm một đáp án thì hiện đúng/sai (đáp án đúng tô xanh, đáp án sai
 * đã chọn tô đỏ) và không đổi được nữa; nút "Câu tiếp theo" luôn hiện (bỏ qua câu cũng được, và dùng để thử lại khi lỗi).
 * Chưa tính điểm. Tạo câu hỏi chạy trên luồng nền chung ({@link ServiceExecutor}) qua {@link LatestTask}.
 */
final class QuizPage extends JPanel implements DataAware {

    /** F9 hoặc F10. */
    enum Kind {
        SLANG_TO_MEANING(Page.QUIZ_SLANG_TO_MEANING, Messages.QUIZ_CAPTION_SLANG),
        MEANING_TO_SLANG(Page.QUIZ_MEANING_TO_SLANG, Messages.QUIZ_CAPTION_MEANING);

        private final Page page;
        private final String promptCaption;

        Kind(Page page, String promptCaption) {
            this.page = page;
            this.promptCaption = promptCaption;
        }
    }

    /** Câu hỏi dài hơn mức này (thường là nghĩa nhiều vế ở F10) dùng cỡ chữ thường để đáp án không bị đẩy xuống quá xa. */
    private static final int LONG_PROMPT_LENGTH = 100;

    private static final String CARD_BLANK = "blank";
    private static final String CARD_STATE = "state";
    private static final String CARD_QUESTION = "question";

    private final Kind kind;
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final StateCard stateCard = new StateCard();
    private final JLabel captionLabel = new JLabel();
    private final JPanel promptHolder = verticalPanel();
    private final JPanel optionsHolder = verticalPanel();
    private final JPanel feedbackHolder = verticalPanel();
    private final LatestTask<Optional<QuizQuestion>> task;
    private final List<OptionCard> optionCards = new ArrayList<>();
    private QuizQuestion question;
    private boolean answered;
    private boolean loadedOnce;

    QuizPage(Kind kind, QuizService quiz) {
        super(new BorderLayout());
        this.kind = kind;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        task = new LatestTask<>("quiz question " + kind, () -> nextQuestion(quiz), this::showQuestion, this::showError);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

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

    /** Dữ liệu vừa đổi: câu hỏi đang hiện dựa trên dữ liệu cũ, nên bỏ đi và tạo câu mới ở lần hiện trang kế tiếp. */
    @Override
    public void dataChanged() {
        loadedOnce = false;
        question = null;
        cards.show(body, CARD_BLANK);
    }

    /** Chạy trên luồng nền. */
    private Optional<QuizQuestion> nextQuestion(QuizService quiz) {
        return kind == Kind.SLANG_TO_MEANING ? quiz.nextSlangToMeaningQuestion() : quiz.nextMeaningToSlangQuestion();
    }

    private static JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    // ------------------------------------------------------------------ dựng giao diện

    private JComponent buildHeader() {
        JLabel title = new JLabel(Messages.pageTitle(kind.page));
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);
        WrappedText description = new WrappedText(Messages.pageDescription(kind.page), Theme.FONT_BODY,
                Theme.TEXT_MUTED, Theme.TEXT_MAX_WIDTH);

        ActionButton next = new ActionButton(Messages.QUIZ_NEXT_BUTTON);
        next.addActionListener(e -> task.request());
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.add(next, BorderLayout.WEST);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.INPUT_HEIGHT));

        JPanel header = verticalPanel();
        for (JComponent c : new JComponent[]{title, description, buttonRow}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        header.add(title);
        header.add(Box.createVerticalStrut(Theme.SPACE_S));
        header.add(description);
        header.add(Box.createVerticalStrut(Theme.SPACE_L));
        header.add(buttonRow);
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
        body.add(buildQuestionCard(), CARD_QUESTION);
        cards.show(body, CARD_BLANK);
        return body;
    }

    private JComponent buildQuestionCard() {
        captionLabel.setText(kind.promptCaption);
        captionLabel.setFont(Theme.FONT_SMALL);
        captionLabel.setForeground(Theme.TEXT_MUTED);

        JPanel inner = verticalPanel();
        inner.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L, Theme.SPACE_L));
        for (JComponent c : new JComponent[]{captionLabel, promptHolder, optionsHolder, feedbackHolder}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        inner.add(captionLabel);
        inner.add(Box.createVerticalStrut(Theme.SPACE_XS));
        inner.add(promptHolder);
        inner.add(Box.createVerticalStrut(Theme.SPACE_L));
        inner.add(optionsHolder);
        inner.add(feedbackHolder);

        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER);
        card.add(inner, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(card, BorderLayout.NORTH);

        // Đáp án dài làm thẻ cao hơn cửa sổ: cho cuộn.
        JScrollPane scroll = new JScrollPane(top, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        SlimScrollBarUI.apply(scroll);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    // ------------------------------------------------------------------ hiển thị (EDT)

    private void showQuestion(Optional<QuizQuestion> next) {
        if (next.isEmpty()) {
            stateCard.show(Messages.QUIZ_EMPTY_TITLE, Messages.quizEmptyDetail(), false);
            cards.show(body, CARD_STATE);
            return;
        }
        question = next.get();
        answered = false;
        promptHolder.removeAll();
        Font promptFont = question.prompt().length() > LONG_PROMPT_LENGTH ? Theme.FONT_BODY_BOLD : Theme.FONT_WORDMARK;
        promptHolder.add(new WrapLabel(question.prompt(), promptFont, Theme.TEXT, Theme.QUIZ_CONTENT_WIDTH));
        optionsHolder.removeAll();
        optionCards.clear();
        List<String> options = question.options();
        for (int i = 0; i < options.size(); i++) {
            final int choice = i;
            OptionCard card = new OptionCard(i, options.get(i), () -> choose(choice));
            card.setAlignmentX(LEFT_ALIGNMENT);
            optionCards.add(card);
            if (i > 0) {
                optionsHolder.add(Box.createVerticalStrut(Theme.SPACE_S));
            }
            optionsHolder.add(card);
        }
        feedbackHolder.removeAll();
        revalidate();
        repaint();
        cards.show(body, CARD_QUESTION);
    }

    private void choose(int choice) {
        if (answered || question == null) {
            return;
        }
        answered = true;
        boolean correct = question.isCorrect(choice);
        for (OptionCard card : optionCards) {
            if (card.index() == question.correctIndex()) {
                card.markAnswered(OptionCard.Mark.CORRECT);
            } else if (card.index() == choice) {
                card.markAnswered(OptionCard.Mark.WRONG);
            } else {
                card.markAnswered(OptionCard.Mark.NONE);
            }
        }
        feedbackHolder.removeAll();
        feedbackHolder.add(Box.createVerticalStrut(Theme.SPACE_M));
        WrapLabel feedback = new WrapLabel(correct ? Messages.QUIZ_CORRECT : Messages.QUIZ_WRONG, Theme.FONT_BODY_BOLD,
                correct ? Theme.SUCCESS : Theme.DANGER, Theme.QUIZ_CONTENT_WIDTH);
        feedback.setAlignmentX(LEFT_ALIGNMENT);
        feedbackHolder.add(feedback);
        revalidate();
        repaint();
    }

    private void showError(Throwable cause) {
        stateCard.show(Messages.QUIZ_ERROR_TITLE, Messages.UNEXPECTED_ERROR_DETAIL, true);
        cards.show(body, CARD_STATE);
    }
}
