package com.slangdict.dev;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import com.slangdict.model.HistoryEntry;
import com.slangdict.model.SlangWord;
import com.slangdict.service.DictionaryService;

/**
 * Menu console TẠM THỜI để thử các chức năng khi chưa có GUI. Sẽ bị xóa ở Phase 5 (task U2).
 */
public final class DevConsole {

    private static final int MAX_SHOWN = 20;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private DevConsole() {
    }

    public static void run(DictionaryService service) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);

        while (true) {
            out.println();
            out.println("=== Slang Dictionary (dev console) ===");
            out.println("1. Search by slang word");
            out.println("2. Search by definition");
            out.println("3. Show history");
            out.println("4. Clear history");
            out.println("0. Exit");
            out.print("Choose: ");
            if (!in.hasNextLine()) {
                return;
            }
            switch (in.nextLine().strip()) {
                case "1" -> {
                    out.print("Slang: ");
                    if (!in.hasNextLine()) return;
                    print(out, service.searchBySlang(in.nextLine()));
                }
                case "2" -> {
                    out.print("Keyword: ");
                    if (!in.hasNextLine()) return;
                    print(out, service.searchByDefinition(in.nextLine()));
                }
                case "3" -> {
                    List<HistoryEntry> history = service.getHistory();
                    out.println(history.size() + " history entries (newest first):");
                    for (HistoryEntry e : history.subList(0, Math.min(history.size(), MAX_SHOWN))) {
                        out.println("  [" + TIME_FORMAT.format(e.time()) + "] " + e.type() + "  " + e.query());
                    }
                }
                case "4" -> {
                    service.clearHistory();
                    out.println("History cleared.");
                }
                case "0" -> {
                    return;
                }
                default -> out.println("Invalid choice.");
            }
        }
    }

    private static void print(PrintStream out, List<SlangWord> results) {
        out.println(results.size() + " result(s)");
        for (SlangWord w : results.subList(0, Math.min(results.size(), MAX_SHOWN))) {
            out.println("  " + w.getSlang() + "  ->  " + String.join(" | ", w.getMeanings()));
        }
        if (results.size() > MAX_SHOWN) {
            out.println("  ... and " + (results.size() - MAX_SHOWN) + " more");
        }
    }
}
