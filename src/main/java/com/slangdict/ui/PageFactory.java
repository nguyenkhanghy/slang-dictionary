package com.slangdict.ui;

import com.slangdict.model.SearchType;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.QuizService;

import javax.swing.JComponent;

/** Tạo màn hình cho từng {@link Page}. U3: F1/F2; U4a: F3, F8; U4b: F9, F10. Các mục còn lại (Add/Edit/Delete/Reset) vẫn là trang giữ chỗ (U4c thay tiếp). */
final class PageFactory {

    private PageFactory() {
    }

    static JComponent create(Page page, DictionaryService service) {
        return switch (page) {
            case SEARCH_SLANG -> new SearchPage(SearchType.SLANG, service);
            case SEARCH_DEFINITION -> new SearchPage(SearchType.DEFINITION, service);
            case HISTORY -> new HistoryPage(service);
            case RANDOM -> new RandomPage(new QuizService(service));
            case QUIZ_SLANG_TO_MEANING -> new QuizPage(QuizPage.Kind.SLANG_TO_MEANING, new QuizService(service));
            case QUIZ_MEANING_TO_SLANG -> new QuizPage(QuizPage.Kind.MEANING_TO_SLANG, new QuizService(service));
            default -> new PlaceholderPage(page);
        };
    }
}
