package com.slangdict.ui;

import com.slangdict.ui.components.Theme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Một đáp án của câu trắc nghiệm: huy hiệu chữ cái (A, B, C, D) và nội dung tự xuống dòng, rộng cố định
 * ({@link Theme#QUIZ_CONTENT_WIDTH}). Chọn bằng chuột hoặc Enter/Space khi đang có focus. Sau khi trả lời, thẻ không còn
 * phản ứng và được tô màu theo {@link Mark}. Tự vẽ theo {@link Theme}.
 */
final class OptionCard extends JPanel {

    /** Trạng thái tô màu sau khi trả lời. */
    enum Mark { NONE, CORRECT, WRONG }

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int TEXT_LEFT = Theme.QUIZ_OPTION_PAD_X + Theme.QUIZ_BADGE_SIZE + Theme.SPACE_M;

    private final int index;
    private final Runnable onChoose;
    private boolean hover;
    private boolean answered;
    private Mark mark = Mark.NONE;

    OptionCard(int index, String text, Runnable onChoose) {
        super(new BorderLayout());
        this.index = index;
        this.onChoose = onChoose;
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(Theme.QUIZ_OPTION_PAD_Y, TEXT_LEFT,
                Theme.QUIZ_OPTION_PAD_Y, Theme.QUIZ_OPTION_PAD_X));
        int textWidth = Theme.QUIZ_CONTENT_WIDTH - TEXT_LEFT - Theme.QUIZ_OPTION_PAD_X;
        add(new WrapLabel(text, Theme.FONT_BODY, Theme.TEXT, textWidth), BorderLayout.NORTH);
        installInput();
    }

    private void installInput() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                choose();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "choose");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "choose");
        getActionMap().put("choose", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choose();
            }
        });
    }

    private void choose() {
        if (!answered) {
            requestFocusInWindow();
            onChoose.run();
        }
    }

    /** Sau khi trả lời: khóa thẻ và tô màu. */
    void markAnswered(Mark mark) {
        this.answered = true;
        this.mark = mark;
        this.hover = false;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    int index() {
        return index;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Theme.QUIZ_CONTENT_WIDTH, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            int w = getWidth();
            int h = getHeight();
            int arc = Theme.RADIUS * 2;
            boolean active = !answered && (hover || isFocusOwner());
            Color fill = mark == Mark.CORRECT ? Theme.SUCCESS_SOFT
                    : mark == Mark.WRONG ? Theme.DANGER_SOFT
                    : active ? Theme.ACCENT_SOFT : Theme.SURFACE;
            Color line = mark == Mark.CORRECT ? Theme.SUCCESS
                    : mark == Mark.WRONG ? Theme.DANGER
                    : active ? Theme.ACCENT : Theme.BORDER;
            boolean thick = mark != Mark.NONE || (!answered && isFocusOwner());
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(line);
            g2.setStroke(new BasicStroke(thick ? 2f : 1f));
            int inset = thick ? 1 : 0;
            g2.drawRoundRect(inset, inset, w - 1 - 2 * inset, h - 1 - 2 * inset, arc, arc);
            paintBadge(g2, line);
        } finally {
            g2.dispose();
        }
    }

    private void paintBadge(Graphics2D g2, Color line) {
        int size = Theme.QUIZ_BADGE_SIZE;
        FontMetrics body = g2.getFontMetrics(Theme.FONT_BODY);
        int x = Theme.QUIZ_OPTION_PAD_X;
        int y = Theme.QUIZ_OPTION_PAD_Y + (body.getHeight() - size) / 2; // thẳng hàng với dòng chữ đầu
        boolean marked = mark != Mark.NONE;
        g2.setColor(marked ? line : Theme.ACCENT_SOFT);
        g2.fillOval(x, y, size, size);
        FontMetrics fm = g2.getFontMetrics(Theme.FONT_BODY_BOLD);
        String letter = String.valueOf(LETTERS.charAt(index % LETTERS.length()));
        g2.setFont(Theme.FONT_BODY_BOLD);
        g2.setColor(marked ? Theme.ON_ACCENT : Theme.ACCENT);
        g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
    }

    /** Dùng cho kiểm thử/gỡ lỗi. */
    @Override
    public String toString() {
        return "OptionCard[" + index + "," + mark + "]";
    }
}
