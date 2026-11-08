package com.slangdict.ui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Ô nhập một dòng tự vẽ theo {@link Theme}: nền trắng bo góc, viền đổi màu khi có focus, chữ gợi ý khi rỗng,
 * và nút × xóa nhanh (chỉ hiện khi có chữ). Enter được báo qua {@link #addActionListener(ActionListener)}.
 */
public final class SearchField extends JPanel {

    private final HintTextField input;
    private final ClearButton clearButton = new ClearButton();
    private Runnable onClear = () -> { };

    public SearchField(String hint, String clearTooltip) {
        super(new BorderLayout(Theme.SPACE_S, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_M, 0, Theme.SPACE_S));

        input = new HintTextField(hint);
        add(input, BorderLayout.CENTER);
        clearButton.setToolTipText(clearTooltip);
        clearButton.setVisible(false);
        add(clearButton, BorderLayout.EAST);

        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshClearButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshClearButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshClearButton();
            }
        });
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public String getText() {
        return input.getText();
    }

    public void setText(String text) {
        input.setText(text);
    }

    /** Được gọi khi người dùng bấm nút × (sau khi ô đã được xóa). */
    public void setOnClear(Runnable onClear) {
        this.onClear = onClear;
    }

    /** Lắng nghe phím Enter trong ô nhập. */
    public void addActionListener(ActionListener listener) {
        input.addActionListener(listener);
    }

    private void refreshClearButton() {
        boolean show = !input.getText().isEmpty();
        if (clearButton.isVisible() != show) {
            clearButton.setVisible(show);
            revalidate();
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, Theme.INPUT_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.createGraphics(g);
        try {
            int arc = Theme.RADIUS * 2;
            boolean focused = input.hasFocus();
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(focused ? Theme.INPUT_BORDER_FOCUS : Theme.INPUT_BORDER);
            g2.setStroke(new BasicStroke(focused ? 2f : 1f));
            int inset = focused ? 1 : 0;
            g2.drawRoundRect(inset, inset, getWidth() - 1 - 2 * inset, getHeight() - 1 - 2 * inset, arc, arc);
        } finally {
            g2.dispose();
        }
    }

    /** JTextField không viền, không nền (nền do {@link SearchField} vẽ), có chữ gợi ý khi rỗng. */
    private static final class HintTextField extends JTextField {

        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setBorder(null);
            setMargin(new Insets(0, 0, 0, 0));
            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT);
            setCaretColor(Theme.TEXT);
            setSelectionColor(Theme.ACCENT);
            setSelectedTextColor(Theme.ON_ACCENT);
            getAccessibleContext().setAccessibleName(hint);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) {
                return;
            }
            Graphics2D g2 = Theme.createGraphics(g);
            try {
                FontMetrics fm = g2.getFontMetrics(Theme.FONT_BODY);
                g2.setFont(Theme.FONT_BODY);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }

    /** Nút tròn nhỏ có dấu ×; bấm thì xóa ô, trả focus về ô nhập rồi báo {@code onClear}. */
    private final class ClearButton extends JComponent {

        private static final int SIZE = 22;
        private boolean hover;

        ClearButton() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(false);
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
                    input.setText("");
                    input.requestFocusInWindow();
                    onClear.run();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.createGraphics(g);
            try {
                int d = Math.min(getWidth(), getHeight());
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;
                g2.setColor(hover ? Theme.SCROLL_THUMB_HOVER : Theme.SCROLL_THUMB);
                g2.fillOval(x, y, d, d);
                g2.setColor(Theme.ON_ACCENT);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = d / 3;
                g2.drawLine(x + pad, y + pad, x + d - pad, y + d - pad);
                g2.drawLine(x + d - pad, y + pad, x + pad, y + d - pad);
            } finally {
                g2.dispose();
            }
        }
    }
}
