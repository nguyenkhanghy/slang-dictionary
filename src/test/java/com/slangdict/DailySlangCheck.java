package com.slangdict;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.QuizService;

/**
 * Kiểm tra F8 "từ lóng của ngày" ({@link QuizService#slangOfTheDay}): cùng ngày cùng từ (kể cả khi {@link Random} khác nhau),
 * từ trả về thuộc dữ liệu, các ngày khác nhau cho nhiều từ khác nhau, dữ liệu rỗng → rỗng.
 * Chạy tay: {@code java -cp target/classes:target/test-classes com.slangdict.DailySlangCheck [fileDữLiệu]}
 * (mặc định {@code data/slang.original.txt}); làm việc trên bản sao trong thư mục tạm, không đụng {@code data/}.
 */
public final class DailySlangCheck {

    private static int checks;
    private static int failures;

    private DailySlangCheck() {
    }

    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args.length > 0 ? args[0] : "data/slang.original.txt");
        Path dir = Files.createTempDirectory("dailyslang");
        try {
            runOnCopy(source, dir.resolve("real"));
            runOnEmpty(dir.resolve("empty"));
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

    private static void runOnCopy(Path source, Path dir) throws Exception {
        Files.createDirectories(dir);
        Path data = dir.resolve("slang.current.txt");
        Files.copy(source, data);
        DictionaryService svc = DictionaryService.open(data, dir.resolve("slang.index.bin"), dir.resolve("history.txt"));
        System.out.println("== Real data: " + svc.size() + " entries");
        QuizService a = new QuizService(svc, new Random(1));
        QuizService b = new QuizService(svc, new Random(999));
        LocalDate day = LocalDate.of(2026, 11, 10);

        Optional<SlangWord> first = a.slangOfTheDay(day);
        check("has a word", first.isPresent());
        check("repeat call same word", first.equals(a.slangOfTheDay(day)));
        check("other QuizService/Random same word", first.equals(b.slangOfTheDay(day)));
        List<SlangWord> all = svc.getAllWords();
        check("word belongs to dictionary", first.isPresent() && all.contains(first.get()));

        Set<String> distinct = new HashSet<>();
        boolean allMembers = true;
        for (int i = 0; i < 365; i++) {
            Optional<SlangWord> w = a.slangOfTheDay(day.plusDays(i));
            allMembers &= w.isPresent() && all.contains(w.get());
            w.ifPresent(x -> distinct.add(x.getSlang() + "|" + x.getMeanings()));
        }
        check("365 days all valid", allMembers);
        check("365 days give many different words (" + distinct.size() + ")", distinct.size() >= Math.min(300, svc.size() / 2));
        svc.close();
    }

    private static void runOnEmpty(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path data = dir.resolve("slang.current.txt");
        Files.writeString(data, "");
        DictionaryService svc = DictionaryService.open(data, dir.resolve("slang.index.bin"), dir.resolve("history.txt"));
        System.out.println("== Empty data: " + svc.size() + " entries");
        check("empty dictionary -> no word", new QuizService(svc).slangOfTheDay(LocalDate.now()).isEmpty());
        svc.close();
    }

    private static void check(String name, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + name);
        } else {
            System.out.println("  ok:   " + name);
        }
    }
}