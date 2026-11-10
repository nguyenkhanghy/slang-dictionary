package com.slangdict.ui;

import java.util.ArrayList;
import java.util.List;

import com.slangdict.model.SlangWord;
import com.slangdict.ui.components.Theme;

/**
 * Bảng kết quả tìm kiếm hai cột "Từ lóng | Nghĩa", dựng trên {@link WrapTable} (toàn bộ việc đo, ngắt dòng, vẽ, bố trí
 * và debounce nằm ở đó). Lớp này chỉ thêm việc nhớ {@link SlangWord} của từng hàng để màn Sửa/Xóa chọn đúng entry.
 *
 * <p>Là lớp con (không phải thành phần bọc) vì người dùng của nó cần một {@code JTable} thật: đặt vào JScrollPane,
 * đọc dòng đang chọn. Bảng không sắp xếp lại nên chỉ số hàng hiển thị trùng chỉ số trong danh sách.
 */
final class ResultTable extends WrapTable {

    private List<SlangWord> words = List.of();

    ResultTable() {
        super(List.of(
                new WrapTable.Column(Messages.COLUMN_SLANG, Theme.FONT_BODY_BOLD, Theme.TEXT, Theme.TABLE_SLANG_COLUMN_WIDTH),
                new WrapTable.Column(Messages.COLUMN_MEANING, Theme.FONT_BODY, Theme.TEXT, 0)));
    }

    /** Hiện kết quả mới ({@code meanings[i]} là chuỗi nghĩa của {@code words.get(i)}), bỏ lựa chọn cũ và cuộn về đầu. */
    void showResults(List<SlangWord> words, String[] meanings) {
        if (words.size() != meanings.length) {
            throw new IllegalArgumentException("words and meanings must have the same size");
        }
        List<String[]> rows = new ArrayList<>(meanings.length);
        for (int i = 0; i < meanings.length; i++) {
            rows.add(new String[]{words.get(i).getSlang(), meanings[i]});
        }
        this.words = words; // đặt trước: setRows phát sự kiện bỏ lựa chọn
        setRows(rows);
    }

    /** Entry ở hàng {@code row}. */
    SlangWord wordAt(int row) {
        return words.get(row);
    }
}