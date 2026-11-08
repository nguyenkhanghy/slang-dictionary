package com.slangdict.ui;

import com.slangdict.model.SearchType;
import com.slangdict.service.DictionaryService;

import javax.swing.JComponent;

/** Tạo màn hình cho từng {@link Page}. U3: hai mục tìm kiếm đã là màn thật; các mục còn lại vẫn là trang giữ chỗ (U4 thay tiếp). */
final class PageFactory {

    private PageFactory() {
    }

    static JComponent create(Page page, DictionaryService service) {
        return switch (page) {
            case SEARCH_SLANG -> new SearchPage(SearchType.SLANG, service);
            case SEARCH_DEFINITION -> new SearchPage(SearchType.DEFINITION, service);
            default -> new PlaceholderPage(page);
        };
    }
}
