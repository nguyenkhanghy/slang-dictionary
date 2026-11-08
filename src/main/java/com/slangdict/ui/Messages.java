package com.slangdict.ui;

import com.slangdict.model.SearchType;

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

    // ------------------------------------------------------------------ màn tìm kiếm (F1, F2)

    /** Số ký tự tối đa của từ khóa được trích lại trong thông báo (từ khóa dài bị cắt bằng "…"). */
    private static final int QUERY_PREVIEW_LENGTH = 24;

    public static final String SEARCH_BUTTON = "Tìm";
    public static final String SEARCH_CLEAR = "Xóa nội dung đã nhập";
    public static final String COLUMN_SLANG = "Từ lóng";
    public static final String COLUMN_MEANING = "Nghĩa";

    public static final String SEARCH_ERROR_TITLE = "Không thể thực hiện tìm kiếm";
    public static final String SEARCH_ERROR_DETAIL =
            "Đã xảy ra lỗi bất ngờ. Bạn hãy thử lại; nếu lỗi vẫn còn, hãy đóng và mở lại ứng dụng.";

    public static String searchPlaceholder(SearchType type) {
        return switch (type) {
            case SLANG -> "Nhập từ lóng cần tra…";
            case DEFINITION -> "Nhập từ khóa trong nghĩa, ví dụ: love";
        };
    }

    public static String searchIdleTitle(SearchType type) {
        return switch (type) {
            case SLANG -> "Chưa tra từ nào";
            case DEFINITION -> "Chưa tìm từ khóa nào";
        };
    }

    public static String searchIdleDetail(SearchType type) {
        return switch (type) {
            case SLANG -> "Nhập một từ lóng vào ô phía trên, rồi nhấn Enter hoặc nút Tìm.";
            case DEFINITION -> "Nhập một từ khóa vào ô phía trên, rồi nhấn Enter hoặc nút Tìm "
                    + "để xem mọi từ lóng có nghĩa chứa từ khóa đó.";
        };
    }

    public static final String SEARCH_EMPTY_INPUT_TITLE = "Ô tìm kiếm đang trống";
    public static final String SEARCH_EMPTY_INPUT_DETAIL = "Hãy nhập nội dung cần tìm, rồi nhấn Enter hoặc nút Tìm.";

    public static String searchingTitle(String query) {
        return "Đang tìm “" + preview(query) + "”…";
    }

    public static final String SEARCHING_DETAIL = "Vui lòng chờ một chút.";

    public static String notFoundTitle(String query) {
        return "Không tìm thấy kết quả cho “" + preview(query) + "”";
    }

    public static String notFoundDetail(SearchType type) {
        return switch (type) {
            case SLANG -> "Hãy kiểm tra lại chính tả, hoặc thử tìm theo nghĩa ở mục “"
                    + navLabel(Page.SEARCH_DEFINITION) + "”.";
            case DEFINITION -> "Hãy thử một từ khóa ngắn hơn hoặc một từ khác.";
        };
    }

    public static String resultCount(int count) {
        return NumberFormat.getIntegerInstance(VIETNAMESE).format(count) + " kết quả";
    }

    /** Rút gọn từ khóa người dùng nhập để thông báo không bị dài quá khổ. */
    private static String preview(String query) {
        String text = query.strip();
        if (text.codePointCount(0, text.length()) <= QUERY_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, QUERY_PREVIEW_LENGTH)) + "…";
    }
}