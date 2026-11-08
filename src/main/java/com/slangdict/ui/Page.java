package com.slangdict.ui;

/**
 * Các màn hình của ứng dụng, theo thứ tự hiển thị trong sidebar (mỗi chức năng F1–F10 một mục).
 * Chỉ chứa định danh và nhóm; mọi chuỗi hiển thị nằm ở {@link Messages}.
 */
public enum Page {
    SEARCH_SLANG(Group.LOOKUP),       // F1
    SEARCH_DEFINITION(Group.LOOKUP),  // F2
    HISTORY(Group.LOOKUP),            // F3
    ADD(Group.MANAGE),                // F4
    EDIT(Group.MANAGE),               // F5
    DELETE(Group.MANAGE),             // F6
    RESET(Group.MANAGE),              // F7
    RANDOM(Group.PLAY),               // F8
    QUIZ_SLANG_TO_MEANING(Group.PLAY),// F9
    QUIZ_MEANING_TO_SLANG(Group.PLAY);// F10

    /** Nhóm mục trong sidebar. */
    public enum Group { LOOKUP, MANAGE, PLAY }

    private final Group group;

    Page(Group group) {
        this.group = group;
    }

    public Group group() {
        return group;
    }
}