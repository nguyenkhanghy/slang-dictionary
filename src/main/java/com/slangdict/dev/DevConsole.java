package com.slangdict.dev;

import com.slangdict.model.HistoryEntry;
import com.slangdict.model.SlangWord;
import com.slangdict.service.AddResult;
import com.slangdict.service.DictionaryService;
import com.slangdict.service.DuplicatePolicy;
import com.slangdict.service.EditResult;
import com.slangdict.service.InputValidator;
import com.slangdict.service.QuizQuestion;
import com.slangdict.service.QuizService;
import com.slangdict.service.ValidationException;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu console TẠM THỜI để thử các chức năng khi chưa có GUI. Sẽ bị xóa ở Phase 5 (task U2).
 * Lớp này đóng vai "UI": hỏi xác nhận trước các thao tác phá hủy rồi mới gọi service.
 */
public final class DevConsole {

    private static final int MAX_SHOWN = 20;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DictionaryService service;
    private final QuizService quiz;
    private final PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private final Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);

    private DevConsole(DictionaryService service) {
        this.service = service;
        this.quiz = new QuizService(service);
    }

    /** Chạy menu cho tới khi thoát; luôn gọi {@code service.close()} khi kết thúc. */
    public static void run(DictionaryService service) {
        try {
            new DevConsole(service).loop();
        } finally {
            service.close();
        }
    }

    private void loop() {
        while (true) {
            out.println();
            out.println("=== Slang Dictionary (dev console) ===");
            out.println("1. Search by slang word      5. Add slang");
            out.println("2. Search by definition      6. Edit slang");
            out.println("3. Show history              7. Delete slang");
            out.println("4. Clear history             8. Reset to original data");
            out.println("9. Random slang              10. Quiz: slang -> meaning");
            out.println("11. Quiz: meaning -> slang");
            out.println("0. Exit");
            String choice = ask("Choose: ");
            if (choice == null) {
                return;
            }
            try {
                switch (choice.strip()) {
                    case "1" -> print(service.searchBySlang(askOrEmpty("Slang: ")));
                    case "2" -> print(service.searchByDefinition(askOrEmpty("Keyword: ")));
                    case "3" -> showHistory();
                    case "4" -> {
                        service.clearHistory();
                        out.println("History cleared.");
                    }
                    case "5" -> add();
                    case "6" -> edit();
                    case "7" -> delete();
                    case "8" -> reset();
                    case "9" -> randomSlang();
                    case "10" -> playQuiz(quiz.nextSlangToMeaningQuestion());
                    case "11" -> playQuiz(quiz.nextMeaningToSlangQuestion());
                    case "0" -> {
                        return;
                    }
                    default -> out.println("Invalid choice.");
                }
            } catch (ValidationException e) {
                out.println("Invalid input: " + e.getReason() + " (" + e.getMessage() + ")");
            } catch (IOException e) {
                out.println("I/O error, nothing was changed: " + e);
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private void add() throws IOException {
        String slang = askOrEmpty("New slang: ");
        List<String> meanings = InputValidator.parseMeanings(askOrEmpty("Meanings (separate with |): "));

        AddResult result = service.addSlang(slang, meanings, DuplicatePolicy.REJECT);
        if (result == AddResult.CONFLICT) {
            out.println("'" + slang.strip() + "' already exists:");
            print(service.lookupSlang(slang));
            String answer = askOrEmpty("[O]verwrite all / [D]uplicate / [C]ancel: ").strip().toLowerCase();
            if (answer.equals("o")) {
                result = service.addSlang(slang, meanings, DuplicatePolicy.OVERWRITE);
            } else if (answer.equals("d")) {
                result = service.addSlang(slang, meanings, DuplicatePolicy.DUPLICATE);
            } else {
                out.println("Cancelled.");
                return;
            }
        }
        out.println("Result: " + result);
    }

    private void edit() throws IOException {
        SlangWord target = pick(service.lookupSlang(askOrEmpty("Slang to edit: ")));
        if (target == null) {
            return;
        }
        String newSlang = askOrEmpty("New slang (blank = keep '" + target.getSlang() + "'): ");
        if (newSlang.isBlank()) {
            newSlang = target.getSlang();
        }
        String rawMeanings = askOrEmpty("New meanings (blank = keep '" + String.join("| ", target.getMeanings()) + "'): ");
        List<String> meanings = rawMeanings.isBlank() ? target.getMeanings() : InputValidator.parseMeanings(rawMeanings);
        out.println("Result: " + service.editSlang(target, newSlang, meanings));
    }

    private void delete() throws IOException {
        SlangWord target = pick(service.lookupSlang(askOrEmpty("Slang to delete: ")));
        if (target == null) {
            return;
        }
        if (!confirm("Delete '" + target + "'?")) {
            out.println("Cancelled.");
            return;
        }
        out.println(service.deleteSlang(target) ? "Deleted." : "Entry not found.");
    }

    private void reset() throws IOException {
        if (!confirm("Reset ALL slang words to the original list? Your changes will be lost.")) {
            out.println("Cancelled.");
            return;
        }
        service.resetToOriginal();
        out.println("Reset done. Entries: " + service.size());
    }

    private void randomSlang() {
        quiz.randomSlang().ifPresentOrElse(
                w -> out.println("Slang of the day: " + w.getSlang() + "  ->  " + QuizService.meaningText(w)),
                () -> out.println("No data."));
    }

    private void playQuiz(Optional<QuizQuestion> question) {
        if (question.isEmpty()) {
            out.println("Not enough data to build a quiz (need at least 4 clearly different entries).");
            return;
        }
        QuizQuestion q = question.get();
        out.println("Question: " + q.prompt());
        for (int i = 0; i < q.options().size(); i++) {
            out.println("  " + (i + 1) + ". " + q.options().get(i));
        }
        String answer = askOrEmpty("Your answer (1-" + q.options().size() + "): ").strip();
        int choice;
        try {
            choice = Integer.parseInt(answer) - 1;
        } catch (NumberFormatException e) {
            choice = -1;
        }
        out.println(q.isCorrect(choice) ? "Correct!" : "Wrong. Correct answer: " + (q.correctIndex() + 1) + ". " + q.correctOption());
    }

    private void showHistory() {
        List<HistoryEntry> history = service.getHistory();
        out.println(history.size() + " history entries (newest first):");
        for (HistoryEntry e : history.subList(0, Math.min(history.size(), MAX_SHOWN))) {
            out.println("  [" + TIME_FORMAT.format(e.time()) + "] " + e.type() + "  " + e.query());
        }
    }

    // ------------------------------------------------------------------ helpers

    private SlangWord pick(List<SlangWord> candidates) {
        if (candidates.isEmpty()) {
            out.println("No such slang.");
            return null;
        }
        for (int i = 0; i < candidates.size(); i++) {
            out.println("  " + (i + 1) + ". " + candidates.get(i).getSlang() + "  ->  "
                    + String.join(" | ", candidates.get(i).getMeanings()));
        }
        String answer = askOrEmpty("Number (blank = cancel): ").strip();
        try {
            int n = Integer.parseInt(answer);
            if (n >= 1 && n <= candidates.size()) {
                return candidates.get(n - 1);
            }
        } catch (NumberFormatException ignored) {
            // rơi xuống thông báo hủy
        }
        out.println("Cancelled.");
        return null;
    }

    private boolean confirm(String question) {
        return askOrEmpty(question + " (y/N): ").strip().equalsIgnoreCase("y");
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private String askOrEmpty(String prompt) {
        String line = ask(prompt);
        return line == null ? "" : line;
    }

    private void print(List<SlangWord> results) {
        out.println(results.size() + " result(s)");
        for (SlangWord w : results.subList(0, Math.min(results.size(), MAX_SHOWN))) {
            out.println("  " + w.getSlang() + "  ->  " + String.join(" | ", w.getMeanings()));
        }
        if (results.size() > MAX_SHOWN) {
            out.println("  ... and " + (results.size() - MAX_SHOWN) + " more");
        }
    }
}