package com.slangdict;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

import com.slangdict.model.SlangWord;
import com.slangdict.repository.SlangFileReader;
import com.slangdict.service.DictionaryService;
import com.slangdict.util.TextNormalizer;

/**
 * Kiểm tra ĐÚNG và NHANH của Phase 2 trên bộ dữ liệu lớn (task C7). Không cần JUnit.
 *
 * <pre>
 *   java tools/GenerateBigData.java                  # sinh data/slang.100k.txt (1 lần)
 *   mvn clean package
 *   java -cp target/classes:target/test-classes com.slangdict.PerfCheck [duongDanFileLon]
 *   (Windows: dùng dấu ; thay cho : trong classpath)
 * </pre>
 *
 * Dữ liệu được copy vào target/perf-data nên không đụng tới thư mục data/ thật.
 * Mọi truy vấn phải < 1000 ms; thoát mã 1 nếu sai kết quả hoặc quá chậm.
 */
public class PerfCheck {

    private static final double LIMIT_MS = 1000.0;
    private static final String[] DEFINITION_KEYWORDS = {
            "love", "the", "a", "money", "lov", "i love you", "c++", ":)", "'s", "xyzzy123", "  Drunk  "
    };

    private static boolean failed = false;      // quá chậm hoặc sai kết quả
    private static boolean mismatch = false;    // riêng cho phần kiểm tra đúng đắn
    private static double worstMs = 0;

    public static void main(String[] args) throws Exception {
        Path big = Paths.get(args.length > 0 ? args[0] : "data/slang.100k.txt");
        if (!Files.isRegularFile(big)) {
            System.err.println("Not found: " + big + "\nGenerate it first: java tools/GenerateBigData.java");
            System.exit(2);
        }
        Path dir = Paths.get("target", "perf-data").toAbsolutePath();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : (Iterable<Path>) files::iterator) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
        Files.copy(big, dir.resolve("slang.original.txt"));
        System.setProperty("slang.dataDir", dir.toString()); // phải đặt TRƯỚC khi AppPaths được nạp

        List<SlangWord> words = SlangFileReader.read(big);
        System.out.println("Dataset: " + words.size() + " entries from " + big);
        System.out.println();

        // 1. mở lần đầu (build index + lưu) rồi mở lần hai (nạp từ cache)
        DictionaryService built = DictionaryService.open();
        System.out.printf("Open #1 (build + save index) : %7d ms  fromCache=%b%n", built.getOpenMillis(), built.isIndexFromCache());
        check("first open must build", !built.isIndexFromCache());
        long indexBytes = Files.size(dir.resolve("slang.index.bin"));
        System.out.printf("Index file size              : %7.1f MB%n", indexBytes / 1048576.0);

        DictionaryService svc = DictionaryService.open();
        System.out.printf("Open #2 (load cached index)  : %7d ms  fromCache=%b%n", svc.getOpenMillis(), svc.isIndexFromCache());
        check("second open must use cache", svc.isIndexFromCache());
        track(svc.getOpenMillis(), "open from cache");
        // Build index chỉ xảy ra khi chưa có cache (1 lần), nên chỉ báo thông tin, không tính vào PASS/FAIL.
        System.out.println();

        // 2. đúng đắn: so sánh kết quả của index với duyệt tuần tự (cả bản build lẫn bản nạp từ cache)
        Random rnd = new Random(7);
        List<String> slangQueries = new ArrayList<>();
        for (int i = 0; i < 500; i++) slangQueries.add(words.get(rnd.nextInt(words.size())).getSlang());
        slangQueries.add("ama"); slangQueries.add("  Ama "); slangQueries.add("no-such-slang-xyz");
        for (String q : slangQueries) {
            List<SlangWord> expected = bruteSlang(words, q);
            check("F1 mismatch (built) for " + q, expected.equals(built.searchBySlang(q)));
            check("F1 mismatch (cached) for " + q, expected.equals(svc.searchBySlang(q)));
        }
        for (String kw : DEFINITION_KEYWORDS) {
            List<SlangWord> expected = bruteDefinition(words, kw);
            check("F2 mismatch (built) for '" + kw + "'", expected.equals(built.searchByDefinition(kw)));
            check("F2 mismatch (cached) for '" + kw + "'", expected.equals(svc.searchByDefinition(kw)));
        }
        System.out.println("Correctness: index results compared with brute-force scan -> " + (mismatch ? "MISMATCH" : "OK"));
        System.out.println();

