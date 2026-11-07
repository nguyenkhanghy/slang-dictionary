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
| `slang.index.bin` | Index đã serialize | ignore (tự sinh) |
| `history.txt` | Lịch sử tìm kiếm | ignore (tự sinh) |

## Công cụ (`tools/`)
```bash
java tools/ValidateData.java data/slang.original.txt   # kiểm tra file dữ liệu (mong đợi 0 ERROR)
java tools/GenerateBigData.java                         # sinh data/slang.100k.txt để test hiệu năng
```

## Đóng gói nộp bài
Đặt `slang-dictionary.jar` và thư mục `data/` **cùng một thư mục** rồi nén cùng source, ảnh commit, file tự chấm điểm.
