package com.slangdict.ui;

import com.slangdict.model.SlangWord;
import com.slangdict.ui.components.Theme;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng kết quả tìm kiếm hai cột "Từ lóng | Nghĩa". Chữ dài tự xuống dòng và chiều cao mỗi hàng co theo nội dung.
 *
 * <p>Chiều cao hàng được tính bằng {@link TextWrapper} (cùng thuật toán với lúc vẽ) cho TOÀN BỘ kết quả mỗi khi có dữ liệu
 * mới hoặc bề rộng cột đổi, nên cuộn luôn đúng dù có hàng chục nghìn dòng; chỉ hàng đang nhìn thấy mới được vẽ.
 * Khi bề rộng đổi: ít hàng thì tính lại ngay; rất nhiều hàng thì gộp (debounce) để không giật khi đang kéo cửa sổ.
 * Mọi thứ chạy trên EDT.
 */
final class ResultTable extends JTable {

    private static final int RELAYOUT_DELAY_MS = 120;
    /** Tới cỡ này tính lại chiều cao ngay khi bề rộng đổi (chỉ vài ms); lớn hơn thì gộp lại để không giật khi kéo cửa sổ. */
    private static final int IMMEDIATE_RELAYOUT_MAX_ROWS = 3000;

    private final ResultTableModel data;
    private final TextWrapper slangWrapper;
    private final TextWrapper meaningWrapper;
    private final Timer relayoutTimer = new Timer(RELAYOUT_DELAY_MS, e -> updateRowHeights());
    private int lastWidth;

    ResultTable() {
        super(new ResultTableModel());
        data = (ResultTableModel) getModel();
        slangWrapper = new TextWrapper(getFontMetrics(Theme.FONT_BODY_BOLD));
        meaningWrapper = new TextWrapper(getFontMetrics(Theme.FONT_BODY));
        relayoutTimer.setRepeats(false);
        configure();
    }

    /** Hiện kết quả mới, bỏ lựa chọn cũ và cuộn về đầu. */
    void showResults(List<SlangWord> words, String[] meanings) {
        data.setData(words, meanings);
        updateRowHeights();
        if (getParent() instanceof JViewport viewport) {
            viewport.setViewPosition(new Point(0, 0));
        }
    }

    /** Entry ở hàng {@code row} (chỉ số theo model; bảng không sắp xếp lại nên trùng chỉ số hiển thị). */
    SlangWord wordAt(int row) {
        return data.wordAt(row);
    }

    private void configure() {
        setBackground(Theme.SURFACE);
        setOpaque(true);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(Theme.TABLE_ROW_MIN_HEIGHT);
        setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        TableColumn slang = getColumnModel().getColumn(0);
        slang.setMinWidth(Theme.TABLE_SLANG_COLUMN_WIDTH);
        slang.setMaxWidth(Theme.TABLE_SLANG_COLUMN_WIDTH);
        slang.setPreferredWidth(Theme.TABLE_SLANG_COLUMN_WIDTH);
        slang.setCellRenderer(new CellRenderer(slangWrapper));
        getColumnModel().getColumn(1).setCellRenderer(new CellRenderer(meaningWrapper));
    }

