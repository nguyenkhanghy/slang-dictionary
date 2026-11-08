package com.slangdict.ui;

import com.slangdict.service.DictionaryService;
import com.slangdict.ui.components.Theme;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/** Nội dung của cửa sổ chính: sidebar điều hướng bên trái, vùng nội dung (mỗi {@link Page} một thẻ) bên phải. */
final class MainPanel extends JPanel {

    MainPanel(DictionaryService service) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);

        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setBackground(Theme.BACKGROUND);
        for (Page page : Page.values()) {
            content.add(PageFactory.create(page, service), page.name());
        }

        NavSidebar sidebar = new NavSidebar(
                Messages.entryCount(service.size()),
                Messages.indexInfo(service.isIndexFromCache(), service.getOpenMillis()));
        sidebar.setOnSelect(page -> cards.show(content, page.name()));

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        sidebar.select(Page.SEARCH_SLANG);
    }
}