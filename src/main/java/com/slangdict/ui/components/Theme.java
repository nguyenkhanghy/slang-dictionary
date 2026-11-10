package com.slangdict.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * Nơi DUY NHẤT định nghĩa màu, font và khoảng cách của giao diện (PROJECT.md 6.4).
 * Mọi component lấy style từ đây; không tạo {@code new Color(...)} / {@code new Font(...)} ở nơi khác.
 *
 * <p>Look and feel: Nimbus (có sẵn trong JDK) với bảng màu của Theme; nếu Nimbus không dùng được thì
 * dùng look and feel mặc định và vẫn chạy bình thường (các component chính tự vẽ).
 * Dark mode (U7) sẽ thay bộ hằng màu bằng một bảng màu khác; vì vậy KHÔNG đặt màu cứng ở component.
 */
public final class Theme {

    private static final Logger LOG = Logger.getLogger(Theme.class.getName());

    // ------------------------------------------------------------------ màu (giao diện sáng, một màu nhấn xanh dương)

    /** Màu nhấn: nút, mục điều hướng đang chọn, thanh tiến trình. Chữ trắng trên màu này đạt tương phản ≥ 5:1. */
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color ACCENT_DARK = new Color(0x1D4ED8);
    /** Nền nhạt cùng tông với màu nhấn: hover, vùng được nhấn nhẹ. */
    public static final Color ACCENT_SOFT = new Color(0xE6EDFD);
    public static final Color ON_ACCENT = Color.WHITE;