    /**
     * JTable tự gắn tiêu đề cột vào JScrollPane khi hiện ra, nhưng đồng thời ghi đè border của JScrollPane bằng border
     * Nimbus nếu border đang là null; việc đó làm hiện lại khung Nimbus mà {@code SlimScrollBarUI.apply} đã bỏ.
     * Giữ tiêu đề cột, trả lại border cũ.
     */
    @Override
    protected void configureEnclosingScrollPane() {
        JScrollPane scrollPane = getParent() instanceof JViewport viewport
                && viewport.getParent() instanceof JScrollPane sp ? sp : null;
        Border border = scrollPane == null ? null : scrollPane.getBorder();
        super.configureEnclosingScrollPane();
        if (scrollPane != null) {
            scrollPane.setBorder(border);
            // Góc trống phía trên thanh cuộn (cạnh tiêu đề cột) nếu không đặt sẽ bị Nimbus tô màu lạ.
            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new HeaderCorner());
        }
    }

    /**
     * Bề rộng đổi (kéo cửa sổ, hoặc thanh cuộn vừa hiện/ẩn làm bảng hẹp/rộng ra vài px): tính lại chiều cao hàng ngay
     * nếu lần đầu hoặc ít hàng, để chữ không bị cắt dù chỉ một khoảnh khắc; nhiều hàng thì gộp lại rồi mới tính.
     */
    @Override
    public void doLayout() {
        super.doLayout();
        int width = getWidth();
        if (width == lastWidth) {
            return;
        }
        boolean first = lastWidth == 0;
        lastWidth = width;
        if (first || getRowCount() <= IMMEDIATE_RELAYOUT_MAX_ROWS) {
            updateRowHeights();
        } else {
            relayoutTimer.restart();
        }
    }

    private void updateRowHeights() {
        relayoutTimer.stop();
        setRowHeight(Theme.TABLE_ROW_MIN_HEIGHT); // xóa mọi chiều cao riêng của lần trước
        int slangWidth = textWidth(0);
        int meaningWidth = textWidth(1);
        if (getRowCount() == 0 || slangWidth <= 0 || meaningWidth <= 0) {
            return;
        }
        int lineHeight = Math.max(slangWrapper.lineHeight(), meaningWrapper.lineHeight());
        for (int row = 0; row < getRowCount(); row++) {
            int lines = Math.max(slangWrapper.wrap(data.slangAt(row), slangWidth, null),
                    meaningWrapper.wrap(data.meaningAt(row), meaningWidth, null));
            int height = Math.max(Theme.TABLE_ROW_MIN_HEIGHT, lines * lineHeight + 2 * Theme.TABLE_CELL_PAD_Y);
            if (height != Theme.TABLE_ROW_MIN_HEIGHT) {
                setRowHeight(row, height);
            }
        }
    }

    /** Bề rộng dành cho chữ trong một cột (đã trừ lề hai bên). */
    private int textWidth(int column) {
        return getColumnModel().getColumn(column).getWidth() - 2 * Theme.TABLE_CELL_PAD_X;
    }

    // ------------------------------------------------------------------ vẽ ô

    /** Vẽ một ô: nền (chọn / thường), đường kẻ dưới, chữ đã ngắt dòng. */
    private final class CellRenderer extends JComponent implements TableCellRenderer {

        private final TextWrapper wrapper;
        private final Font font;
        private final List<String> lines = new ArrayList<>();
        private boolean selected;

        CellRenderer(TextWrapper wrapper) {
            this.wrapper = wrapper;
            this.font = wrapper == slangWrapper ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            selected = isSelected;
            lines.clear();
            wrapper.wrap(String.valueOf(value), Math.max(1, textWidth(column)), lines);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.createGraphics(g);
            try {
                g2.setColor(selected ? Theme.TABLE_ROW_SELECTED : Theme.SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.setFont(font);
                g2.setColor(Theme.TEXT);
                int y = Theme.TABLE_CELL_PAD_Y + wrapper.ascent();
                for (String line : lines) {
                    g2.drawString(line, Theme.TABLE_CELL_PAD_X, y);
                    y += wrapper.lineHeight();
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Góc trên bên phải của vùng cuộn: liền màu với tiêu đề cột để hàng tiêu đề trông liền mạch. */
    private static final class HeaderCorner extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.BORDER);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

    /** Vẽ tiêu đề cột: chữ đậm màu phụ và một đường kẻ dưới. */
    private static final class HeaderRenderer extends JComponent implements TableCellRenderer {

        private String text = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            text = String.valueOf(value);
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Theme.TABLE_SLANG_COLUMN_WIDTH, Theme.TABLE_HEADER_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.createGraphics(g);
            try {
                g2.setColor(Theme.SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                FontMetrics fm = g2.getFontMetrics(Theme.FONT_BODY_BOLD);
                g2.setFont(Theme.FONT_BODY_BOLD);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(text, Theme.TABLE_CELL_PAD_X, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
