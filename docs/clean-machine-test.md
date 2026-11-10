# L1 — Checklist chạy trên "máy sạch"

Mục tiêu: giảng viên giải nén zip, chạy `java -jar`, mọi thứ hoạt động, không cần repo/IDE/mạng. Ghi kết quả vào cột cuối rồi dán cho AI.
Máy sạch lý tưởng = máy khác chỉ có JDK 17. Nếu không có: dùng thư mục mới `/tmp/clean` và JDK 17 (bước 0).

## 0. Chuẩn bị
```bash
java -version                 # cần 17.x; nếu máy có nhiều JDK:
JAVA17="$(/usr/libexec/java_home -v 17)/bin/java"; "$JAVA17" -version
mvn clean package             # phải BUILD SUCCESS, có target/slang-dictionary.jar
rm -rf /tmp/clean && mkdir -p /tmp/clean/data
cp target/slang-dictionary.jar /tmp/clean/
cp data/slang.original.txt /tmp/clean/data/      # CHỈ file gốc, đúng như gói nộp
```

## 1. Bảng kiểm
| # | Việc | Lệnh / thao tác | Mong muốn | Kết quả |
|---|------|-----------------|-----------|---------|
| 1 | Lần đầu, chạy từ trong thư mục | `cd /tmp/clean && "$JAVA17" -jar slang-dictionary.jar` | Cửa sổ tải rồi cửa sổ chính; log `Opened 7641 entries (index rebuilt)`; không hộp thoại lỗi | |
| 2 | Thử nhanh 10 chức năng | Tìm từ lóng (vd `lol`), tìm theo nghĩa, Lịch sử, Thêm (cả trường hợp trùng), Sửa, Xóa (có xác nhận), Khôi phục, Ngẫu nhiên, 2 Quiz | Không lỗi, không treo | |
| 3 | Thoát bình thường | Đóng cửa sổ | Cửa sổ đóng ngay; `ls -la /tmp/clean/data` có `slang.original.txt`, `slang.current.txt`, `slang.index.bin`, `history.txt` (nếu đã tìm); **không** có file `*.tmp` | |
| 4 | Lần hai, chạy từ thư mục cha | `cd /tmp && "$JAVA17" -jar clean/slang-dictionary.jar` | Tìm đúng `clean/data` (không phải `/tmp/data`); log `index from cache`; dữ liệu đã sửa ở bước 2 còn nguyên | |
| 5 | Mở bằng double-click (macOS Finder) | Chuột phải jar → Open With → Jar Launcher | Mở bình thường (thư mục làm việc khác, vẫn tìm `data/` cạnh jar) | |
| 6 | Offline | Tắt Wi-Fi, chạy lại | Không khác gì | |
| 7 | Mất index | `rm /tmp/clean/data/slang.index.bin`, chạy | Tự build lại, một dòng log, không hộp thoại | |
| 8 | Sửa file ngoài ứng dụng | Thêm tay một dòng hợp lệ vào `slang.current.txt` rồi chạy | CRC lệch → build lại; dòng mới tìm được | |
| 9 | Lần đầu thật sự (chưa có current/index/history) | `rm /tmp/clean/data/{slang.current.txt,slang.index.bin,history.txt}`, chạy | Giống bước 1; dữ liệu về như gốc | |
| 10 | Hiệu năng 100k (nếu có `data/slang.100k.txt`) | `mkdir /tmp/clean100k && cp data/slang.100k.txt /tmp/clean100k/slang.original.txt && "$JAVA17" -Dslang.dataDir=/tmp/clean100k -jar /tmp/clean/slang-dictionary.jar` | Lần đầu mở ≈ 1 s (ghi lại số ms trong log); lần hai nhanh hơn; Tìm `a` hiện được (khựng ≲ 0,2 s) | |
| 11 | Thư mục `data/` chỉ đọc (chỉ để biết, ghi vào README) | `chmod -w /tmp/clean/data` sau khi xóa current, chạy | Hộp thoại lỗi mở dữ liệu thân thiện, thoát mã 1 (ứng dụng cần ghi được `data/` lần đầu) | |
| 12 | Gói nộp thật (làm SAU L5) | giải nén `MSSV_SoDiemTuDanhGia.zip` vào thư mục mới, chạy jar trong đó | Bước 1–3 đạt với đúng nội dung zip | |

Ghi chú: ứng dụng cần quyền **ghi** vào `data/` (tạo `slang.current.txt` lần đầu). Hãy ghi một dòng này trong README để giảng viên không giải nén vào chỗ chỉ đọc.