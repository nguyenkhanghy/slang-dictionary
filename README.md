# Slang Dictionary

Ứng dụng từ điển tiếng lóng (Java 17, Swing) — Đồ án DAMH#1, môn CSC13002 Lập trình ứng dụng Java (HCMUS – FIT).

> Mô tả đầy đủ yêu cầu, kiến trúc, roadmap, task tracker và quy tắc làm việc: xem [`PROJECT.md`](PROJECT.md).

## Yêu cầu
- JDK 17+
- Maven 3.8+ (hoặc build bằng IDE)

## Build & chạy
```bash
mvn clean package
java -jar target/slang-dictionary.jar
```
Chạy từ **thư mục gốc repo** để ứng dụng tìm thấy thư mục `data/`.
Có thể chỉ định thư mục dữ liệu khác: `java -Dslang.dataDir=/duong/dan/data -jar target/slang-dictionary.jar`.

## Thư mục dữ liệu `data/`
| File | Vai trò | Git |
|------|---------|-----|
| `slang.original.txt` | Dữ liệu gốc đã làm sạch (chỉ đọc) | commit |
| `slang.current.txt` | Dữ liệu đang dùng (add/edit/delete) | ignore (tự sinh) |
| `slang.index.bin` | Index đã lưu (nhị phân) — tự build lại nếu thiếu/hỏng/lệch | ignore (tự sinh) |
| `history.txt` | Lịch sử tìm kiếm | ignore (tự sinh) |

## Chạy thử (giai đoạn phát triển)
Hiện `Main` mở một menu console tạm thời (F1 tìm theo slang, F2 tìm theo definition, F3 history).
Sẽ được thay bằng giao diện Swing ở Phase 5.

## Kiểm tra hiệu năng (100.000 slang)
```bash
java tools/GenerateBigData.java            # sinh data/slang.100k.txt (1 lần, file này bị .gitignore)
mvn clean package
java -cp target/classes:target/test-classes com.slangdict.PerfCheck
```
(Windows: thay `:` bằng `;` trong classpath.) Kết quả cuối cùng phải là `RESULT: PASS`.

## Công cụ (`tools/`)
```bash
java tools/ValidateData.java data/slang.original.txt   # kiểm tra file dữ liệu (mong đợi 0 ERROR)
java tools/GenerateBigData.java                         # sinh data/slang.100k.txt để test hiệu năng
```

## Đóng gói nộp bài
Đặt `slang-dictionary.jar` và thư mục `data/` **cùng một thư mục** rồi nén cùng source, ảnh commit, file tự chấm điểm.
