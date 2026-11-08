package com.slangdict.ui;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * MỌI chuỗi hiển thị cho người dùng nằm ở đây (tiếng Việt) để đổi ngôn ngữ ở một chỗ.
 * Thông báo log và exception vẫn viết bằng tiếng Anh ở nơi phát sinh, không đi qua lớp này.
 */
public final class Messages {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private Messages() {
    }

    // ------------------------------------------------------------------ chung

    public static final String APP_NAME = "Slang Dictionary";
    public static final String APP_TAGLINE = "Từ điển tiếng lóng";

    // ------------------------------------------------------------------ khởi động

    public static final String LOADING_STATUS = "Đang tải từ điển…";
    public static final String LOADING_HINT = "Lần mở đầu tiên có thể lâu hơn vì phải lập chỉ mục.";
    public static final String LOADING_CANCELLING = "Đang đóng, vui lòng chờ một chút…";

    public static final String ERROR_OPEN_TITLE = "Không thể mở dữ liệu";

    public static final String HEADLESS =
            "Không tìm thấy màn hình để hiển thị giao diện (môi trường không có đồ họa).\n"
                    + "Hãy chạy trên máy có giao diện đồ họa, hoặc thêm tham số --console để dùng menu dòng lệnh.";

    /** Nội dung hộp thoại khi không mở được dữ liệu; luôn nêu thư mục dữ liệu, không kèm stack trace. */
    public static String errorOpenMessage(String dataDir, String reason) {
        return "Không mở được dữ liệu của từ điển.\n\n"
                + "Thư mục dữ liệu: " + dataDir + "\n"
                + "Hãy kiểm tra thư mục này có file slang.original.txt và bạn có quyền đọc, ghi trong đó.\n\n"
                + "Chi tiết: " + reason;
    }

    // ------------------------------------------------------------------ sidebar

    public static String groupLabel(Page.Group group) {
        return switch (group) {
            case LOOKUP -> "Tra cứu";
            case MANAGE -> "Quản lý từ điển";
            case PLAY -> "Học và chơi";
        };
    }

    public static String navLabel(Page page) {
        return switch (page) {
            case SEARCH_SLANG -> "Tìm theo từ lóng";
            case SEARCH_DEFINITION -> "Tìm theo nghĩa";
            case HISTORY -> "Lịch sử tìm kiếm";
            case ADD -> "Thêm từ lóng";
            case EDIT -> "Sửa từ lóng";
            case DELETE -> "Xóa từ lóng";
            case RESET -> "Khôi phục dữ liệu gốc";
            case RANDOM -> "Từ lóng ngẫu nhiên";
            case QUIZ_SLANG_TO_MEANING -> "Đố nghĩa của từ lóng";
            case QUIZ_MEANING_TO_SLANG -> "Đố tìm từ lóng";
        };
    }

    public static String entryCount(int count) {
        return NumberFormat.getIntegerInstance(VIETNAMESE).format(count) + " từ lóng trong từ điển";
    }

    public static String indexInfo(boolean fromCache, long millis) {
        return (fromCache ? "Chỉ mục nạp từ bộ nhớ đệm" : "Chỉ mục được lập lại từ file dữ liệu")
                + " trong " + millis + " ms";
    }

    // ------------------------------------------------------------------ trang

    public static String pageTitle(Page page) {
        return navLabel(page);
    }

    public static String pageDescription(Page page) {
        return switch (page) {
            case SEARCH_SLANG -> "Nhập một từ lóng để xem nghĩa của nó. Không phân biệt chữ hoa và chữ thường.";
            case SEARCH_DEFINITION -> "Nhập một từ khóa để tìm mọi từ lóng có nghĩa chứa từ khóa đó.";
            case HISTORY -> "Xem lại các lần tìm kiếm gần đây, lần mới nhất ở trên cùng.";
            case ADD -> "Thêm một từ lóng mới cùng một hoặc nhiều nghĩa. "
                    + "Nếu từ đã có, bạn có thể ghi đè hoặc thêm một bản trùng.";
            case EDIT -> "Chọn một từ lóng có sẵn để đổi tên hoặc cập nhật nghĩa.";
            case DELETE -> "Chọn một từ lóng để xóa khỏi từ điển. Bạn sẽ được hỏi xác nhận trước khi xóa.";
            case RESET -> "Đưa từ điển về danh sách ban đầu. Mọi thay đổi của bạn sẽ mất, "
                    + "lịch sử tìm kiếm vẫn được giữ.";
            case RANDOM -> "Xem một từ lóng bất kỳ để khám phá thêm.";
            case QUIZ_SLANG_TO_MEANING -> "Cho một từ lóng, chọn nghĩa đúng trong bốn đáp án.";
            case QUIZ_MEANING_TO_SLANG -> "Cho một nghĩa, chọn từ lóng đúng trong bốn đáp án.";
        };
    }

    public static final String PLACEHOLDER_STATUS = "Màn hình này đang được xây dựng.";
}