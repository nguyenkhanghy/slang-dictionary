# Ghi chú thiết kế — cấu trúc dữ liệu (dùng khi quay video)

> Nội dung lấy từ `PROJECT.md` mục 3.4–3.6 và các quyết định đã chốt. Trước khi quay, mở `index/SlangIndex.java` đối chiếu một lần để câu chữ trong video khớp code (đặc biệt kiểu của posting list).

## 1. Bài toán và số liệu
- 7.641 từ lóng thật; yêu cầu mọi thao tác < 1 s ngay cả với 100.000 từ.
- 17 khóa slang bị trùng (vd `AWS`, `GOAT`) nên **một slang có thể có nhiều entry**.

## 2. Cấu trúc trong RAM (`index/SlangIndex`)
| Mục đích | Cấu trúc | Độ phức tạp |
|----------|----------|-------------|
| Lưu toàn bộ, giữ thứ tự | `List<SlangWord>`, id = chỉ số | truy cập O(1) |
| F1 tìm theo slang | `Map<String, IntList>`: khóa = slang chuẩn hóa IN HOA, giá trị = danh sách id | O(1) trung bình |
| F2 tìm theo nghĩa | **inverted index**: token chữ thường → danh sách id; keyword khớp **chuỗi con**: quét từ vựng tìm token chứa keyword, hợp các danh sách, rồi kiểm tra lại bằng `contains` | tỉ lệ với số kết quả |
| F8–F10 random | truy cập ngẫu nhiên trên `List` | O(1) |
- `IntList` là danh sách `int` tự viết (tránh boxing, nhỏ và nhanh khi 100k phần tử).
- `SlangWord` bất biến, tự chặn dữ liệu sai định dạng.

## 3. Lưu index để lần sau không phải index lại (R4)
- File `data/slang.index.bin`, định dạng nhị phân tự định nghĩa bằng `DataOutputStream` (không dùng Java Serialization vì chậm với 100k đối tượng).
- Header: `MAGIC, VERSION, kích thước, CRC32` của `slang.current.txt`; cuối file có `END_MAGIC`.
- Khi mở: kích thước + CRC32 khớp thì nạp thẳng index (không parse lại txt); thiếu/hỏng/lệch thì build lại từ txt rồi ghi lại. **Không crash.**
- Dùng CRC32 thay vì ngày sửa file vì ngày sửa đổi khi copy/giải nén zip.
- Đo: mở 7.641 từ ≈ 26 ms (cache) / 69 ms (build lại); ở 100k build lại ≈ 1 s, nạp cache ≈ 0,3–0,5 s.

## 4. Thêm / sửa / xóa (R5)
- Cập nhật index **tăng dần** (`add`, `replaceAt`, `removeAt`) thay vì build lại (build 100k ≈ 1 s là quá chậm cho mỗi thao tác).
- Thứ tự an toàn: tạo danh sách mới → **ghi file txt trước** (file tạm + move, `AtomicFiles`) → rồi mới đổi index trong RAM. Ghi lỗi thì RAM không đổi nên RAM và file không bao giờ lệch.
- Index trên đĩa chỉ ghi khi đóng ứng dụng; nếu tắt đột ngột, lần sau CRC lệch và index tự build lại.

## 5. Dữ liệu lỗi (yêu cầu của GV)
- Tách dòng tại dấu `` ` `` **đầu tiên**; chỉ phần nghĩa mới tách bằng `|` (có 5 slang chứa chính ký tự `|`, vd `8-|`). Dấu `/` không phải dấu ngăn nghĩa.
- Header, 5 dòng "mồ côi" (nghĩa bị xuống dòng, nối vào dòng trước), khoảng trắng thừa, dòng trùng y hệt, 2 dòng lỗi mã hóa (`®`, `™`): sửa tay **một lần** thành `slang.original.txt` sạch; `SlangFileReader` vẫn phòng thủ nếu file bị sửa tay (dòng hỏng bị bỏ + log).
- `slang.original.txt` **chỉ đọc**; thao tác ghi chỉ đụng `slang.current.txt`; history ở file riêng `history.txt`.

## 6. Kiến trúc và luồng
`ui → service → index / repository → file`. `ui` không chứa logic nghiệp vụ và không đọc file; `service` không import Swing. Service không thread-safe nên mọi lời gọi từ giao diện đi qua **một luồng nền duy nhất** (`ServiceExecutor`); tìm kiếm dùng cơ chế "yêu cầu mới nhất thắng" (`LatestTask`), thao tác ghi dùng `BackgroundCall` (mỗi lần đều chạy).

## 7. Xử lý lỗi (H1)
Mọi log lỗi qua `LogSupport`: terminal một dòng, stack trace chỉ khi chạy `-Dslang.debug=true`. Lỗi mở dữ liệu: hộp thoại thân thiện rồi thoát mã 1. Ngoại lệ không bắt: `GlobalErrorHandler` (log + một hộp thoại).

## 8. Kiểm thử (chạy tay, so với mô hình tham chiếu đơn giản)
`PerfCheck` (đúng + tốc độ F1/F2 trên 100k), `MutationCheck` (704 kiểm tra thêm/sửa/xóa/reset/lưu trữ), `QuizCheck` (33 kiểm tra).