    /** Nền vùng nội dung. */
    public static final Color BACKGROUND = new Color(0xF5F7FB);
    /** Nền các bề mặt nổi trên nền (sidebar, thẻ, ô nhập). */
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xDEE3EC);

    public static final Color TEXT = new Color(0x1C2536);
    public static final Color TEXT_MUTED = new Color(0x5A6478);

    public static final Color SCROLL_THUMB = new Color(0xC3CBD9);
    public static final Color SCROLL_THUMB_HOVER = new Color(0x97A3B8);

    /** Chữ báo lỗi và nền nút nguy hiểm (chữ trắng trên màu này đạt tương phản ≥ 6:1). */
    public static final Color DANGER = new Color(0xB42318);
    /** Nền nút nguy hiểm khi rê chuột/nhấn. */
    public static final Color DANGER_DARK = new Color(0x912018);
    /** Nền dòng đang chọn trong bảng kết quả. */
    public static final Color TABLE_ROW_SELECTED = ACCENT_SOFT;
    /** Viền ô nhập; đổi sang màu nhấn khi ô đang có focus. */
    public static final Color INPUT_BORDER = BORDER;
    public static final Color INPUT_BORDER_FOCUS = ACCENT;
    /** Màu báo đúng / sai (viền, nền nhạt, chữ) trong màn trắc nghiệm. */
    public static final Color SUCCESS = new Color(0x15803D);
    public static final Color SUCCESS_SOFT = new Color(0xE7F6EC);
    public static final Color DANGER_SOFT = new Color(0xFDECEA);

    // ------------------------------------------------------------------ khoảng cách và kích thước (px logic)

    public static final int SPACE_XS = 4;
    public static final int SPACE_S = 8;
    public static final int SPACE_M = 16;
    public static final int SPACE_L = 24;
    public static final int SPACE_XL = 32;

    public static final int RADIUS = 10;
    public static final int SIDEBAR_WIDTH = 264;
    public static final int NAV_ITEM_HEIGHT = 36;
    /** Chiều rộng tối đa của một đoạn văn (giữ dòng chữ dễ đọc, dưới ~80 ký tự). */
    public static final int TEXT_MAX_WIDTH = 480;
    /** Chiều rộng tối đa của chữ trong thẻ trạng thái (nhỏ hơn TEXT_MAX_WIDTH để vừa thẻ ở cỡ cửa sổ tối thiểu). */
    public static final int STATE_TEXT_MAX_WIDTH = 420;
    /** Chiều cao ô nhập và nút hành động đi kèm. */
    public static final int INPUT_HEIGHT = 44;
    public static final int TABLE_HEADER_HEIGHT = 40;
    /** Chiều cao tối thiểu của một hàng trong bảng kết quả (hàng nhiều dòng sẽ cao hơn). */
    public static final int TABLE_ROW_MIN_HEIGHT = 40;
    public static final int TABLE_CELL_PAD_X = 12;
    public static final int TABLE_CELL_PAD_Y = 10;
    /** Chiều rộng cố định của cột "Từ lóng"; cột "Nghĩa" lấy phần còn lại. */
    public static final int TABLE_SLANG_COLUMN_WIDTH = 180;
    /** Bề rộng cố định của cột thời gian và cột loại trong bảng lịch sử; cột từ khóa lấy phần còn lại. */
    public static final int HISTORY_TIME_COLUMN_WIDTH = 170;
    public static final int HISTORY_TYPE_COLUMN_WIDTH = 130;
    /** Màn trắc nghiệm: bề rộng nội dung (câu hỏi, đáp án; vừa thẻ ở cỡ cửa sổ tối thiểu), lề và cỡ huy hiệu A/B/C/D. */
    public static final int QUIZ_CONTENT_WIDTH = 440;
    public static final int QUIZ_OPTION_PAD_X = 16;
    public static final int QUIZ_OPTION_PAD_Y = 12;
    public static final int QUIZ_BADGE_SIZE = 26;
    /** Bề rộng ô "Từ lóng" trong biểu mẫu thêm/sửa (ô "Nghĩa" lấy phần còn lại). */
    public static final int FORM_SLANG_FIELD_WIDTH = 200;
    /** Chiều cao tối thiểu dành cho dòng thông báo kết quả (tránh bố cục nhảy khi thông báo hiện/ẩn). */
    public static final int STATUS_LINE_MIN_HEIGHT = 40;
    /** Bề rộng chữ trong hộp thoại xác nhận/thông báo. */
    public static final int DIALOG_TEXT_WIDTH = 360;

    /** Kích thước mặc định của cửa sổ chính (quyết định Q5): 1000x680. */
    public static Dimension windowDefaultSize() {
        return new Dimension(1000, 680);
    }

    /** Kích thước tối thiểu của cửa sổ chính (quyết định Q5): 820x560. */
    public static Dimension windowMinimumSize() {
        return new Dimension(820, 560);
    }

    public static Dimension loadingWindowSize() {
        return new Dimension(440, 240);
    }

    // ------------------------------------------------------------------ font

    /** Chuỗi tiếng Việt có dấu dùng để kiểm tra font có hiển thị được hay không. */
    private static final String VIETNAMESE_SAMPLE = "ăâêôơưđ ạệịọụ ảẻỉỏủ ãẽĩõũ ắằẳẵặ ếềểễệ ốồổỗộ ớờởỡợ ứừửữự";
    private static final String[] FAMILY_PREFERENCE = {"Segoe UI", "Helvetica Neue", "Arial", "Noto Sans", "DejaVu Sans"};

    private static final String FONT_FAMILY = resolveFamily();

    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_NAV = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font FONT_WORDMARK = new Font(FONT_FAMILY, Font.BOLD, 20);

    private Theme() {
    }

    private static String resolveFamily() {
        Set<String> available = new HashSet<>(
                Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String name : FAMILY_PREFERENCE) {
            if (available.contains(name) && new Font(name, Font.PLAIN, 14).canDisplayUpTo(VIETNAMESE_SAMPLE) == -1) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }

    // ------------------------------------------------------------------ cài đặt look and feel

    /** Cài Nimbus và áp bảng màu/font của Theme. Gọi một lần trên EDT, trước khi tạo bất kỳ component nào. */
    public static void install() {
        // Các khóa này phải được đặt TRƯỚC khi cài Nimbus thì Nimbus mới dùng làm màu gốc.
        UIManager.put("defaultFont", FONT_BODY);
        UIManager.put("nimbusBase", ACCENT);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusOrange", ACCENT);
        UIManager.put("control", BACKGROUND);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("text", TEXT);
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException | RuntimeException e) {
            LOG.log(Level.WARNING, "Nimbus is unavailable, using the default look and feel: " + e, e);
        }
    }

    /**
     * Bản sao Graphics2D đã bật khử răng cưa cho hình và chữ (theo cấu hình của hệ điều hành).
     * Component tự vẽ phải gọi {@code dispose()} sau khi dùng.
     */
    public static Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Object desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map<?, ?> hints) {
            g2.addRenderingHints(hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        return g2;
    }
}