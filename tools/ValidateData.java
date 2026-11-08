import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Công cụ kiểm tra file dữ liệu slang. Chạy (Java 11+):
 *   java tools/ValidateData.java data/slang.original.txt
 * Exit code: 0 = không có lỗi nghiêm trọng, 1 = có ERROR.
 * Quy ước: SLANG`MEANING1|MEANING2 ; chỉ '|' ngăn các nghĩa ('/' là một phần của nghĩa).
 */
public class ValidateData {
    public static void main(String[] args) throws IOException {
        Path p = Paths.get(args.length > 0 ? args[0] : "data/slang.original.txt");
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        int errors = 0, warns = 0;
        Map<String, Integer> seenLine = new HashMap<>();
        Map<String, List<Integer>> keyLines = new TreeMap<>();

        for (int i = 0; i < lines.size(); i++) {
            int n = i + 1;
            String line = lines.get(i);
            if (n == 1 && line.startsWith("Slag`")) { System.out.println("INFO  L1 header (sẽ bị bỏ khi load): " + line); continue; }
            int bt = line.indexOf('`');
            if (line.isBlank()) { errors++; System.out.println("ERROR L" + n + " dòng trống"); continue; }
            if (bt < 0) { errors++; System.out.println("ERROR L" + n + " thiếu dấu ` (dòng mồ côi?): " + line); continue; }
            if (line.indexOf('`', bt + 1) >= 0) { errors++; System.out.println("ERROR L" + n + " có nhiều hơn 1 dấu `: " + line); continue; }
            String slang = line.substring(0, bt), meaning = line.substring(bt + 1);
            if (slang.isBlank()) { errors++; System.out.println("ERROR L" + n + " slang rỗng: " + line); }
            if (meaning.isBlank()) { errors++; System.out.println("ERROR L" + n + " meaning rỗng: " + line); }
            for (String seg : meaning.split("\\|", -1)) {
                if (seg.isBlank() && !meaning.isBlank()) { errors++; System.out.println("ERROR L" + n + " có nghĩa rỗng giữa các dấu |: " + line); break; }
            }
            if (!line.equals(line.strip())) { warns++; System.out.println("WARN  L" + n + " thừa khoảng trắng đầu/cuối: [" + line + "]"); }
            if (slang.length() != slang.strip().length() || meaning.length() != meaning.strip().length())
                { warns++; System.out.println("WARN  L" + n + " khoảng trắng thừa quanh slang/meaning: [" + line + "]"); }
            for (char c : line.toCharArray()) if (c > 127) { warns++; System.out.println("WARN  L" + n + " ký tự non-ASCII '" + c + "' (kiểm tra lỗi encoding): " + line); break; }
            if (line.contains("\t")) { warns++; System.out.println("WARN  L" + n + " chứa tab: " + line); }
            if (line.contains("  ")) { warns++; System.out.println("WARN  L" + n + " có 2 dấu cách liên tiếp: " + line); }
            String norm = line.strip();
            Integer first = seenLine.putIfAbsent(norm, n);
            if (first != null) { warns++; System.out.println("WARN  L" + n + " trùng y hệt dòng L" + first + ": " + line); }
            keyLines.computeIfAbsent(slang.strip(), k -> new ArrayList<>()).add(n);
        }
        long dupKeys = keyLines.values().stream().filter(v -> v.size() > 1).count();
        System.out.println("\nINFO  Tổng dòng: " + lines.size() + " | slang key khác nhau: " + keyLines.size() + " | key lặp: " + dupKeys);
        System.out.println("TỔNG KẾT: " + errors + " ERROR, " + warns + " WARN");
        System.exit(errors > 0 ? 1 : 0);
    }
}
