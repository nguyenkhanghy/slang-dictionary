# L4 — Kịch bản video YouTube (≈ 6–8 phút)

Đề yêu cầu 3 phần: **cấu trúc dữ liệu**, **% code tham khảo**, **demo toàn bộ một chức năng**. Đây là gợi ý, bạn nói bằng lời của mình. Quay màn hình + mic, đặt video ở chế độ "Không công khai" hoặc "Công khai" (link phải mở được không cần đăng nhập). Kiểm tra link bằng cửa sổ ẩn danh.

## 0:00–0:30 Mở đầu
Họ tên, MSSV, tên đồ án (Slang Dictionary, Java 17 + Swing), nói sẽ trình bày ba phần. Cho thấy ứng dụng mở lên (chạy `java -jar`).

## 0:30–3:00 Cấu trúc dữ liệu (mở `docs/design-notes.md` hoặc sơ đồ vẽ tay)
1. Vì sao không chỉ dùng `List`: tìm tuần tự chậm, đề giới hạn 1 giây với 100.000 từ.
2. `List<SlangWord>` giữ toàn bộ; `Map<String, IntList>` khóa IN HOA cho F1 (O(1), một slang nhiều entry); inverted index cho F2 (khớp chuỗi con rồi kiểm tra lại).
3. Lưu index ra `slang.index.bin` (nhị phân tự định nghĩa; hợp lệ khi kích thước + CRC32 của file txt khớp; hỏng thì build lại, không crash). Cho xem số ms trong log: mở từ cache nhanh hơn build lại.
4. Thêm/sửa/xóa: cập nhật index tăng dần, ghi file txt trước (file tạm + move) rồi mới đổi RAM.
5. (Ngắn) Kiến trúc `ui → service → index/repository`, một luồng nền cho service.

## 3:00–3:30 % code tham khảo
Đọc con số từ `docs/references.md` (xem cách tính bên dưới) và nêu nguồn tham khảo chính. Nói rõ: mọi đoạn tham khảo đều có `// REF:` trong code. **Không bịa số**: nếu không có đoạn nào tham khảo ngoài, nói đúng như vậy và nói rõ phần AI hỗ trợ viết code theo quy định của môn (ghi `AI-generated (tên công cụ)` nếu bạn đã ghi như thế).

Cách tính gợi ý:
```bash
grep -rn "REF:" src tools                                   # các đoạn đã đánh dấu
find src/main/java tools -name '*.java' | xargs wc -l | tail -1   # tổng số dòng
```
% tham khảo = (số dòng thuộc các đoạn REF) / (tổng số dòng) × 100, lấy số dòng từng đoạn từ `docs/references.md`.

## 3:30–7:00 Demo trọn một chức năng — đề xuất **F4 Thêm từ lóng**
(chọn F4 vì có nhiều nhánh nhất; đổi sang chức năng khác nếu bạn thích)
1. Vào trang Thêm; bấm Thêm khi để trống → thông báo dữ liệu không hợp lệ.
2. Thêm một từ mới (vd `TESTWORD` với hai nghĩa) → thành công; sang Tìm theo từ lóng thấy ngay; số từ ở sidebar tăng.
3. Thêm lại từ đó → hộp thoại "Từ lóng đã tồn tại": chọn Duplicate (có hai entry), rồi làm lại và chọn Overwrite (còn một entry).
4. Cho xem `data/slang.current.txt` đã được ghi; đóng ứng dụng, mở lại → dữ liệu còn nguyên (index nạp từ cache).
5. (15 giây) Reset về dữ liệu gốc (có xác nhận) để chứng minh file gốc không bị sửa.
6. (15 giây) Nói về hiệu năng: tìm trên 100.000 từ vẫn vài ms.

## 7:00–7:30 Kết
Nói hạn chế đã biết (từ của ngày có thể đổi nếu thêm/xóa từ trong ngày; chưa có JUnit) và cảm ơn.

## Sau khi quay
Dán link vào `docs/self-grading.txt` (dòng "Video YouTube"), mở thử link ở cửa sổ ẩn danh.