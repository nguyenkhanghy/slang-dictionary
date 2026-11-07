package com.slangdict.index;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.slangdict.model.SlangWord;
import com.slangdict.util.TextNormalizer;

/**
 * Cấu trúc dữ liệu chính trong RAM (xem PROJECT.md mục 3.4).
 *
 * <ul>
 *   <li>{@code entries}: toàn bộ slang, id = vị trí trong list (giữ thứ tự file);</li>
 *   <li>{@code bySlang}: slang chuẩn hóa (IN HOA) -> id các entry. Dùng List vì một slang có thể có nhiều entry;
 *       tra cứu O(1) cho F1;</li>
 *   <li>{@code byToken}: inverted index, token trong nghĩa (in thường) -> id các entry chứa token. Dùng cho F2.</li>
 * </ul>
 *
 * <p>Tìm theo definition (F2) khớp theo CHUỖI CON không phân biệt hoa thường: "lov" khớp nghĩa chứa "love".
 * Cách làm: quét từ vựng (vài chục nghìn token) tìm token chứa keyword, hợp các posting list; keyword nhiều
 * từ thì giao các tập; cuối cùng kiểm tra lại bằng contains khi keyword có ký tự ngoài chữ/số hoặc nhiều từ.
 *
 * <p>Lớp này không thread-safe và (ở Phase 2) chỉ đọc. Có thể ghi/đọc dạng nhị phân qua
 * {@link #writeTo} / {@link #readFrom}.
 */
public final class SlangIndex {

    private static final int MAX_ENTRIES = 50_000_000;
    private static final int MAX_MEANINGS_PER_ENTRY = 10_000;

    private final List<SlangWord> entries;
    private final Map<String, IntList> bySlang;
    private final Map<String, IntList> byToken;

    private SlangIndex(List<SlangWord> entries, Map<String, IntList> bySlang, Map<String, IntList> byToken) {
        this.entries = entries;
        this.bySlang = bySlang;
        this.byToken = byToken;
    }

    // ------------------------------------------------------------------ build

    public static SlangIndex build(List<SlangWord> words) {
        List<SlangWord> entries = new ArrayList<>(words);
        Map<String, IntList> bySlang = new HashMap<>(capacityFor(entries.size()));
        Map<String, IntList> byToken = new HashMap<>(capacityFor(entries.size() / 2));
        for (int id = 0; id < entries.size(); id++) {
            SlangWord word = entries.get(id);
            bySlang.computeIfAbsent(TextNormalizer.normalizeSlang(word.getSlang()), k -> new IntList()).add(id);
            for (String meaning : word.getMeanings()) {
                for (String token : TextNormalizer.tokenize(TextNormalizer.normalizeText(meaning))) {
                    byToken.computeIfAbsent(token, k -> new IntList()).add(id);
                }
            }
        }
        return new SlangIndex(entries, bySlang, byToken);
    }

    // ------------------------------------------------------------------ queries

    public int size() {
        return entries.size();
    }

    /** Toàn bộ entry theo thứ tự file (không thể sửa đổi). */
    public List<SlangWord> all() {
        return Collections.unmodifiableList(entries);
    }

    /** F1: tìm chính xác theo slang (không phân biệt hoa/thường). Trả về mọi entry trùng key, theo thứ tự file. */
    public List<SlangWord> findBySlang(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        IntList ids = bySlang.get(TextNormalizer.normalizeSlang(query));
        if (ids == null) {
            return List.of();
        }
        List<SlangWord> result = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            result.add(entries.get(ids.get(i)));
        }
        return result;
    }

    /** F2: mọi entry có ít nhất một nghĩa chứa keyword (chuỗi con, không phân biệt hoa/thường), theo thứ tự file. */
    public List<SlangWord> findByDefinition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = TextNormalizer.normalizeText(keyword);
        List<String> queryTokens = TextNormalizer.tokenize(kw);

        BitSet candidates;
        if (queryTokens.isEmpty()) {
            // keyword toàn ký tự đặc biệt (vd ":)") -> không có token để tra, duyệt toàn bộ
            candidates = new BitSet(entries.size());
            candidates.set(0, entries.size());
        } else {
            candidates = null;
            for (String token : new LinkedHashSet<>(queryTokens)) {
                BitSet ids = idsOfTokensContaining(token);
                if (candidates == null) {
                    candidates = ids;
                } else {
                    candidates.and(ids);
                }
                if (candidates.isEmpty()) {
                    return List.of();
                }
            }
        }

        // Keyword chỉ gồm 1 cụm chữ/số liên tiếp: một nghĩa chứa keyword <=> có token chứa keyword,
        // nên ứng viên đã chính xác, không cần kiểm tra lại.
        boolean needVerify = !(queryTokens.size() == 1 && queryTokens.get(0).length() == kw.length());

        List<SlangWord> result = new ArrayList<>();
        for (int id = candidates.nextSetBit(0); id >= 0; id = candidates.nextSetBit(id + 1)) {
            SlangWord word = entries.get(id);
            if (!needVerify || anyMeaningContains(word, kw)) {
                result.add(word);
            }
        }
        return result;
    }

    private BitSet idsOfTokensContaining(String part) {
        BitSet bits = new BitSet(entries.size());
        for (Map.Entry<String, IntList> e : byToken.entrySet()) {
            if (e.getKey().contains(part)) {
                e.getValue().addTo(bits);
            }
        }
        return bits;
    }

    private static boolean anyMeaningContains(SlangWord word, String lowerKeyword) {
        for (String meaning : word.getMeanings()) {
            if (meaning.toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ persistence (binary)

    /** Ghi toàn bộ cấu trúc ra stream. Header/magic/checksum do IndexStore quản lý. */
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(entries.size());
        for (SlangWord word : entries) {
            out.writeUTF(word.getSlang());
            List<String> meanings = word.getMeanings();
            out.writeInt(meanings.size());
            for (String m : meanings) {
                out.writeUTF(m);
            }
        }
        writeMap(out, bySlang);
        writeMap(out, byToken);
    }

    /** Đọc lại cấu trúc đã ghi bởi {@link #writeTo}. Dữ liệu hỏng -> IOException/IllegalArgumentException. */
    public static SlangIndex readFrom(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IOException("Corrupt entry count: " + count);
        }
        List<SlangWord> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String slang = in.readUTF();
            int meaningCount = in.readInt();
            if (meaningCount < 1 || meaningCount > MAX_MEANINGS_PER_ENTRY) {
                throw new IOException("Corrupt meaning count: " + meaningCount);
            }
            List<String> meanings = new ArrayList<>(meaningCount);
            for (int j = 0; j < meaningCount; j++) {
                meanings.add(in.readUTF());
            }
            entries.add(new SlangWord(slang, meanings));
        }
        Map<String, IntList> bySlang = readMap(in, count);
        Map<String, IntList> byToken = readMap(in, count);
        return new SlangIndex(entries, bySlang, byToken);
    }

    private static void writeMap(DataOutputStream out, Map<String, IntList> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<String, IntList> e : map.entrySet()) {
            out.writeUTF(e.getKey());
            e.getValue().writeTo(out);
        }
    }

    private static Map<String, IntList> readMap(DataInputStream in, int entryCount) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > MAX_ENTRIES) {
            throw new IOException("Corrupt map size: " + n);
        }
        Map<String, IntList> map = new HashMap<>(capacityFor(n));
        for (int i = 0; i < n; i++) {
            map.put(in.readUTF(), IntList.readFrom(in, entryCount));
        }
        return map;
    }

    private static int capacityFor(int expected) {
        return (int) (expected / 0.75f) + 16;
    }
}
