package com.slangdict.ui;

import com.slangdict.ui.components.Theme;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng chỉ đọc nhiều cột toàn chữ, mỗi ô tự xuống dòng và chiều cao hàng co theo ô cao nhất (dùng cho lịch sử tìm kiếm).
 * Các cột đầu có bề rộng cố định, cột cuối lấy phần còn lại. Cùng cách làm với {@link ResultTable} (đo và vẽ bằng
 * {@link TextWrapper} nên không cắt chữ; border của JScrollPane không bị JTable ghi đè; góc trên-phải liền màu tiêu đề);
 * khác ở chỗ số cột, font và màu tùy chọn, dữ liệu là {@code List<String[]>}. Chạy trên EDT.
 */
final class WrapTable extends JTable {

    /** Một cột: tiêu đề, font/màu chữ, bề rộng cố định (px) hoặc 0 cho cột co giãn (chỉ được là cột cuối). */
    record Column(String title, Font font, Color color, int fixedWidth) {
    }

    private static final int RELAYOUT_DELAY_MS = 120;
    private static final int IMMEDIATE_RELAYOUT_MAX_ROWS = 3000;

    private final RowsModel data;
    private final TextWrapper[] wrappers;
    private final Timer relayoutTimer = new Timer(RELAYOUT_DELAY_MS, e -> updateRowHeights());
    private int lastWidth;

    WrapTable(List<Column> columns) {
        super(new RowsModel(columns));
        if (columns.isEmpty() || columns.subList(0, columns.size() - 1).stream().anyMatch(c -> c.fixedWidth() <= 0)) {
            throw new IllegalArgumentException("every column except the last must have a fixed width");
        }
        data = (RowsModel) getModel();
        wrappers = new TextWrapper[columns.size()];
        for (int i = 0; i < wrappers.length; i++) {
            wrappers[i] = new TextWrapper(getFontMetrics(columns.get(i).font()));
        }
        relayoutTimer.setRepeats(false);
        configure(columns);
    }

    /** Hiện các hàng mới (mỗi hàng một chuỗi cho mỗi cột), bỏ lựa chọn cũ và cuộn về đầu. */
    void setRows(List<String[]> rows) {
        data.setRows(rows);
        updateRowHeights();
        if (getParent() instanceof JViewport viewport) {
            viewport.setViewPosition(new Point(0, 0));
        }
    }

    private void configure(List<Column> columns) {
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

        for (int i = 0; i < columns.size(); i++) {
            Column spec = columns.get(i);
            TableColumn column = getColumnModel().getColumn(i);
            if (spec.fixedWidth() > 0) {
                column.setMinWidth(spec.fixedWidth());
                column.setMaxWidth(spec.fixedWidth());
                column.setPreferredWidth(spec.fixedWidth());
            }
            column.setCellRenderer(new CellRenderer(wrappers[i], spec.font(), spec.color()));
        }
    }

    /** Giữ tiêu đề cột nhưng trả lại border của JScrollPane (JTable ghi đè bằng border Nimbus) và đặt góc liền màu. */
    @Override
    protected void configureEnclosingScrollPane() {
        JScrollPane scrollPane = getParent() instanceof JViewport viewport
                && viewport.getParent() instanceof JScrollPane sp ? sp : null;
        Border border = scrollPane == null ? null : scrollPane.getBorder();
        super.configureEnclosingScrollPane();
        if (scrollPane != null) {
            scrollPane.setBorder(border);
            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new HeaderCorner());
        }
    }

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
        setRowHeight(Theme.TABLE_ROW_MIN_HEIGHT);
        int columns = getColumnCount();
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = textWidth(c);
            if (widths[c] <= 0) {
                return; // chưa được bố trí
            }
        }
        int lineHeight = wrappers[0].lineHeight();
        for (int row = 0; row < getRowCount(); row++) {
            String[] cells = data.rowAt(row);
            int lines = 1;
            for (int c = 0; c < columns; c++) {
                lines = Math.max(lines, wrappers[c].wrap(cells[c], widths[c], null));
            }
            int height = Math.max(Theme.TABLE_ROW_MIN_HEIGHT, lines * lineHeight + 2 * Theme.TABLE_CELL_PAD_Y);
            if (height != Theme.TABLE_ROW_MIN_HEIGHT) {
                setRowHeight(row, height);
            }
        }
    }

    private int textWidth(int column) {
        return getColumnModel().getColumn(column).getWidth() - 2 * Theme.TABLE_CELL_PAD_X;
    }

    // ------------------------------------------------------------------ model

    private static final class RowsModel extends AbstractTableModel {

        private final List<Column> columns;
        private List<String[]> rows = List.of();

        RowsModel(List<Column> columns) {
            this.columns = List.copyOf(columns);
        }

        void setRows(List<String[]> rows) {
            for (String[] row : rows) {
                if (row.length != columns.size()) {
                    throw new IllegalArgumentException("row has " + row.length + " cells, expected " + columns.size());
                }
            }
            this.rows = rows;
            fireTableDataChanged();
        }

        String[] rowAt(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).title();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row)[column];
        }
    }

    // ------------------------------------------------------------------ vẽ

    private final class CellRenderer extends JComponent implements TableCellRenderer {

        private final TextWrapper wrapper;
        private final Font font;
        private final Color color;
        private final List<String> lines = new ArrayList<>();
        private boolean selected;

        CellRenderer(TextWrapper wrapper, Font font, Color color) {
            this.wrapper = wrapper;
            this.font = font;
            this.color = color;
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
                g2.setColor(color);
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

    private static final class HeaderCorner extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.BORDER);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
        }
    }

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
