# Slang Dictionary

Ứng dụng từ điển tiếng lóng (Java 17, Swing) — Đồ án DAMH#1, môn CSC13002 Lập trình ứng dụng Java (HCMUS – FIT).

> Mô tả đầy đủ yêu cầu, kiến trúc, roadmap, task tracker và quy tắc làm việc: xem [`PROJECT.md`](PROJECT.md)

## Chức năng
Tìm theo từ lóng (F1) và theo nghĩa (F2), lịch sử tìm kiếm (F3), thêm có xử lý trùng Overwrite/Duplicate (F4), sửa (F5), xóa có xác nhận (F6), khôi phục dữ liệu gốc (F7), từ lóng của ngày + từ ngẫu nhiên (F8), hai loại quiz 4 đáp án (F9, F10). Giao diện tiếng Việt, chạy hoàn toàn offline.

## Yêu cầu
- JDK 17+
- Maven 3.8+ (hoặc build bằng IDE)

## Build & chạy
```bash
mvn clean package
java -jar target/slang-dictionary.jar
```
Thư mục dữ liệu `data/` được tìm theo thứ tự: `-Dslang.dataDir=...` → `./data` (nếu có `slang.original.txt`) → `data/` nằm cạnh file jar. Vì vậy chạy được cả từ thư mục gốc repo lẫn khi chỉ có `slang-dictionary.jar` + `data/` cạnh nhau.
Ứng dụng cần quyền **ghi** vào `data/` (lần đầu tự tạo `slang.current.txt`).

Chỉ định thư mục dữ liệu khác: `java -Dslang.dataDir=/duong/dan/data -jar target/slang-dictionary.jar`.
Bật stack trace chi tiết khi gặp lỗi (mặc định terminal chỉ in một dòng): `java -Dslang.debug=true -jar target/slang-dictionary.jar`.
Tham số dòng lệnh khác không được dùng. Môi trường không có đồ họa (headless) sẽ in thông báo và thoát mã 1.

## Thư mục dữ liệu `data/`
| File | Vai trò | Git |
|------|---------|-----|
| `slang.original.txt` | Dữ liệu gốc đã làm sạch (chỉ đọc) | commit |
| `slang.current.txt` | Dữ liệu đang dùng (add/edit/delete) | ignore (tự sinh) |
| `slang.index.bin` | Index đã lưu (nhị phân) — tự build lại nếu thiếu/hỏng/lệch | ignore (tự sinh) |
| `history.txt` | Lịch sử tìm kiếm | ignore (tự sinh) |

## Kiểm tra hiệu năng và tính đúng (100.000 slang)
```bash
java tools/GenerateBigData.java            # sinh data/slang.100k.txt (1 lần, file này bị .gitignore)
mvn clean package
java -cp target/classes:target/test-classes com.slangdict.PerfCheck
java -cp target/classes:target/test-classes com.slangdict.MutationCheck   # add/edit/delete/reset; chạy trên bản sao, không đụng data/
java -cp target/classes:target/test-classes com.slangdict.QuizCheck
java -cp target/classes:target/test-classes com.slangdict.DailySlangCheck # từ lóng của ngày (F8)
```
(Windows: thay `:` bằng `;` trong classpath.) Tất cả bài test phải in ra `RESULT: PASS`.

## Công cụ (`tools/`)
```bash
java tools/ValidateData.java data/slang.original.txt   # kiểm tra file dữ liệu (mong đợi 0 ERROR)
java tools/GenerateBigData.java                         # sinh data/slang.100k.txt để test hiệu năng
```