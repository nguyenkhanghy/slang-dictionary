package com.slangdict.ui;

import javax.swing.JComponent;

import com.slangdict.model.SearchType;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.QuizService;

/**
 * Tạo màn hình cho từng {@link Page} (F1–F10). Câu lệnh {@code switch} cố ý KHÔNG có nhánh mặc định: thêm một giá trị
 * vào {@link Page} mà quên tạo màn ở đây sẽ báo lỗi biên dịch thay vì hiện một trang trống lúc chạy.
 */
final class PageFactory {

    private PageFactory() {
    }

    /**
     * @param events kênh báo dữ liệu đổi (màn ghi dữ liệu gọi {@code fire}, MainPanel nối sidebar và các trang cần làm mới)
     * @param dialogs hộp thoại xác nhận/thông báo dùng chung
     */
    static JComponent create(Page page, DictionaryService service, DataEvents events, Dialogs dialogs) {
        return switch (page) {
            case SEARCH_SLANG -> new SearchPage(SearchType.SLANG, service);
            case SEARCH_DEFINITION -> new SearchPage(SearchType.DEFINITION, service);
            case HISTORY -> new HistoryPage(service, dialogs);
            case ADD -> new AddPage(service, events, dialogs);
            case EDIT -> new EditPage(service, events, dialogs);
            case DELETE -> new DeletePage(service, events, dialogs);
            case RESET -> new ResetPage(service, events, dialogs);
            case RANDOM -> new RandomPage(new QuizService(service));
            case QUIZ_SLANG_TO_MEANING -> new QuizPage(QuizPage.Kind.SLANG_TO_MEANING, new QuizService(service));
            case QUIZ_MEANING_TO_SLANG -> new QuizPage(QuizPage.Kind.MEANING_TO_SLANG, new QuizService(service));
        };
    }
}