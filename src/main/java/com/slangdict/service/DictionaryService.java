package com.slangdict.service;

import com.slangdict.index.SlangIndex;
import com.slangdict.model.HistoryEntry;
import com.slangdict.model.SearchType;
import com.slangdict.model.SlangWord;
import com.slangdict.repository.DataBootstrap;
import com.slangdict.repository.HistoryStore;
import com.slangdict.repository.IndexStore;
import com.slangdict.repository.SlangFileReader;
import com.slangdict.repository.SlangFileWriter;
import com.slangdict.util.AppPaths;
import com.slangdict.util.TextNormalizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logic nghiệp vụ trung tâm (tầng service). KHÔNG import javax.swing; UI chỉ gọi class này.
 *
 * <p>Chức năng: mở dữ liệu (nạp index từ cache hoặc build lại), F1/F2 tìm kiếm, F3 history,
 * F4 add, F5 edit, F6 delete, F7 reset. Không thread-safe: gọi từ một luồng tại một thời điểm.
 *
 * <p>Quy tắc lưu trữ cho add/edit/delete: tạo danh sách mới sau thay đổi, GHI FILE TXT TRƯỚC (an toàn),
 * rồi mới cập nhật index trong RAM. Nếu ghi file lỗi ({@link IOException}) thì RAM không đổi, nên RAM và file
 * không bao giờ lệch nhau. Index trên đĩa chỉ được ghi ở {@link #close()}; nếu chương trình bị tắt đột ngột,
 * lần sau CRC32 của file txt không khớp và index tự được build lại.
 * UI chịu trách nhiệm hỏi xác nhận (delete/reset/overwrite) trước khi gọi các hàm phá hủy.
 */
public final class DictionaryService {

    private static final Logger LOG = Logger.getLogger(DictionaryService.class.getName());

    /** Số history hiển thị tối đa; file được cắt gọn khi vượt gấp đôi giá trị này. */
    public static final int MAX_HISTORY = 1000;

    private SlangIndex index;
    private final Path dataFile;
    private final Path indexFile;
    private final Path originalFile;
    private final HistoryStore historyStore;
    private final List<HistoryEntry> history; // cũ nhất trước
    private final boolean indexFromCache;
    private final long openMillis;
    private boolean indexDirty; // index trong RAM mới hơn file index trên đĩa

    private DictionaryService(SlangIndex index, Path dataFile, Path indexFile, Path originalFile,
                              HistoryStore historyStore, List<HistoryEntry> history,
                              boolean indexFromCache, long openMillis) {
        this.index = index;
        this.dataFile = dataFile;
        this.indexFile = indexFile;
        this.originalFile = originalFile;
        this.historyStore = historyStore;
        this.history = history;
        this.indexFromCache = indexFromCache;
        this.openMillis = openMillis;
    }

    // ------------------------------------------------------------------ open / close

    /** Mở dịch vụ với các file trong thư mục dữ liệu mặc định ({@link AppPaths}). */
    public static DictionaryService open() throws IOException {
        DataBootstrap.ensureCurrentFile();
        return open(AppPaths.currentFile(), AppPaths.indexFile(), AppPaths.historyFile(), AppPaths.originalFile());
    }

    /** Như {@link #open(Path, Path, Path, Path)}; file gốc là {@code slang.original.txt} cùng thư mục với dataFile. */
    public static DictionaryService open(Path dataFile, Path indexFile, Path historyFile) throws IOException {
        return open(dataFile, indexFile, historyFile, dataFile.resolveSibling(AppPaths.ORIGINAL_NAME));
    }

    /**
     * Nạp index từ {@code indexFile} nếu còn hợp lệ, nếu không thì đọc {@code dataFile}, build index và lưu lại.
     * {@code originalFile} chỉ được đọc (dùng cho {@link #resetToOriginal()}).
     */
    public static DictionaryService open(Path dataFile, Path indexFile, Path historyFile, Path originalFile)
            throws IOException {
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
        return new DictionaryService(index, dataFile, indexFile, originalFile, store, history,
                cached.isPresent(), millis);
    }

    /**
     * Ghi index ra đĩa nếu có thay đổi chưa lưu. Gọi khi thoát ứng dụng; gọi nhiều lần vẫn an toàn.
     * Lỗi ghi chỉ được log (lần sau index sẽ tự build lại).
     */
    public void close() {
        if (!indexDirty) {
            return;
        }
        try {
            IndexStore.save(index, indexFile, dataFile);
            indexDirty = false;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Cannot save index on close (will rebuild next time): " + e, e);
        }
    }

    // ------------------------------------------------------------------ info

    public int size() {
        return index.size();
    }

    /** Toàn bộ slang theo thứ tự lưu (view không thể sửa đổi; đừng giữ lại sau khi gọi hàm ghi). */
    public List<SlangWord> getAllWords() {
        return index.all();
    }

    /** true nếu index được nạp từ file cache khi mở (không phải build lại). */
    public boolean isIndexFromCache() {
        return indexFromCache;
    }

    /** Thời gian mở dữ liệu (nạp/build index), tính bằng ms. */
    public long getOpenMillis() {
        return openMillis;
    }

    // ------------------------------------------------------------------ search (F1, F2, F3)

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

    /** Tra cứu chính xác theo slang NHƯNG KHÔNG ghi history (dùng để kiểm tra trùng, chọn entry để sửa/xóa). */
    public List<SlangWord> lookupSlang(String slang) {
        return index.findBySlang(slang);
    }

    /** Slang đã tồn tại chưa (không phân biệt hoa/thường), không ghi history. */
    public boolean exists(String slang) {
        return index.findIdsBySlang(slang).length > 0;
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

    // ------------------------------------------------------------------ modify (F4, F5, F6, F7)

    /**
     * F4: thêm slang mới.
     *
     * @param meanings danh sách nghĩa (UI có thể dùng {@link InputValidator#parseMeanings} để tách từ ô nhập)
     * @param policy   cách xử lý nếu khóa đã tồn tại; nếu chưa tồn tại thì luôn thêm
     * @throws ValidationException dữ liệu không hợp lệ (E5)
     * @throws IOException         ghi file lỗi; khi đó không có gì thay đổi
     */
    public AddResult addSlang(String slang, List<String> meanings, DuplicatePolicy policy) throws IOException {
        Objects.requireNonNull(policy, "policy");
        SlangWord word = InputValidator.toSlangWord(slang, meanings);
        int[] existing = index.findIdsBySlang(word.getSlang());

        if (existing.length == 0 || policy == DuplicatePolicy.DUPLICATE) {
            List<SlangWord> next = new ArrayList<>(index.all());
            next.add(word);
            SlangFileWriter.write(dataFile, next);
            index.add(word);
            indexDirty = true;
            return AddResult.ADDED;
        }
        if (policy == DuplicatePolicy.REJECT) {
            return AddResult.CONFLICT;
        }

        // OVERWRITE: entry mới thay vào vị trí của entry đầu tiên; các entry cùng khóa còn lại bị xóa
        List<SlangWord> next = new ArrayList<>(index.all());
        next.set(existing[0], word);
        for (int i = existing.length - 1; i >= 1; i--) {
            next.remove(existing[i]);
        }
        SlangFileWriter.write(dataFile, next);
        index.replaceAt(existing[0], word);
        for (int i = existing.length - 1; i >= 1; i--) {
            index.removeAt(existing[i]);
        }
        indexDirty = true;
        return AddResult.OVERWRITTEN;
    }

    /**
     * F5: sửa một entry (slang và/hoặc nghĩa). {@code target} là entry lấy từ kết quả tìm kiếm.
     * Đổi slang sang khóa mà entry KHÁC đã dùng → {@link EditResult#CONFLICT} (không thay đổi gì).
     *
     * @throws ValidationException dữ liệu mới không hợp lệ
     * @throws IOException         ghi file lỗi; khi đó không có gì thay đổi
     */
    public EditResult editSlang(SlangWord target, String newSlang, List<String> newMeanings) throws IOException {
        Objects.requireNonNull(target, "target");
        SlangWord word = InputValidator.toSlangWord(newSlang, newMeanings);
        int id = index.findId(target);
        if (id < 0) {
            return EditResult.NOT_FOUND;
        }
        if (index.all().get(id).equals(word)) {
            return EditResult.UNCHANGED;
        }
        boolean keyChanged = !TextNormalizer.normalizeSlang(target.getSlang())
                .equals(TextNormalizer.normalizeSlang(word.getSlang()));
        if (keyChanged && index.findIdsBySlang(word.getSlang()).length > 0) {
            return EditResult.CONFLICT;
        }
        List<SlangWord> next = new ArrayList<>(index.all());
        next.set(id, word);
        SlangFileWriter.write(dataFile, next);
        index.replaceAt(id, word);
        indexDirty = true;
        return EditResult.EDITED;
    }

    /**
     * F6: xóa một entry (lấy từ kết quả tìm kiếm). UI phải hỏi xác nhận trước khi gọi.
     *
     * @return true nếu đã xóa, false nếu không tìm thấy entry
     * @throws IOException ghi file lỗi; khi đó không có gì thay đổi
     */
    public boolean deleteSlang(SlangWord target) throws IOException {
        Objects.requireNonNull(target, "target");
        int id = index.findId(target);
        if (id < 0) {
            return false;
        }
        List<SlangWord> next = new ArrayList<>(index.all());
        next.remove(id);
        SlangFileWriter.write(dataFile, next);
        index.removeAt(id);
        indexDirty = true;
        return true;
    }

    /**
     * F7: khôi phục danh sách slang gốc. File gốc không bị sửa; history được giữ nguyên.
     * UI phải hỏi xác nhận trước khi gọi.
     *
     * @throws FileNotFoundException không có file gốc
     * @throws IOException           lỗi đọc/ghi; khi đó không có gì thay đổi
     */
    public void resetToOriginal() throws IOException {
        if (!Files.isRegularFile(originalFile)) {
            throw new FileNotFoundException("Original data file not found: " + originalFile);
        }
        SlangIndex fresh = SlangIndex.build(SlangFileReader.read(originalFile));
        DataBootstrap.replaceWithCopy(originalFile, dataFile);
        index = fresh;
        try {
            IndexStore.save(index, indexFile, dataFile);
            indexDirty = false;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Cannot save index after reset (will be retried on close): " + e, e);
            indexDirty = true;
        }
    }

    // ------------------------------------------------------------------ internals

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
