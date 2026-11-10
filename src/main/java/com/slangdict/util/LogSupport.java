package com.slangdict.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nơi DUY NHẤT quyết định cách log lỗi kèm exception: mặc định chỉ MỘT dòng ngắn (tiếng Anh) ở terminal,
 * stack trace đầy đủ chỉ xuất hiện khi bật debug bằng {@code -Dslang.debug=true} hoặc khi logger được đặt mức FINE.
 *
 * <p>Không import Swing; dùng được ở mọi tầng. Mọi chỗ log kèm {@link Throwable} phải gọi class này thay vì
 * {@code LOG.log(level, msg, e)} để terminal không bị tràn stack trace.
 */
public final class LogSupport {

    /** Tên system property bật stack trace đầy đủ. */
    public static final String DEBUG_PROPERTY = "slang.debug";

    private static final String FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    private static final String ONE_LINE_FORMAT = "[%1$tT] %4$s: %5$s%6$s%n";

    private LogSupport() {
    }

    /**
     * Đặt định dạng log một dòng cho mỗi bản ghi (mặc định của JDK dùng hai dòng). Gọi ĐẦU TIÊN trong {@code main},
     * trước khi bất kỳ Logger nào được dùng. Không ghi đè nếu người dùng đã tự đặt định dạng.
     */
    public static void configure() {
        if (System.getProperty(FORMAT_PROPERTY) == null) {
            System.setProperty(FORMAT_PROPERTY, ONE_LINE_FORMAT);
        }
    }

    /** true nếu chạy với {@code -Dslang.debug=true}. */
    public static boolean isDebug() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    public static void warn(Logger log, String message, Throwable cause) {
        log(log, Level.WARNING, message, cause);
    }

    public static void severe(Logger log, String message, Throwable cause) {
        log(log, Level.SEVERE, message, cause);
    }

    /**
     * Log {@code message + ": " + mô tả ngắn của cause} ở mức {@code level}. Stack trace chỉ đi kèm khi debug
     * hoặc khi {@code level} đã là FINE trở xuống; còn lại stack trace được log riêng ở mức FINE (mặc định không hiện).
     */
    public static void log(Logger log, Level level, String message, Throwable cause) {
        if (!log.isLoggable(level)) {
            return;
        }
        String line = message + ": " + describe(cause);
        if (cause == null) {
            log.log(level, line);
        } else if (isDebug() || level.intValue() <= Level.FINE.intValue()) {
            log.log(level, line, cause);
        } else {
            log.log(level, line);
            log.log(Level.FINE, "Stack trace of the previous message", cause);
        }
    }

    /** Mô tả ngắn của exception: {@code TênLớp: thông điệp} (không có stack trace). */
    public static String describe(Throwable cause) {
        if (cause == null) {
            return "unknown error";
        }
        String text = cause.getClass().getSimpleName();
        String detail = cause.getMessage();
        return detail == null || detail.isBlank() ? text : text + ": " + detail;
    }
}