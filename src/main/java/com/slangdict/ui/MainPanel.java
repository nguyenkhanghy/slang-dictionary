package com.slangdict.ui;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * Nội dung của cửa sổ chính: sidebar điều hướng bên trái, vùng nội dung (mỗi {@link Page} một thẻ) bên phải.
 * Nối {@link DataEvents}: khi một màn ghi dữ liệu xong thì sidebar cập nhật số từ lóng và các trang {@link DataAware}
 * (trừ trang vừa phát sự kiện) bỏ dữ liệu cũ.
 */
final class MainPanel extends JPanel {

    MainPanel(DictionaryService service) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        DataEvents events = new DataEvents();
        Dialogs dialogs = new SwingDialogs();
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setBackground(Theme.BACKGROUND);
        for (Page page : Page.values()) {
            JComponent view = PageFactory.create(page, service, events, dialogs);
            content.add(view, page.name());
            if (view instanceof DataAware aware) {
                events.addListener((count, source) -> {
                    if (source != view) {
                        aware.dataChanged();
                    }
                });
            }
        }

        String indexInfo = Messages.indexInfo(service.isIndexFromCache(), service.getOpenMillis());
        NavSidebar sidebar = new NavSidebar(Messages.entryCount(service.size()), indexInfo);
        events.addListener((count, source) -> sidebar.setFooter(Messages.entryCount(count), indexInfo));
        sidebar.setOnSelect(page -> cards.show(content, page.name()));

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        sidebar.select(Page.SEARCH_SLANG);
    }
}
