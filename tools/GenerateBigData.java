import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Sinh file dữ liệu lớn (mặc định 100.000 slang) để test hiệu năng tìm kiếm (< 1s).
 * Lấy toàn bộ entry thật làm đầu, rồi sinh thêm entry giả (slang ngẫu nhiên, nghĩa ghép từ từ vựng thật).
 *
 * Chạy (Java 11+), từ thư mục gốc repo:
 *   java tools/GenerateBigData.java [fileNguon] [fileDich] [soLuong]
 * Mặc định: data/slang.original.txt  data/slang.100k.txt  100000
 * Dùng seed cố định -> kết quả lặp lại được. File đích bị .gitignore, không commit.
 */
public class GenerateBigData {

    public static void main(String[] args) throws IOException {
        Path source = Paths.get(args.length > 0 ? args[0] : "data/slang.original.txt");
        Path target = Paths.get(args.length > 1 ? args[1] : "data/slang.100k.txt");
        int count = args.length > 2 ? Integer.parseInt(args[2]) : 100_000;

        List<String> realLines = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        Set<String> vocabulary = new HashSet<>();
        try (BufferedReader r = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                int sep = line.indexOf('`');
                if (sep <= 0 || line.startsWith("Slag`")) continue;
                realLines.add(line.strip());
                usedKeys.add(line.substring(0, sep).strip());
                for (String w : line.substring(sep + 1).split("[^A-Za-z]+")) {
                    if (w.length() >= 2) vocabulary.add(w);
                }
            }
        }
        List<String> words = new ArrayList<>(vocabulary);
        Random rnd = new Random(42);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            int written = 0;
            for (String real : realLines) {
                if (written >= count) break;
                w.write(real);
                w.write('\n');
                written++;
            }
            while (written < count) {
                String key;
                do {
                    int len = 3 + rnd.nextInt(6);
                    StringBuilder sb = new StringBuilder(len);
                    for (int i = 0; i < len; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
                    key = sb.toString();
                } while (!usedKeys.add(key));

                int meaningCount = 1 + rnd.nextInt(3);
                StringBuilder line = new StringBuilder(key).append('`');
                for (int m = 0; m < meaningCount; m++) {
                    if (m > 0) line.append("| ");
                    int wc = 1 + rnd.nextInt(5);
                    for (int i = 0; i < wc; i++) {
                        if (i > 0) line.append(' ');
                        line.append(words.get(rnd.nextInt(words.size())));
                    }
                }
                w.write(line.toString());
                w.write('\n');
                written++;
            }
            System.out.println("Wrote " + written + " entries (" + Math.min(realLines.size(), count)
                    + " real) to " + target.toAbsolutePath());
        }
    }
}
