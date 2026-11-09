package com.slangdict.ui;

import com.slangdict.model.SlangWord;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Model ảo của bảng kết quả: không tạo component cho từng dòng, chỉ giữ danh sách và chuỗi nghĩa đã dựng sẵn.
 * Cột 0 là từ lóng, cột 1 là nghĩa. Mọi ô chỉ đọc.
 */
final class ResultTableModel extends AbstractTableModel {

    private List<SlangWord> words = List.of();
    private String[] meanings = new String[0];

    /** Thay toàn bộ dữ liệu; {@code meanings[i]} là chuỗi hiển thị của {@code words.get(i)}. Gọi trên EDT. */
    void setData(List<SlangWord> words, String[] meanings) {
        if (words.size() != meanings.length) {
            throw new IllegalArgumentException("words and meanings must have the same size");
        }
        this.words = words;
        this.meanings = meanings;
        fireTableDataChanged();
    }

    SlangWord wordAt(int row) {
        return words.get(row);
    }

    String slangAt(int row) {
        return words.get(row).getSlang();
    }

    String meaningAt(int row) {
        return meanings[row];
    }

    @Override
    public int getRowCount() {
        return meanings.length;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? Messages.COLUMN_SLANG : Messages.COLUMN_MEANING;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return column == 0 ? slangAt(row) : meaningAt(row);
    }
}