        // 3. tốc độ: truy vấn ĐẦU TIÊN sau khi mở (JVM còn nguội - giống lúc giảng viên chấm)
        DictionaryService fresh = DictionaryService.open();
        String firstKey = words.get(words.size() / 2).getSlang();
        long t0 = System.nanoTime();
        fresh.searchBySlang(firstKey);
        double firstF1 = ms(t0);
        t0 = System.nanoTime();
        int firstF2Count = fresh.searchByDefinition("love").size();
        double firstF2 = ms(t0);
        System.out.printf("Cold first query  F1 '%s' : %8.2f ms%n", firstKey, firstF1);
        System.out.printf("Cold first query  F2 'love' : %8.2f ms  (%d results)%n", firstF2, firstF2Count);
        track(firstF1, "cold F1");
        track(firstF2, "cold F2");
        System.out.println();

        // 4. tốc độ ổn định
        double sum = 0, max = 0;
        for (int i = 0; i < 500; i++) {
            String q = words.get(rnd.nextInt(words.size())).getSlang();
            long s = System.nanoTime();
            fresh.searchBySlang(q);
            double d = ms(s);
            sum += d;
            max = Math.max(max, d);
        }
        System.out.printf("F1 x500 random slang        : avg %.3f ms, max %.3f ms%n", sum / 500, max);
        track(max, "F1 max");

        System.out.printf("%-14s %9s %9s %9s%n", "F2 keyword", "results", "avg ms", "max ms");
        for (String kw : DEFINITION_KEYWORDS) {
            double total = 0, worst = 0;
            int results = 0;
            for (int i = 0; i < 10; i++) {
                long s = System.nanoTime();
                results = fresh.searchByDefinition(kw).size();
                double d = ms(s);
                total += d;
                worst = Math.max(worst, d);
            }
            System.out.printf("%-14s %9d %9.2f %9.2f%n", "'" + kw.strip() + "'", results, total / 10, worst);
            track(worst, "F2 '" + kw + "'");
        }

        System.out.println();
        System.out.printf("Worst single operation: %.2f ms (limit %.0f ms)%n", worstMs, LIMIT_MS);
        System.out.println(failed ? "RESULT: FAIL" : "RESULT: PASS");
        System.exit(failed ? 1 : 0);
    }

    // ------------------------------------------------------------ brute-force reference

    private static List<SlangWord> bruteSlang(List<SlangWord> words, String query) {
        String key = TextNormalizer.normalizeSlang(query);
        List<SlangWord> r = new ArrayList<>();
        for (SlangWord w : words) if (TextNormalizer.normalizeSlang(w.getSlang()).equals(key)) r.add(w);
        return r;
    }

    private static List<SlangWord> bruteDefinition(List<SlangWord> words, String keyword) {
        String kw = TextNormalizer.normalizeText(keyword);
        List<SlangWord> r = new ArrayList<>();
        for (SlangWord w : words) {
            for (String m : w.getMeanings()) {
                if (m.toLowerCase(Locale.ROOT).contains(kw)) { r.add(w); break; }
            }
        }
        return r;
    }

    // ------------------------------------------------------------ helpers

    private static double ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static void track(double millis, String what) {
        worstMs = Math.max(worstMs, millis);
        if (millis >= LIMIT_MS) {
            failed = true;
            System.out.printf("  !! too slow: %s = %.2f ms%n", what, millis);
        }
    }

    private static void check(String message, boolean ok) {
        if (!ok) {
            failed = true;
            mismatch = true;
            System.out.println("  !! " + message);
        }
    }
}
