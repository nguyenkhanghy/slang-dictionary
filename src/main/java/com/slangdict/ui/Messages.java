package com.slangdict.ui;

import java.text.NumberFormat;
import java.util.Locale;

import com.slangdict.model.SearchType;
import com.slangdict.service.ValidationException;

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

    public static final String ERROR_UNEXPECTED_TITLE = "Đã xảy ra lỗi";
    public static final String ERROR_UNEXPECTED_MESSAGE = "Chương trình gặp một lỗi không mong muốn.\nDữ liệu của bạn không bị ảnh hưởng. Nếu lỗi lặp lại, hãy đóng và mở lại ứng dụng.";
    
    public static final String HEADLESS =
            "Không tìm thấy màn hình để hiển thị giao diện (môi trường không có đồ họa).\n"
                    + "Hãy chạy ứng dụng trên máy có giao diện đồ họa.";

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

    // ------------------------------------------------------------------ chung cho các màn U4

    public static final String UNEXPECTED_ERROR_DETAIL =
            "Đã xảy ra lỗi bất ngờ. Bạn hãy thử lại; nếu lỗi vẫn còn, hãy đóng và mở lại ứng dụng.";

    // ------------------------------------------------------------------ màn từ lóng ngẫu nhiên (F8)

    public static final String RANDOM_BUTTON = "Từ khác";
    public static final String RANDOM_LABEL_SLANG = "Từ lóng";
    public static final String RANDOM_LABEL_MEANING = "Nghĩa";
    public static final String RANDOM_EMPTY_TITLE = "Từ điển đang trống";
    public static final String RANDOM_ERROR_TITLE = "Không thể chọn từ ngẫu nhiên";
    public static final String RANDOM_TODAY_CAPTION = "Từ lóng của hôm nay";
    public static final String RANDOM_PICKED_CAPTION = "Từ lóng ngẫu nhiên";

    public static String randomEmptyDetail() {
        return "Hãy thêm từ lóng ở mục “" + navLabel(Page.ADD) + "” hoặc khôi phục dữ liệu gốc.";
    }

    // ------------------------------------------------------------------ màn lịch sử tìm kiếm (F3)

    public static final String HISTORY_CLEAR_BUTTON = "Xóa lịch sử";
    public static final String HISTORY_TIME_PATTERN = "dd/MM/yyyy HH:mm";
    public static final String COLUMN_TIME = "Thời gian";
    public static final String COLUMN_TYPE = "Loại";
    public static final String COLUMN_QUERY = "Từ khóa";
    public static final String HISTORY_EMPTY_TITLE = "Chưa có lịch sử tìm kiếm";
    public static final String HISTORY_ERROR_TITLE = "Không thể tải lịch sử tìm kiếm";
    public static final String HISTORY_CONFIRM_TITLE = "Xóa lịch sử tìm kiếm";
    public static final String HISTORY_CONFIRM_MESSAGE =
            "Bạn có chắc muốn xóa toàn bộ lịch sử tìm kiếm? Thao tác này không thể hoàn tác.";
    public static final String HISTORY_CONFIRM_YES = "Xóa";
    public static final String HISTORY_CONFIRM_NO = "Hủy";

    public static String historyEmptyDetail() {
        return "Các lần tìm ở mục “" + navLabel(Page.SEARCH_SLANG) + "” và “" + navLabel(Page.SEARCH_DEFINITION)
                + "” sẽ hiện ở đây.";
    }

    public static String historyType(SearchType type) {
        return switch (type) {
            case SLANG -> "Theo từ lóng";
            case DEFINITION -> "Theo nghĩa";
        };
    }

    public static String historyCount(int count) {
        return NumberFormat.getIntegerInstance(VIETNAMESE).format(count) + " lần tìm";
    }

    // ------------------------------------------------------------------ màn trắc nghiệm (F9, F10)

    public static final String QUIZ_NEXT_BUTTON = "Câu tiếp theo";
    public static final String QUIZ_CAPTION_SLANG = "Từ lóng";
    public static final String QUIZ_CAPTION_MEANING = "Nghĩa";
    public static final String QUIZ_CORRECT = "Chính xác!";
    public static final String QUIZ_WRONG = "Chưa đúng. Đáp án đúng được tô màu xanh.";
    public static final String QUIZ_EMPTY_TITLE = "Chưa đủ dữ liệu để ra câu hỏi";
    public static final String QUIZ_ERROR_TITLE = "Không thể tạo câu hỏi";

    public static String quizEmptyDetail() {
        return "Cần ít nhất bốn từ lóng có nghĩa khác nhau. Hãy thêm từ lóng ở mục “" + navLabel(Page.ADD)
                + "” hoặc khôi phục dữ liệu gốc.";
    }

    // ------------------------------------------------------------------ quản lý từ điển (F4–F7) và hộp thoại (U5)

    public static final String FORM_SLANG_HINT = "Từ lóng";
    public static final String FORM_MEANINGS_HINT = "Nghĩa 1 | Nghĩa 2";
    public static final String PICKER_IDLE_TITLE = "Chưa chọn từ lóng nào";
    public static final String SAVE_ERROR_TITLE = "Không thể lưu dữ liệu";
    public static final String SAVE_ERROR_DETAIL =
            "Không ghi được file dữ liệu. Hãy kiểm tra quyền ghi trong thư mục dữ liệu rồi thử lại. "
                    + "Từ điển chưa bị thay đổi.";

    public static final String ADD_BUTTON = "Thêm";
    public static final String EDIT_SAVE_BUTTON = "Lưu";
    public static final String DELETE_BUTTON = "Xóa mục đã chọn";
    public static final String RESET_BUTTON = "Khôi phục dữ liệu gốc";

    public static final String EDIT_IDLE_DETAIL =
            "Nhập một từ lóng và nhấn Tìm, chọn một dòng trong kết quả rồi sửa nội dung ở phía dưới.";
    public static final String DELETE_IDLE_DETAIL =
            "Nhập một từ lóng và nhấn Tìm, chọn một dòng trong kết quả rồi bấm “" + DELETE_BUTTON + "”.";

    public static String addedMessage(String slang) {
        return "Đã thêm “" + preview(slang) + "” vào từ điển.";
    }

    public static String overwrittenMessage(String slang) {
        return "Đã ghi đè “" + preview(slang) + "” bằng nội dung mới.";
    }

    public static String editedMessage(String slang) {
        return "Đã lưu thay đổi cho “" + preview(slang) + "”.";
    }

    public static final String EDIT_UNCHANGED = "Chưa có thay đổi nào để lưu.";
    public static final String EDIT_NOT_FOUND = "Mục này không còn trong từ điển (có thể đã bị xóa hoặc đổi). Hãy tìm lại.";

    public static String editConflictMessage(String slang) {
        return "Từ lóng “" + preview(slang) + "” đã có trong từ điển. Hãy chọn một tên khác.";
    }

    public static String deletedMessage(String slang) {
        return "Đã xóa “" + preview(slang) + "” khỏi từ điển.";
    }

    public static final String DELETE_NOT_FOUND = "Mục này đã không còn trong từ điển.";
    public static final String DELETE_CONFIRM_TITLE = "Xóa từ lóng";
    public static final String DELETE_CONFIRM_YES = "Xóa";
    public static final String DELETE_CONFIRM_NO = "Hủy";

    public static String deleteConfirmMessage(String slang) {
        return "Bạn có chắc muốn xóa “" + preview(slang) + "” khỏi từ điển? Thao tác này không thể hoàn tác.";
    }

    public static final String RESET_CONFIRM_TITLE = "Khôi phục dữ liệu gốc";
    public static final String RESET_CONFIRM_MESSAGE =
            "Mọi thay đổi của bạn trong từ điển sẽ mất; lịch sử tìm kiếm vẫn được giữ. Bạn có chắc muốn khôi phục?";
    public static final String RESET_CONFIRM_YES = "Khôi phục";
    public static final String RESET_CONFIRM_NO = "Hủy";
    public static final String RESET_MISSING_ORIGINAL =
            "Không tìm thấy file dữ liệu gốc (slang.original.txt) trong thư mục dữ liệu nên chưa khôi phục được.";
    public static final String RESET_ERROR =
            "Không khôi phục được dữ liệu. Từ điển chưa bị thay đổi; hãy kiểm tra quyền đọc, ghi trong thư mục dữ liệu.";

    public static String resetDoneMessage(int count) {
        return "Đã khôi phục dữ liệu gốc: " + NumberFormat.getIntegerInstance(VIETNAMESE).format(count)
                + " từ lóng.";
    }

    public static final String DUPLICATE_TITLE = "Từ lóng đã tồn tại";
    public static final String DUPLICATE_OVERWRITE = "Ghi đè";
    public static final String DUPLICATE_ADD = "Thêm bản trùng";
    public static final String DUPLICATE_CANCEL = "Hủy";

    public static String duplicateMessage(String slang) {
        return "“" + preview(slang) + "” đã có trong từ điển. Ghi đè sẽ thay mọi bản cũ bằng nội dung mới; "
                + "thêm bản trùng sẽ giữ bản cũ và thêm một mục nữa. Bạn muốn làm gì?";
    }

    /** Thông báo thân thiện cho từng lý do dữ liệu nhập không hợp lệ ({@code InputValidator}). */
    public static String validationMessage(ValidationException.Reason reason) {
        return switch (reason) {
            case SLANG_EMPTY -> "Hãy nhập từ lóng.";
            case SLANG_INVALID_CHAR -> "Từ lóng không được chứa dấu ` hoặc xuống dòng.";
            case NO_MEANING -> "Hãy nhập ít nhất một nghĩa.";
            case MEANING_EMPTY -> "Có một nghĩa đang để trống.";
            case MEANING_INVALID_CHAR -> "Nghĩa không được chứa dấu ` hoặc xuống dòng; nhiều nghĩa hãy ngăn bằng dấu |.";
        };
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