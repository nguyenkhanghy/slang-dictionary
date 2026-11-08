package com.slangdict;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.DuplicatePolicy;
import com.slangdict.service.QuizQuestion;
import com.slangdict.service.QuizService;
import com.slangdict.util.TextNormalizer;

/**
 * Kiểm tra Phase 4 (F8 random, F9/F10 quiz) mà không cần JUnit.
 *
 * <pre>
 *   mvn clean package
 *   java -cp target/classes:target/test-classes com.slangdict.QuizCheck [fileDuLieuSach] [file100k]
 *   (mặc định: data/slang.original.txt và data/slang.100k.txt; thiếu file 100k thì bỏ qua phần đo tốc độ lớn;
 *    Windows dùng ; thay cho : trong classpath)
 * </pre>
 *
 * Việc kiểm tra "chỉ có đúng một đáp án đúng" được tính ĐỘC LẬP với logic của QuizService (từ bảng tra
 * slang -> các entry dựng lại từ dữ liệu). Chạy trên bản sao trong target/, không đụng data/ thật. Thoát mã 1 nếu sai.
 */
public class QuizCheck {

    private static final int REAL_QUESTIONS = 2500;
    /** Giới hạn cho 99% câu hỏi; max cho phép rộng hơn vì tạm dừng GC của JVM không thuộc thuật toán. */
    private static final double SPEED_LIMIT_MS = 50.0;
    private static final double SPEED_MAX_MS = 250.0;

