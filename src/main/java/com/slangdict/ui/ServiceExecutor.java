package com.slangdict.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Một luồng nền duy nhất cho MỌI lời gọi {@code DictionaryService} từ UI.
 * Service không thread-safe nên các thao tác không được chạy đồng thời, kể cả khi hai trang cùng tìm một lúc;
 * dùng chung một hàng đợi một luồng thì các thao tác tự xếp hàng. Luồng là daemon nên không cản việc thoát.
 * Mọi thao tác ghi (thêm, sửa, xóa, reset) và history cũng phải gọi service qua executor này.
 */
final class ServiceExecutor {

    private static final Executor SHARED = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "slangdict-service");
        thread.setDaemon(true);
        return thread;
    });

    private ServiceExecutor() {
    }

    static Executor shared() {
        return SHARED;
    }
}