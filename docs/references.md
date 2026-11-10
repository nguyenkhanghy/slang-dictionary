# Code tham khảo (`// REF:`) — để tính % tham khảo

> **BẢN MẪU.** Quy định trong `PROJECT.md` mục 6.7: mọi đoạn lấy từ internet / AI / người khác phải có comment `// REF: <nguồn hoặc "AI-generated (tên công cụ)">` và được liệt kê ở đây để tính %. Lần kiểm tra gần nhất (`grep -rn "REF:" src tools`) **không có dòng `// REF:` nào** trong code. Bạn cần tự khai đúng thực tế; không điền số nếu chưa đếm.

## 1. Việc bạn cần quyết định (theo quy định của môn / giảng viên)
- Phần nào của code do bạn tự viết, phần nào do công cụ AI hoặc nguồn ngoài tạo ra hoặc sửa? Nếu có dùng AI để viết code, đề bài (mục 2.3) yêu cầu đánh dấu và thống kê. Nếu không chắc cách khai báo, hỏi giảng viên.
- Mức chi tiết: đánh dấu theo **file** (đơn giản, nên dùng nếu cả file do công cụ tạo) hoặc theo **đoạn** (ghi số dòng).

## 2. Bảng khai báo (điền)
| File / đoạn | Số dòng | Nguồn (URL hoặc "AI-generated (tên công cụ)") | Ghi chú (tự sửa thế nào) |
|-------------|--------:|-----------------------------------------------|---------------------------|
| _(điền)_ | | | |

## 3. Tính %
```bash
# tổng số dòng code (src + tools):
find src tools -name '*.java' | xargs wc -l | tail -1
# số dòng của từng file (để điền bảng):
wc -l $(find src tools -name '*.java') | sort -n
# các đoạn đã đánh dấu trong code:
grep -rn "REF:" src tools
```
% tham khảo = (tổng số dòng đã khai ở bảng) / (tổng số dòng code) × 100.

## 4. (Tùy chọn) Chèn comment `// REF:` hàng loạt cho các file bạn đã quyết định là tham khảo
Chỉ chạy sau khi bạn chốt danh sách file; script bỏ qua file đã có `REF:`. Hãy `git status` sạch trước khi chạy và xem `git diff` sau đó.
```bash
for f in $(cat docs/ref-files.txt); do        # docs/ref-files.txt: mỗi dòng một đường dẫn file .java
  grep -q "REF:" "$f" || perl -0pi -e 's/^(package [^\n]+\n)/$1\n\/\/ REF: AI-generated (ghi tên công cụ)\n/m' "$f"
done
```