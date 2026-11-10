# L3 + L5 — Ảnh commit và đóng gói zip

Hạn nộp: **23:55 ngày 11/11/2026**. Thay `MSSV` bằng mã số thật ở mọi chỗ.

## L3 — Ảnh chụp lịch sử commit (`docs/commits/`)
Đề yêu cầu nộp kèm ảnh chụp commit. Làm cả hai cách cho chắc:
1. Chụp trang GitHub: `https://github.com/nguyenkhanghy/slang-dictionary/commits/main` (cuộn và chụp từng đoạn nếu dài, thấy rõ ngày, thông điệp, mã commit). Lưu `docs/commits/01.png`, `02.png`, ...
2. Chụp terminal: `git log --pretty=format:'%h  %ad  %s' --date=short` (nếu nhiều commit, chụp nhiều ảnh).
3. Xuất văn bản kèm theo: `mkdir -p docs/commits && git log --pretty=format:'%h  %ad  %an  %s' --date=short > docs/commits/git-log.txt`
4. Đếm số commit để điền vào `self-grading.txt`: `git log --oneline | wc -l`.
Kiểm tra: ảnh phải thấy được ngày và thông điệp; commit ảnh + `git-log.txt` **trước** khi đóng gói (bước dưới dùng `git archive`).

## L5 — Đóng gói `MSSV_SoDiemTuDanhGia.zip`
Cấu trúc gói (gợi ý):
```
MSSV_SoDiemTuDanhGia.zip
├── self-grading.txt                 (đã điền MSSV, điểm, link YouTube)
├── slang-dictionary.jar
├── data/
│   └── slang.original.txt           (CHỈ file này; không current/index/history/100k)
├── commits/                         (ảnh chụp commit + git-log.txt)
└── source/                          (mã nguồn đúng như commit: src, tools, pom.xml, README.md, docs, PROJECT.md...)
```
Lệnh:
```bash
git status                                   # phải sạch: đã commit docs/, ảnh, self-grading.txt
mvn clean package                            # BUILD SUCCESS
rm -rf /tmp/pack && mkdir -p /tmp/pack/source /tmp/pack/data /tmp/pack/commits
git archive HEAD | tar -x -C /tmp/pack/source            # chỉ file đã commit; không có target/, data runtime, .git
cp target/slang-dictionary.jar /tmp/pack/
cp data/slang.original.txt /tmp/pack/data/
cp docs/commits/* /tmp/pack/commits/
cp docs/self-grading.txt /tmp/pack/
cd /tmp/pack && zip -r ~/Desktop/MSSV_SoDiemTuDanhGia.zip . -x '*.DS_Store'
```
Kiểm tra bắt buộc (chính là dòng 12 của `docs/clean-machine-test.md`):
```bash
rm -rf /tmp/unz && mkdir /tmp/unz && cd /tmp/unz && unzip ~/Desktop/MSSV_SoDiemTuDanhGia.zip
java -jar slang-dictionary.jar               # mở được, đủ chức năng
unzip -l ~/Desktop/MSSV_SoDiemTuDanhGia.zip | grep -E "slang\.(current|index|100k)|history|\.tmp"   # phải rỗng
```

## Danh sách "đã đủ chưa" (theo `PROJECT.md` 2.3)
- [ ] Source code  - [ ] File jar  - [ ] File data (`slang.original.txt`)  - [ ] Ảnh chụp commit
- [ ] File txt tự chấm điểm (có MSSV, điểm, link YouTube, % tham khảo)  - [ ] Video YouTube mở được ở cửa sổ ẩn danh
- [ ] README.md ghi cách chạy (`java -jar`, thư mục `data/` cạnh jar và cần quyền ghi); không còn nhắc `--console`
- [ ] Mọi đoạn tham khảo có `// REF:`, `docs/references.md` khớp