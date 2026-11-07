package com.slangdict.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Một lần tìm kiếm trong history. Query được làm sạch (tab/xuống dòng -> dấu cách, strip)
 * để ghi 1 dòng/entry vào file. Query rỗng -> {@link IllegalArgumentException}.
 */
public record HistoryEntry(Instant time, SearchType type, String query) {

    public HistoryEntry {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(query, "query");
        query = query.replaceAll("[\\t\\r\\n]+", " ").strip();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("query must not be empty");
        }
    }
}
