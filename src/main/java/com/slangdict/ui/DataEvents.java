package com.slangdict.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Kênh báo "dữ liệu từ điển đã đổi" giữa các màn: màn thêm/sửa/xóa/reset gọi {@link #fire} sau khi ghi thành công; sidebar cập
 * nhật số từ lóng và các trang {@link DataAware} bỏ dữ liệu cũ. Chỉ dùng trên EDT.
 */
final class DataEvents {

    /** Người nhận sự kiện. {@code source} là nơi phát (để trang phát tự bỏ qua sự kiện của chính mình). */
    interface Listener {
        void dataChanged(int entryCount, Object source);
    }

    private final List<Listener> listeners = new ArrayList<>();

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    /** @param entryCount số từ lóng sau thay đổi (đọc trên luồng nền cùng lúc với thao tác ghi) */
    void fire(int entryCount, Object source) {
        for (Listener listener : List.copyOf(listeners)) {
            listener.dataChanged(entryCount, source);
        }
    }
}
