package com.slangdict;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import com.slangdict.index.SlangIndex;
import com.slangdict.model.SlangWord;
import com.slangdict.repository.SlangFileReader;
import com.slangdict.service.AddResult;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.DuplicatePolicy;
import com.slangdict.service.EditResult;
import com.slangdict.service.InputValidator;
import com.slangdict.service.ValidationException;
import com.slangdict.service.ValidationException.Reason;
import com.slangdict.util.TextNormalizer;

/**
 * Kiểm tra Phase 3 (add / edit / delete / reset / validate / lưu trữ) mà không cần JUnit.
 *
 * <pre>
 *   mvn clean package
 *   java -cp target/classes:target/test-classes com.slangdict.MutationCheck [fileDuLieuSach]
 *   (mặc định: data/slang.original.txt; Windows dùng ; thay cho : trong classpath)
 * </pre>
 *
 * Chạy trên BẢN SAO trong target/mutation-data, không đụng tới thư mục data/ thật.
 * Cốt lõi: sau hàng trăm thao tác ngẫu nhiên, danh sách trong service, nội dung file txt và kết quả tìm kiếm của
 * index cập nhật tăng dần phải TRÙNG với một mô hình tham chiếu đơn giản và với index build mới từ đầu.
 * Thoát mã 1 nếu có kiểm tra thất bại.
 */
public class MutationCheck {

    private static int checks = 0;
    private static int failures = 0;

    private static Path dataFile;
    private static Path indexFile;
    private static Path historyFile;
    private static Path originalFile;