    private static int checks = 0;
    private static int failures = 0;
    private static Path workDir;

    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args.length > 0 ? args[0] : "data/slang.original.txt");
        Path big = Paths.get(args.length > 1 ? args[1] : "data/slang.100k.txt");
        if (!Files.isRegularFile(source)) {
            System.err.println("Not found: " + source);
            System.exit(2);
        }
        workDir = Paths.get("target", "quiz-data").toAbsolutePath();
        resetDir(workDir);

        try {
            realDataTests(source);
            smallDataTests();
            dynamicDataTests();
            degenerateLargeTest();
            if (Files.isRegularFile(big)) {
                largeDataTests(big);
            } else {
                System.out.println("\n== Large dataset: SKIPPED (not found: " + big + "; run java tools/GenerateBigData.java)");
            }
        } catch (Throwable t) {
            failures++;
            System.out.println("  FAIL: unexpected exception " + t);
            t.printStackTrace(System.out);
        }

        System.out.println();
        System.out.println(checks + " checks, " + failures + " failed");
        System.out.println(failures == 0 ? "RESULT: PASS" : "RESULT: FAIL");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ real data

    private static void realDataTests(Path source) throws IOException {
        section("Real data: " + source);
        DictionaryService svc = open("real", Files.readAllLines(source, StandardCharsets.UTF_8));
        System.out.println("  " + svc.size() + " entries");
        Reference ref = new Reference(svc.getAllWords());

        QuizService quiz = new QuizService(svc, new Random(11));
        Set<SlangWord> seen = new HashSet<>();
        boolean allPresent = true;
        for (int i = 0; i < 3000; i++) {
            Optional<SlangWord> w = quiz.randomSlang();
            allPresent &= w.isPresent() && ref.contains(w.get());
            w.ifPresent(seen::add);
        }
        check("F8: always returns an existing entry", allPresent);
        check("F8: really random (>= 1000 distinct entries in 3000 calls, got " + seen.size() + ")", seen.size() >= 1000);

        for (boolean slangToMeaning : new boolean[]{true, false}) {
            String label = slangToMeaning ? "F9 " : "F10";
            Supplier<Optional<QuizQuestion>> next = slangToMeaning ? quiz::nextSlangToMeaningQuestion : quiz::nextMeaningToSlangQuestion;
            int empty = 0, bad = 0;
            String firstBad = null;
            int[] positions = new int[4];
            for (int i = 0; i < REAL_QUESTIONS; i++) {
                Optional<QuizQuestion> q = next.get();
                if (q.isEmpty()) { empty++; continue; }
                String problem = slangToMeaning ? ref.problemF9(q.get()) : ref.problemF10(q.get());
                if (problem != null) { bad++; if (firstBad == null) firstBad = problem; }
                positions[q.get().correctIndex()]++;
            }
            check(label + ": never empty on real data (empty=" + empty + ")", empty == 0);
            check(label + ": all " + REAL_QUESTIONS + " questions valid (bad=" + bad + (firstBad == null ? "" : ", first: " + firstBad) + ")", bad == 0);
            boolean balanced = true;
            for (int p : positions) balanced &= p >= REAL_QUESTIONS * 0.15;
            check(label + ": correct answer position balanced " + java.util.Arrays.toString(positions), balanced);
        }

        // cùng seed -> cùng chuỗi câu hỏi
        QuizService a = new QuizService(svc, new Random(99));
        QuizService b = new QuizService(svc, new Random(99));
        boolean same = true;
        for (int i = 0; i < 200; i++) {
            same &= a.nextSlangToMeaningQuestion().equals(b.nextSlangToMeaningQuestion());
            same &= a.nextMeaningToSlangQuestion().equals(b.nextMeaningToSlangQuestion());
            same &= a.randomSlang().equals(b.randomSlang());
        }
        check("same seed -> same sequence of questions", same);
    }

    // ------------------------------------------------------------------ tiny / degenerate data

    private static void smallDataTests() throws IOException {
        section("Small and degenerate datasets");

        DictionaryService none = open("n0", List.of());
        QuizService q0 = new QuizService(none, new Random(1));
        check("0 entries: F8 empty", q0.randomSlang().isEmpty());
        check("0 entries: F9 and F10 empty", q0.nextSlangToMeaningQuestion().isEmpty() && q0.nextMeaningToSlangQuestion().isEmpty());

        DictionaryService three = open("n3", List.of("AAA`apple fruit", "BBB`banana fruit", "CCC`cherry fruit"));
        QuizService q3 = new QuizService(three, new Random(1));
        check("3 entries: F8 works", q3.randomSlang().isPresent());
        check("3 entries (< 4): F9 and F10 empty", q3.nextSlangToMeaningQuestion().isEmpty() && q3.nextMeaningToSlangQuestion().isEmpty());

        DictionaryService four = open("n4", List.of("AAA`apple", "BBB`banana", "CCC`cherry", "DDD`date"));
        QuizService q4 = new QuizService(four, new Random(2));
        Reference ref4 = new Reference(four.getAllWords());
        boolean ok = true;
        Set<String> answers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Optional<QuizQuestion> f9 = q4.nextSlangToMeaningQuestion();
            Optional<QuizQuestion> f10 = q4.nextMeaningToSlangQuestion();
            ok &= f9.isPresent() && f10.isPresent();
            if (f9.isPresent()) {
                ok &= ref4.problemF9(f9.get()) == null && new HashSet<>(f9.get().options()).equals(Set.of("apple", "banana", "cherry", "date"));
                answers.add(f9.get().answer().getSlang());
            }
            if (f10.isPresent()) ok &= ref4.problemF10(f10.get()) == null && new HashSet<>(f10.get().options()).equals(Set.of("AAA", "BBB", "CCC", "DDD"));
        }
        check("4 distinct entries: valid questions using exactly those 4 entries", ok);
        check("4 distinct entries: every entry becomes the answer sometimes", answers.size() == 4);

        // 4 slang cùng nghĩa + 1 slang khác: chỉ có 2 nghĩa phân biệt -> F9 không thể; F10 chỉ làm được khi đáp án là slang khác
        DictionaryService sameMeaning = open("same-meaning", List.of("A1`laughing", "A2`Laughing", "A3`laughing", "A4`LAUGHING", "E`crying"));
        QuizService qs = new QuizService(sameMeaning, new Random(3));
        check("4 slangs with the same meaning + 1 other: F9 empty (only 2 distinct meanings)", qs.nextSlangToMeaningQuestion().isEmpty());
        boolean f10ok = true;
        for (int i = 0; i < 100; i++) {
            Optional<QuizQuestion> q = qs.nextMeaningToSlangQuestion();
            f10ok &= q.isPresent() && q.get().answer().getSlang().equals("E")
                    && new Reference(sameMeaning.getAllWords()).problemF10(q.get()) == null;
        }
        check("same-meaning data: F10 found (answer always the odd one out, no ambiguous options)", f10ok);

        // "kim đáy bể": 150 slang cùng nghĩa + 1 slang khác. Chỉ đáp án là slang khác mới tạo được câu F10; thử ngẫu nhiên
        // gần như luôn trượt, nên kết quả đúng đòi hỏi bước duyệt vét cạn cho dữ liệu nhỏ.
        List<String> haystack = new ArrayList<>();
        for (int i = 0; i < 150; i++) haystack.add("H" + i + "`common meaning");
        haystack.add("NEEDLE`unique meaning");
        DictionaryService needle = open("needle", haystack);
        QuizService qn = new QuizService(needle, new Random(9));
        boolean needleOk = true;
        for (int i = 0; i < 100; i++) {
            Optional<QuizQuestion> q = qn.nextMeaningToSlangQuestion();
            needleOk &= q.isPresent() && q.get().answer().getSlang().equals("NEEDLE");
        }
        check("needle in haystack: F10 always found, answer is the odd one out (exhaustive fallback)", needleOk);
        check("needle in haystack: F9 empty (only 2 distinct meanings)", qn.nextSlangToMeaningQuestion().isEmpty());

        // mọi entry cùng một slang -> không có đáp án nhiễu hợp lệ
        DictionaryService sameKey = open("same-key", List.of("X`one", "x`two", "X`three", "X`four", "X`five"));
        QuizService qk = new QuizService(sameKey, new Random(4));
        check("all entries share one slang: F9 and F10 empty", qk.nextSlangToMeaningQuestion().isEmpty() && qk.nextMeaningToSlangQuestion().isEmpty());

        // 3 slang cùng nghĩa + 3 slang khác nhau: cả hai kiểu đều làm được với mọi đáp án
        DictionaryService mixed = open("mixed", List.of("T1`lol", "T2`lol", "T3`LOL", "X`ex", "Y`why", "Z`zed"));
        QuizService qm = new QuizService(mixed, new Random(5));
        Reference refM = new Reference(mixed.getAllWords());
        boolean mixedOk = true;
        for (int i = 0; i < 300; i++) {
            Optional<QuizQuestion> f9 = qm.nextSlangToMeaningQuestion();
            Optional<QuizQuestion> f10 = qm.nextMeaningToSlangQuestion();
            mixedOk &= f9.isPresent() && refM.problemF9(f9.get()) == null && f10.isPresent() && refM.problemF10(f10.get()) == null;
        }
        check("trio with same meaning + 3 others: 300 valid questions of each kind", mixedOk);

        // một slang có nhiều entry: nghĩa của entry khác cùng slang không được làm đáp án nhiễu của F9
        DictionaryService multi = open("multi-entry", List.of("GOAT`greatest of all time", "GOAT`farm animal", "AAA`animal", "BBB`farm animal",
                "CCC`cool", "DDD`dull", "EEE`eager"));
        QuizService qg = new QuizService(multi, new Random(6));
        Reference refG = new Reference(multi.getAllWords());
        boolean multiOk = true;
        int goatQuestions = 0;
        for (int i = 0; i < 600; i++) {
            Optional<QuizQuestion> f9 = qg.nextSlangToMeaningQuestion();
            multiOk &= f9.isPresent() && refG.problemF9(f9.get()) == null;
            if (f9.isPresent() && f9.get().answer().getSlang().equals("GOAT")) goatQuestions++;
            Optional<QuizQuestion> f10 = qg.nextMeaningToSlangQuestion();
            multiOk &= f10.isPresent() && refG.problemF10(f10.get()) == null;
        }
        check("multi-entry slang (GOAT) never gives an ambiguous question (" + goatQuestions + " GOAT questions)", multiOk && goatQuestions > 0);
    }

    // ------------------------------------------------------------------ data changes between questions

    private static void dynamicDataTests() throws IOException {
        section("Data changes between questions (no stale cache)");
        DictionaryService svc = open("dynamic", List.of("AAA`apple", "BBB`banana", "CCC`cherry", "DDD`date"));
        QuizService quiz = new QuizService(svc, new Random(7));
        check("4 entries: question available", quiz.nextSlangToMeaningQuestion().isPresent());
        check("delete one -> 3 entries -> F9/F10 empty", svc.deleteSlang(svc.lookupSlang("DDD").get(0))
                && quiz.nextSlangToMeaningQuestion().isEmpty() && quiz.nextMeaningToSlangQuestion().isEmpty());
        svc.addSlang("EEE", List.of("elderberry"), DuplicatePolicy.REJECT);
        boolean usesNew = false;
        for (int i = 0; i < 100 && !usesNew; i++) {
            Optional<QuizQuestion> q = quiz.nextSlangToMeaningQuestion();
            usesNew = q.isPresent() && (q.get().options().contains("elderberry")) && !q.get().options().contains("date");
        }
        check("add one -> question again, uses the new entry and not the deleted one", usesNew);
    }

    // ------------------------------------------------------------------ degenerate data at scale must terminate quickly

    private static void degenerateLargeTest() throws IOException {
        section("Degenerate 100,000 entries (all share ONE meaning): must give up fast");
        List<String> lines = new ArrayList<>(100_000);
        for (int i = 0; i < 100_000; i++) lines.add("K" + i + "`same meaning");
        DictionaryService svc = open("degenerate", lines);
        QuizService quiz = new QuizService(svc, new Random(8));
        long t = System.nanoTime();
        boolean f9 = quiz.nextSlangToMeaningQuestion().isEmpty();
        double f9ms = (System.nanoTime() - t) / 1e6;
        t = System.nanoTime();
        boolean f10 = quiz.nextMeaningToSlangQuestion().isEmpty();
        double f10ms = (System.nanoTime() - t) / 1e6;
        System.out.printf("  F9 %.1f ms, F10 %.1f ms%n", f9ms, f10ms);
        check("impossible quiz at 100k returns empty for F9 and F10", f9 && f10);
        check("...and does so in < 1000 ms", f9ms < 1000 && f10ms < 1000);
    }

    // ------------------------------------------------------------------ large dataset speed + validity

    private static void largeDataTests(Path big) throws IOException {
        section("Large dataset: " + big);
        DictionaryService svc = open("large", Files.readAllLines(big, StandardCharsets.UTF_8));
        Reference ref = new Reference(svc.getAllWords());
        QuizService quiz = new QuizService(svc, new Random(21));
        System.out.println("  " + svc.size() + " entries");
        System.gc(); // loại tạm dừng GC do các bài test trước (đặc biệt bộ dữ liệu suy biến) ra khỏi phép đo
        for (boolean slangToMeaning : new boolean[]{true, false}) {
            String label = slangToMeaning ? "F9 " : "F10";
            Supplier<Optional<QuizQuestion>> next = slangToMeaning ? quiz::nextSlangToMeaningQuestion : quiz::nextMeaningToSlangQuestion;
            double[] times = new double[1000];
            int bad = 0, empty = 0;
            for (int i = 0; i < times.length; i++) {
                long t = System.nanoTime();
                Optional<QuizQuestion> q = next.get();
                times[i] = (System.nanoTime() - t) / 1e6;
                if (q.isEmpty()) { empty++; continue; }
                if (i < 300 && (slangToMeaning ? ref.problemF9(q.get()) : ref.problemF10(q.get())) != null) bad++;
            }
            double total = 0;
            for (double t : times) total += t;
            double[] sorted = times.clone();
            java.util.Arrays.sort(sorted);
            double p99 = sorted[(int) (sorted.length * 0.99) - 1], max = sorted[sorted.length - 1];
            System.out.printf("  %s x1000: avg %.3f ms, p99 %.3f ms, max %.3f ms%n", label, total / times.length, p99, max);
            check(label + ": no empty/invalid question at scale (empty=" + empty + ", bad=" + bad + ")", empty == 0 && bad == 0);
            check(label + ": p99 < " + SPEED_LIMIT_MS + " ms (got " + String.format("%.2f", p99) + ")", p99 < SPEED_LIMIT_MS);
            check(label + ": max < " + SPEED_MAX_MS + " ms (got " + String.format("%.2f", max) + ")", max < SPEED_MAX_MS);
        }
    }

    // ------------------------------------------------------------------ independent reference

    /** Kiểm tra câu hỏi độc lập với QuizService: dựng bảng tra slang -> entries trực tiếp từ dữ liệu. */
    private static final class Reference {
        private final Map<String, List<SlangWord>> byKey = new HashMap<>();
        private final Set<String> meaningTexts = new HashSet<>();
        private final Set<SlangWord> all = new HashSet<>();

        Reference(List<SlangWord> words) {
            for (SlangWord w : words) {
                byKey.computeIfAbsent(TextNormalizer.normalizeSlang(w.getSlang()), k -> new ArrayList<>()).add(w);
                meaningTexts.add(String.join(" | ", w.getMeanings()));
                all.add(w);
            }
        }

        boolean contains(SlangWord w) {
            return all.contains(w);
        }

        Set<String> meaningsOf(Iterable<SlangWord> entries) {
            Set<String> set = new HashSet<>();
            for (SlangWord e : entries) for (String m : e.getMeanings()) set.add(TextNormalizer.normalizeText(m));
            return set;
        }

        /** @return null nếu hợp lệ, ngược lại mô tả vấn đề đầu tiên. */
        String problemF9(QuizQuestion q) {
            String structure = structureProblem(q);
            if (structure != null) return structure;
            SlangWord answer = q.answer();
            if (!q.prompt().equals(answer.getSlang())) return "prompt is not the slang";
            if (!q.correctOption().equals(String.join(" | ", answer.getMeanings()))) return "correct option is not the answer's meanings";
            Set<String> seen = new HashSet<>();
            for (String o : q.options()) if (!seen.add(TextNormalizer.normalizeText(o))) return "duplicate option text: " + o;
            Set<String> forbidden = meaningsOf(byKey.get(TextNormalizer.normalizeSlang(answer.getSlang())));
            for (int i = 0; i < q.options().size(); i++) {
                if (i == q.correctIndex()) continue;
                String option = q.options().get(i);
                if (!meaningTexts.contains(option)) return "distractor is not the meaning text of any entry: " + option;
                for (String part : option.split(" \\| ")) {
                    if (forbidden.contains(TextNormalizer.normalizeText(part))) return "ambiguous: distractor '" + option + "' shares a meaning with slang " + answer.getSlang();
                }
            }
            return null;
        }

        String problemF10(QuizQuestion q) {
            String structure = structureProblem(q);
            if (structure != null) return structure;
            SlangWord answer = q.answer();
            if (!q.prompt().equals(String.join(" | ", answer.getMeanings()))) return "prompt is not the answer's meanings";
            if (!q.correctOption().equals(answer.getSlang())) return "correct option is not the answer's slang";
            Set<String> seen = new HashSet<>();
            for (String o : q.options()) if (!seen.add(TextNormalizer.normalizeSlang(o))) return "duplicate option slang: " + o;
            Set<String> answerMeanings = meaningsOf(List.of(answer));
            for (int i = 0; i < q.options().size(); i++) {
                if (i == q.correctIndex()) continue;
                String slang = q.options().get(i);
                List<SlangWord> entries = byKey.get(TextNormalizer.normalizeSlang(slang));
                if (entries == null) return "distractor slang does not exist: " + slang;
                for (String m : meaningsOf(entries)) {
                    if (answerMeanings.contains(m)) return "ambiguous: distractor slang '" + slang + "' also means '" + m + "'";
                }
            }
            return null;
        }

        private String structureProblem(QuizQuestion q) {
            if (q.options().size() != 4) return "not 4 options";
            if (q.correctIndex() < 0 || q.correctIndex() > 3) return "bad correctIndex";
            if (!contains(q.answer())) return "answer is not an entry of the dictionary";
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static DictionaryService open(String name, List<String> lines) throws IOException {
        Path dir = workDir.resolve(name);
        Files.createDirectories(dir);
        Files.write(dir.resolve("slang.current.txt"), lines, StandardCharsets.UTF_8);
        return DictionaryService.open(dir.resolve("slang.current.txt"), dir.resolve("slang.index.bin"), dir.resolve("history.txt"));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static void check(String name, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + name);
        }
    }

    private static void resetDir(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) walk.sorted(java.util.Comparator.reverseOrder())::iterator) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
    }
}