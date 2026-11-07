package com.slangdict.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.slangdict.index.SlangIndex;
import com.slangdict.model.HistoryEntry;
import com.slangdict.model.SearchType;
import com.slangdict.model.SlangWord;
import com.slangdict.repository.DataBootstrap;
import com.slangdict.repository.HistoryStore;
import com.slangdict.repository.IndexStore;
import com.slangdict.repository.SlangFileReader;
import com.slangdict.util.AppPaths;

/**
 * Logic nghiệp vụ trung tâm (tầng service). KHÔNG import javax.swing; UI chỉ gọi class này.
 *
 * <p>Phase 2: mở dữ liệu (nạp index từ cache hoặc build lại), F1 tìm theo slang, F2 tìm theo definition,
 * F3 history. Không thread-safe: gọi từ một luồng tại một thời điểm.
 */
public final class DictionaryService {

    private static final Logger LOG = Logger.getLogger(DictionaryService.class.getName());

    /** Số history hiển thị tối đa; file được cắt gọn khi vượt gấp đôi giá trị này. */
    public static final int MAX_HISTORY = 1000;

    private final SlangIndex index;
    private final HistoryStore historyStore;
    private final List<HistoryEntry> history; // cũ nhất trước
    private final boolean indexFromCache;
    private final long openMillis;

    private DictionaryService(SlangIndex index, HistoryStore historyStore, List<HistoryEntry> history,
                              boolean indexFromCache, long openMillis) {
        this.index = index;
        this.historyStore = historyStore;
        this.history = history;
        this.indexFromCache = indexFromCache;
        this.openMillis = openMillis;
    }

    /** Mở dịch vụ với các file trong thư mục dữ liệu mặc định ({@link AppPaths}). */
    public static DictionaryService open() throws IOException {
        DataBootstrap.ensureCurrentFile();
        return open(AppPaths.currentFile(), AppPaths.indexFile(), AppPaths.historyFile());
    }

    /**
     * Nạp index từ {@code indexFile} nếu còn hợp lệ, nếu không thì đọc {@code dataFile}, build index và lưu lại.
     */
    public static DictionaryService open(Path dataFile, Path indexFile, Path historyFile) throws IOException {
        long start = System.nanoTime();

        Optional<SlangIndex> cached = IndexStore.load(indexFile, dataFile);
        SlangIndex index;
        if (cached.isPresent()) {
            index = cached.get();
        } else {
            index = SlangIndex.build(SlangFileReader.read(dataFile));
            try {
                IndexStore.save(index, indexFile, dataFile);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Cannot save index file (will rebuild next time): " + e, e);
            }
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        HistoryStore store = new HistoryStore(historyFile);
        List<HistoryEntry> history;
        try {
            history = new ArrayList<>(store.loadAll());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Cannot read history file, starting with empty history: " + e, e);
            history = new ArrayList<>();
        }
        LOG.log(Level.INFO, "Opened {0} entries ({1}) in {2} ms",
                new Object[]{String.valueOf(index.size()), cached.isPresent() ? "index from cache" : "index rebuilt",
                        String.valueOf(millis)});
        return new DictionaryService(index, store, history, cached.isPresent(), millis);
    }

    public int size() {
        return index.size();
    }

    /** true nếu index được nạp từ file cache (không phải build lại). */
    public boolean isIndexFromCache() {
        return indexFromCache;
    }

    /** Thời gian mở dữ liệu (nạp/build index), tính bằng ms. */
    public long getOpenMillis() {
        return openMillis;
    }

    /** F1: tìm theo slang word. Query rỗng -> kết quả rỗng và không ghi history. */
    public List<SlangWord> searchBySlang(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<SlangWord> result = index.findBySlang(query);
        recordHistory(SearchType.SLANG, query);
        return result;
    }

    /** F2: tìm các slang có nghĩa chứa keyword. Keyword rỗng -> kết quả rỗng và không ghi history. */
    public List<SlangWord> searchByDefinition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<SlangWord> result = index.findByDefinition(keyword);
        recordHistory(SearchType.DEFINITION, keyword);
        return result;
    }

    /** F3: history, mới nhất trước, tối đa {@link #MAX_HISTORY} dòng. */
    public List<HistoryEntry> getHistory() {
        int from = Math.max(0, history.size() - MAX_HISTORY);
        List<HistoryEntry> recent = new ArrayList<>(history.subList(from, history.size()));
        Collections.reverse(recent);
        return recent;
    }

    public void clearHistory() {
        history.clear();
        try {
            historyStore.clear();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Cannot delete history file: " + e, e);
        }
    }

    private void recordHistory(SearchType type, String query) {
        HistoryEntry entry;
        try {
            entry = new HistoryEntry(Instant.now(), type, query);
        } catch (IllegalArgumentException e) {
            return;
        }
        history.add(entry);
        try {
            historyStore.append(entry);
            if (history.size() > 2 * MAX_HISTORY) {
                history.subList(0, history.size() - MAX_HISTORY).clear();
                historyStore.rewrite(history);
            }
        } catch (IOException e) {
            // history là tính năng phụ: lỗi ghi file không được làm hỏng việc tìm kiếm
            LOG.log(Level.WARNING, "Cannot write history file: " + e, e);
        }
    }
}