    public static void main(String[] args) throws Exception {
        Path source = Paths.get(args.length > 0 ? args[0] : "data/slang.original.txt");
        if (!Files.isRegularFile(source)) {
            System.err.println("Not found: " + source);
            System.exit(2);
        }
        Path dir = Paths.get("target", "mutation-data").toAbsolutePath();
        resetDir(dir);
        originalFile = dir.resolve("slang.original.txt");
        dataFile = dir.resolve("slang.current.txt");
        indexFile = dir.resolve("slang.index.bin");
        historyFile = dir.resolve("history.txt");
        Files.copy(source, originalFile);
        Files.copy(source, dataFile);
        byte[] originalBytes = Files.readAllBytes(originalFile);

        try {
            DictionaryService svc = DictionaryService.open(dataFile, indexFile, historyFile);
            List<SlangWord> model = new ArrayList<>(svc.getAllWords());
            System.out.println("Dataset: " + model.size() + " entries from " + source);

            validationTests(svc);
            semanticsTests(svc, model);
            randomDifferentialTest(svc, model);
            svc = persistenceTests(svc, model);
            svc = resetTests(svc);
            failureAtomicityTests(svc, dir);

            check("slang.original.txt never modified", Arrays.equals(originalBytes, Files.readAllBytes(originalFile)));

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

    // ------------------------------------------------------------------ E5 validation

    private static void validationTests(DictionaryService svc) throws IOException {
        section("Validation (E5)");
        expect("empty slang", Reason.SLANG_EMPTY, () -> InputValidator.toSlangWord("   ", List.of("x")));
        expect("null slang", Reason.SLANG_EMPTY, () -> InputValidator.toSlangWord(null, List.of("x")));
        expect("backtick in slang", Reason.SLANG_INVALID_CHAR, () -> InputValidator.toSlangWord("a`b", List.of("x")));
        expect("newline in slang", Reason.SLANG_INVALID_CHAR, () -> InputValidator.toSlangWord("a\nb", List.of("x")));
        expect("null meanings", Reason.NO_MEANING, () -> InputValidator.toSlangWord("a", null));
        expect("empty meanings", Reason.NO_MEANING, () -> InputValidator.toSlangWord("a", List.of()));
        expect("blank meaning", Reason.MEANING_EMPTY, () -> InputValidator.toSlangWord("a", List.of("  ")));
        expect("pipe in meaning", Reason.MEANING_INVALID_CHAR, () -> InputValidator.toSlangWord("a", List.of("x|y")));
        expect("backtick in meaning", Reason.MEANING_INVALID_CHAR, () -> InputValidator.toSlangWord("a", List.of("x`y")));
        expect("newline in meaning", Reason.MEANING_INVALID_CHAR, () -> InputValidator.toSlangWord("a", List.of("x\ny")));
        expect("parseMeanings with no meaning", Reason.NO_MEANING, () -> InputValidator.parseMeanings(" | | "));
        check("slang may contain '|' (like 8-|)", InputValidator.toSlangWord("A|B", List.of("x")).getSlang().equals("A|B"));
        check("parseMeanings splits/strips/drops empty",
                InputValidator.parseMeanings("a| b |  | c").equals(List.of("a", "b", "c")));

        int sizeBefore = svc.size();
        int historyBefore = svc.getHistory().size();
        byte[] fileBefore = Files.readAllBytes(dataFile);
        try {
            svc.addSlang("bad`slang", List.of("x"), DuplicatePolicy.REJECT);
            check("service rejects invalid slang", false);
        } catch (ValidationException e) {
            check("service rejects invalid slang", e.getReason() == Reason.SLANG_INVALID_CHAR);
        }
        check("invalid input changed nothing (size, file, history)",
                svc.size() == sizeBefore && svc.getHistory().size() == historyBefore
                        && Arrays.equals(fileBefore, Files.readAllBytes(dataFile)));
    }

    // ------------------------------------------------------------------ E1-E3 deterministic semantics

    private static void semanticsTests(DictionaryService svc, List<SlangWord> model) throws IOException {
        section("Add / Edit / Delete semantics (E1-E3)");
        int historyBefore = svc.getHistory().size();
        int size0 = svc.size();

        check("not exists before", !svc.exists("zz-new"));
        check("add new -> ADDED", svc.addSlang("  zz-new ", List.of(" first meaning qqtokenone "), DuplicatePolicy.REJECT) == AddResult.ADDED);
        model.add(new SlangWord("zz-new", List.of("first meaning qqtokenone")));
        check("exists ignores case/spaces", svc.exists(" ZZ-NEW "));
        check("lookup finds trimmed slang", svc.lookupSlang("ZZ-new").size() == 1 && svc.lookupSlang("zz-new").get(0).getSlang().equals("zz-new"));
        check("F2 finds token of new entry", svc.searchByDefinition("qqtokenone").size() == 1);
        check("exists/lookup did not write history (only the F2 search did)", svc.getHistory().size() == historyBefore + 1);

        byte[] fileBefore = Files.readAllBytes(dataFile);
        check("add existing with REJECT -> CONFLICT", svc.addSlang("ZZ-NEW", List.of("other"), DuplicatePolicy.REJECT) == AddResult.CONFLICT);
        check("CONFLICT changed nothing", Arrays.equals(fileBefore, Files.readAllBytes(dataFile)) && svc.size() == size0 + 1);

        check("add with DUPLICATE -> ADDED", svc.addSlang("zz-new", List.of("second meaning qqtokentwo"), DuplicatePolicy.DUPLICATE) == AddResult.ADDED);
        model.add(new SlangWord("zz-new", List.of("second meaning qqtokentwo")));
        check("F1 returns both duplicates in order", svc.lookupSlang("zz-new").equals(List.of(model.get(model.size() - 2), model.get(model.size() - 1))));

        int firstPos = svc.getAllWords().indexOf(model.get(model.size() - 2));
        check("OVERWRITE -> OVERWRITTEN", svc.addSlang("ZZ-NEW", List.of("final meaning qqtokenthree"), DuplicatePolicy.OVERWRITE) == AddResult.OVERWRITTEN);
        model.set(firstPos, new SlangWord("ZZ-NEW", List.of("final meaning qqtokenthree")));
        model.remove(model.size() - 1);
        check("OVERWRITE replaced ALL entries of the key by one", svc.lookupSlang("zz-new").size() == 1);
        check("OVERWRITE kept position of the first entry", svc.getAllWords().get(firstPos).getSlang().equals("ZZ-NEW"));
        check("OVERWRITE left no stale tokens", svc.searchByDefinition("qqtokenone").isEmpty() && svc.searchByDefinition("qqtokentwo").isEmpty()
                && svc.searchByDefinition("qqtokenthree").size() == 1);
        check("list equals model after overwrite", svc.getAllWords().equals(model));

        SlangWord target = svc.lookupSlang("zz-new").get(0);
        check("edit meanings -> EDITED", svc.editSlang(target, "ZZ-NEW", List.of("edited qqtokenfour")) == EditResult.EDITED);
        model.set(firstPos, new SlangWord("ZZ-NEW", List.of("edited qqtokenfour")));
        check("edit updates F2 index", svc.searchByDefinition("qqtokenthree").isEmpty() && svc.searchByDefinition("qqtokenfour").size() == 1);

        SlangWord edited = svc.lookupSlang("zz-new").get(0);
        fileBefore = Files.readAllBytes(dataFile);
        check("edit to identical content -> UNCHANGED", svc.editSlang(edited, edited.getSlang(), edited.getMeanings()) == EditResult.UNCHANGED);
        check("UNCHANGED wrote nothing", Arrays.equals(fileBefore, Files.readAllBytes(dataFile)));

        check("edit key to a fresh key -> EDITED", svc.editSlang(edited, "zz-renamed", edited.getMeanings()) == EditResult.EDITED);
        model.set(firstPos, new SlangWord("zz-renamed", List.of("edited qqtokenfour")));
        check("old key gone, new key present", !svc.exists("zz-new") && svc.exists("zz-renamed"));

        String otherKey = model.get(0).getSlang();
        SlangWord renamed = svc.lookupSlang("zz-renamed").get(0);
        fileBefore = Files.readAllBytes(dataFile);
        check("edit key onto another existing key -> CONFLICT", svc.editSlang(renamed, otherKey, renamed.getMeanings()) == EditResult.CONFLICT);
        check("CONFLICT (edit) changed nothing", Arrays.equals(fileBefore, Files.readAllBytes(dataFile)));
        check("edit with an equal-but-different instance works",
                svc.editSlang(new SlangWord("zz-renamed", List.of("edited qqtokenfour")), "zz-renamed", List.of("via copy qqtokenfive")) == EditResult.EDITED);
        model.set(firstPos, new SlangWord("zz-renamed", List.of("via copy qqtokenfive")));

        SlangWord toDelete = svc.lookupSlang("zz-renamed").get(0);
        check("delete -> true", svc.deleteSlang(toDelete));
        model.remove(firstPos);
        check("delete again -> false", !svc.deleteSlang(toDelete));
        check("edit of a deleted entry -> NOT_FOUND", svc.editSlang(toDelete, "x", List.of("y")) == EditResult.NOT_FOUND);
        check("deleted entry gone from F1 and F2", !svc.exists("zz-renamed") && svc.searchByDefinition("qqtokenfive").isEmpty());
        check("size back to original", svc.size() == size0);
        check("list equals model after deletes", svc.getAllWords().equals(model));
        check("file equals model", SlangFileReader.read(dataFile).equals(model));

        // slang chứa '|' (như 8-|) phải thêm/tìm/xóa được
        check("add slang containing '|'", svc.addSlang("zz|pipe", List.of("pipe meaning"), DuplicatePolicy.REJECT) == AddResult.ADDED);
        model.add(new SlangWord("zz|pipe", List.of("pipe meaning")));
        check("file with '|'-slang reloads identically", SlangFileReader.read(dataFile).equals(model));
        check("delete '|'-slang", svc.deleteSlang(svc.lookupSlang("zz|pipe").get(0)));
        model.remove(model.size() - 1);
    }

    // ------------------------------------------------------------------ randomized differential test

    private static void randomDifferentialTest(DictionaryService svc, List<SlangWord> model) throws IOException {
        section("Randomized differential test (index updated incrementally vs reference model)");
        Random rnd = new Random(2026);
        List<String> vocabulary = new ArrayList<>();
        for (SlangWord w : model) {
            for (String m : w.getMeanings()) vocabulary.addAll(TextNormalizer.tokenize(TextNormalizer.normalizeText(m)));
        }
        List<String> removedTokens = new ArrayList<>();
        int[] counts = new int[7];
        int ops = 600;
        boolean listsOk = true, fileOk = true;

        for (int i = 0; i < ops; i++) {
            int kind = rnd.nextInt(10);
            if (model.size() < 50) kind = 0;
            try {
                if (kind <= 1) {                                       // add fresh key
                    SlangWord w = randomWord(rnd, vocabulary, "FRESH" + i);
                    check("op " + i + " add fresh", svc.addSlang(w.getSlang(), w.getMeanings(), DuplicatePolicy.REJECT) == AddResult.ADDED);
                    model.add(w);
                    counts[0]++;
                } else if (kind == 2) {                                // add existing key -> CONFLICT
                    String key = pickKey(rnd, model);
                    AddResult r = svc.addSlang(key, List.of("conflict probe"), DuplicatePolicy.REJECT);
                    check("op " + i + " reject conflict", r == AddResult.CONFLICT);
                    counts[1]++;
                } else if (kind == 3) {                                // add duplicate
                    String key = pickKey(rnd, model);
                    SlangWord w = randomWord(rnd, vocabulary, key);
                    check("op " + i + " duplicate", svc.addSlang(key, w.getMeanings(), DuplicatePolicy.DUPLICATE) == AddResult.ADDED);
                    model.add(new SlangWord(key, w.getMeanings()));
                    counts[2]++;
                } else if (kind == 4) {                                // overwrite
                    String key = pickKey(rnd, model);
                    SlangWord w = randomWord(rnd, vocabulary, key);
                    List<Integer> ids = idsOf(model, key);
                    for (int id : ids) collectTokens(model.get(id), removedTokens);
                    check("op " + i + " overwrite", svc.addSlang(key, w.getMeanings(), DuplicatePolicy.OVERWRITE) == AddResult.OVERWRITTEN);
                    model.set(ids.get(0), new SlangWord(key, w.getMeanings()));
                    for (int k = ids.size() - 1; k >= 1; k--) model.remove((int) ids.get(k));
                    counts[3]++;
                } else if (kind <= 6) {                                // edit
                    int t = rnd.nextInt(model.size());
                    boolean viaCopy = rnd.nextInt(4) == 0;
                    SlangWord target = viaCopy ? new SlangWord(model.get(t).getSlang(), model.get(t).getMeanings()) : svc.getAllWords().get(t);
                    int idx = viaCopy ? model.indexOf(target) : t;
                    String newKey;
                    switch (rnd.nextInt(3)) {
                        case 0 -> newKey = target.getSlang();
                        case 1 -> newKey = "EDITED" + i;
                        default -> newKey = pickKey(rnd, model);
                    }
                    List<String> newMeanings = rnd.nextInt(5) == 0 ? target.getMeanings() : randomWord(rnd, vocabulary, "x").getMeanings();
                    SlangWord newWord = new SlangWord(newKey, newMeanings);
                    EditResult expected;
                    if (model.get(idx).equals(newWord)) expected = EditResult.UNCHANGED;
                    else if (!TextNormalizer.normalizeSlang(target.getSlang()).equals(TextNormalizer.normalizeSlang(newKey))
                            && !idsOf(model, newKey).isEmpty()) expected = EditResult.CONFLICT;
                    else expected = EditResult.EDITED;
                    SlangWord before = model.get(idx);
                    EditResult actual = svc.editSlang(target, newKey, newMeanings);
                    check("op " + i + " edit result " + expected + " vs " + actual, expected == actual);
                    if (expected == EditResult.EDITED) {
                        collectTokens(before, removedTokens);
                        model.set(idx, newWord);
                    }
                    counts[4]++;
                } else if (kind <= 8) {                                // delete
                    int t = rnd.nextInt(model.size());
                    boolean viaCopy = rnd.nextInt(4) == 0;
                    SlangWord target = viaCopy ? new SlangWord(model.get(t).getSlang(), model.get(t).getMeanings()) : svc.getAllWords().get(t);
                    int idx = viaCopy ? model.indexOf(target) : t;
                    collectTokens(model.get(idx), removedTokens);
                    check("op " + i + " delete", svc.deleteSlang(target));
                    model.remove(idx);
                    counts[5]++;
                } else {                                               // delete something that no longer exists
                    SlangWord ghost = new SlangWord("GHOST" + i, List.of("never existed"));
                    check("op " + i + " delete missing -> false", !svc.deleteSlang(ghost));
                    counts[6]++;
                }
            } catch (IOException | RuntimeException e) {
                check("op " + i + " threw " + e, false);
                break;
            }

            if (!svc.getAllWords().equals(model)) { listsOk = false; check("list equals model after op " + i, false); break; }
            if (i % 10 == 0 && !SlangFileReader.read(dataFile).equals(model)) { fileOk = false; check("file equals model after op " + i, false); break; }
            if (i % 100 == 99) compareQueries(svc, model, vocabulary, removedTokens, rnd, "after op " + i);
        }
        check("in-memory list equals model after " + ops + " ops", listsOk);
        check("file equals model after " + ops + " ops", fileOk && SlangFileReader.read(dataFile).equals(model));
        compareQueries(svc, model, vocabulary, removedTokens, rnd, "at end");
        System.out.println("  ops: add=" + counts[0] + " conflict=" + counts[1] + " duplicate=" + counts[2] + " overwrite=" + counts[3]
                + " edit=" + counts[4] + " delete=" + counts[5] + " delete-missing=" + counts[6] + "; final size " + model.size());
    }

    /** So F1/F2 của service với index build mới từ đầu trên cùng dữ liệu (kể cả token của entry đã bị xóa/sửa). */
    private static void compareQueries(DictionaryService svc, List<SlangWord> model, List<String> vocabulary,
                                       List<String> removedTokens, Random rnd, String when) {
        SlangIndex fresh = SlangIndex.build(model);
        boolean ok = true;
        for (int i = 0; i < 80 && ok; i++) {
            String key = pickKey(rnd, model);
            ok = svc.lookupSlang(key).equals(fresh.findBySlang(key));
        }
        check("F1 incremental == rebuilt " + when, ok);
        ok = true;
        List<String> keywords = new ArrayList<>(List.of("a", "the", "i love", "lov", ":)", "zzzz"));
        for (int i = 0; i < 25; i++) keywords.add(vocabulary.get(rnd.nextInt(vocabulary.size())));
        for (int i = 0; i < 25 && !removedTokens.isEmpty(); i++) keywords.add(removedTokens.get(rnd.nextInt(removedTokens.size())));
        for (String k : keywords) ok &= svc.searchByDefinition(k).equals(fresh.findByDefinition(k));
        check("F2 incremental == rebuilt " + when + " (" + keywords.size() + " keywords)", ok);
    }

    // ------------------------------------------------------------------ persistence

    private static DictionaryService persistenceTests(DictionaryService svc, List<SlangWord> model) throws IOException {
        section("Persistence (txt immediately, index on close)");
        svc.close();
        DictionaryService reopened = DictionaryService.open(dataFile, indexFile, historyFile);
        check("reopen after close(): index loaded from cache", reopened.isIndexFromCache());
        check("reopen after close(): data equals model", reopened.getAllWords().equals(model));
        SlangIndex fresh = SlangIndex.build(model);
        boolean same = true;
        for (SlangWord w : model) { if (!reopened.lookupSlang(w.getSlang()).equals(fresh.findBySlang(w.getSlang()))) { same = false; break; } }
        check("cached index answers F1 like a rebuilt one", same);

        check("mutate then NOT close", reopened.addSlang("zz-no-close", List.of("kept without close"), DuplicatePolicy.REJECT) == AddResult.ADDED);
        model.add(new SlangWord("zz-no-close", List.of("kept without close")));
        DictionaryService crashed = DictionaryService.open(dataFile, indexFile, historyFile);
        check("reopen WITHOUT close(): stale index detected, rebuilt", !crashed.isIndexFromCache());
        check("reopen WITHOUT close(): data survived", crashed.getAllWords().equals(model) && crashed.exists("zz-no-close"));
        check("F2 sees the entry added before the 'crash'", crashed.searchByDefinition("kept without close").size() == 1);

        DictionaryService again = DictionaryService.open(dataFile, indexFile, historyFile);
        check("next open uses the rebuilt cache", again.isIndexFromCache());
        check("close() twice is safe", closeTwice(again));

        // Mỗi loại thao tác ghi phải đánh dấu index "dirty": close() lưu index => lần mở sau nạp từ cache.
        String[] kinds = {"add", "edit", "overwrite", "delete"};
        for (String kind : kinds) {
            DictionaryService s = DictionaryService.open(dataFile, indexFile, historyFile);
            check("precondition for " + kind + ": opened from cache", s.isIndexFromCache());
            switch (kind) {
                case "add" -> s.addSlang("zz-dirty", List.of("dirty add"), DuplicatePolicy.REJECT);
                case "edit" -> s.editSlang(s.lookupSlang("zz-dirty").get(0), "zz-dirty", List.of("dirty edit"));
                case "overwrite" -> s.addSlang("zz-dirty", List.of("dirty overwrite"), DuplicatePolicy.OVERWRITE);
                default -> s.deleteSlang(s.lookupSlang("zz-dirty").get(0));
            }
            s.close();
            DictionaryService next = DictionaryService.open(dataFile, indexFile, historyFile);
            check(kind + " then close(): next open uses cache", next.isIndexFromCache());
            check(kind + " then close(): data equals file", next.getAllWords().equals(SlangFileReader.read(dataFile)));
        }
        return DictionaryService.open(dataFile, indexFile, historyFile);
    }

    private static boolean closeTwice(DictionaryService s) {
        s.close();
        s.close();
        return true;
    }

    // ------------------------------------------------------------------ reset (E4)

    private static DictionaryService resetTests(DictionaryService svc) throws IOException {
        section("Reset (E4)");
        svc.searchBySlang("ama");
        int historyBefore = svc.getHistory().size();
        svc.addSlang("zz-before-reset", List.of("will vanish"), DuplicatePolicy.REJECT);
        svc.resetToOriginal();

        List<SlangWord> original = SlangFileReader.read(originalFile);
        check("after reset: data equals original", svc.getAllWords().equals(original));
        check("after reset: current file is a byte copy of original", Arrays.equals(Files.readAllBytes(originalFile), Files.readAllBytes(dataFile)));
        check("after reset: added entry is gone", !svc.exists("zz-before-reset"));
        check("after reset: history kept", svc.getHistory().size() == historyBefore);

        DictionaryService reopened = DictionaryService.open(dataFile, indexFile, historyFile);
        check("reopen after reset (no close): index from cache", reopened.isIndexFromCache());
        check("reopen after reset: data equals original", reopened.getAllWords().equals(original));

        check("works after reset: add", reopened.addSlang("zz-after-reset", List.of("fine"), DuplicatePolicy.REJECT) == AddResult.ADDED);
        check("works after reset: file consistent", SlangFileReader.read(dataFile).size() == original.size() + 1);
        check("works after reset: delete", reopened.deleteSlang(reopened.lookupSlang("zz-after-reset").get(0)));
        return reopened;
    }

    // ------------------------------------------------------------------ write failure keeps RAM == file

    /**
     * Mô phỏng lỗi ghi file (không phụ thuộc quyền hệ điều hành): dời thư mục dữ liệu sang chỗ khác và đặt một
     * FILE THƯỜNG vào đúng đường dẫn thư mục đó, nên mọi lần ghi đều ném IOException.
     */
    private static void failureAtomicityTests(DictionaryService svc, Path dir) throws IOException {
        section("Write failure leaves memory and file unchanged");
        List<SlangWord> before = new ArrayList<>(svc.getAllWords());
        byte[] fileBefore = Files.readAllBytes(dataFile);
        Path aside = dir.resolveSibling(dir.getFileName() + ".aside");

        Files.move(dir, aside);
        Files.writeString(dir, "blocker");
        try {
            expectIoFailure("add", () -> svc.addSlang("zz-fail-add", List.of("x"), DuplicatePolicy.DUPLICATE));
            expectIoFailure("overwrite", () -> svc.addSlang(before.get(0).getSlang(), List.of("x"), DuplicatePolicy.OVERWRITE));
            expectIoFailure("edit", () -> svc.editSlang(svc.getAllWords().get(0), "zz-fail-edit", List.of("x")));
            expectIoFailure("delete", () -> svc.deleteSlang(svc.getAllWords().get(0)));
            check("memory unchanged after failed writes", svc.getAllWords().equals(before));
            check("failed add/edit left no trace in lookups", !svc.exists("zz-fail-add") && !svc.exists("zz-fail-edit"));
            check("F2 index unchanged after failed writes", svc.searchByDefinition("x").equals(SlangIndex.build(before).findByDefinition("x")));
        } finally {
            Files.delete(dir);
            Files.move(aside, dir);
        }
        check("file unchanged after failed writes", Arrays.equals(fileBefore, Files.readAllBytes(dataFile)));
    }

    private interface IoAction {
        void run() throws IOException;
    }

    private static void expectIoFailure(String name, IoAction action) {
        try {
            action.run();
            check(name + " throws IOException when the file cannot be written", false);
        } catch (IOException expected) {
            check(name + " throws IOException when the file cannot be written", true);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static SlangWord randomWord(Random rnd, List<String> vocabulary, String slang) {
        int meaningCount = 1 + rnd.nextInt(3);
        List<String> meanings = new ArrayList<>();
        for (int m = 0; m < meaningCount; m++) {
            StringBuilder sb = new StringBuilder();
            int words = 1 + rnd.nextInt(4);
            for (int i = 0; i < words; i++) sb.append(i > 0 ? " " : "").append(vocabulary.get(rnd.nextInt(vocabulary.size())));
            meanings.add(sb.toString());
        }
        return new SlangWord(slang, meanings);
    }

    private static String pickKey(Random rnd, List<SlangWord> model) {
        return model.get(rnd.nextInt(model.size())).getSlang();
    }

    private static List<Integer> idsOf(List<SlangWord> model, String slang) {
        String key = TextNormalizer.normalizeSlang(slang);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            if (TextNormalizer.normalizeSlang(model.get(i).getSlang()).equals(key)) ids.add(i);
        }
        return ids;
    }

    private static void collectTokens(SlangWord w, List<String> into) {
        for (String m : w.getMeanings()) into.addAll(TextNormalizer.tokenize(TextNormalizer.normalizeText(m)));
    }

    private static void expect(String name, Reason reason, Runnable action) {
        try {
            action.run();
            check(name, false);
        } catch (ValidationException e) {
            check(name + " -> " + reason, e.getReason() == reason);
        }
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
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : (Iterable<Path>) files::iterator) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
    }
}
