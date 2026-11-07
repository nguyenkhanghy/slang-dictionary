# PROJECT.md — Slang Dictionary (CSC13002 – DAMH#1)

> **Dành cho mọi AI/assistant làm việc trên repo này:** hãy đọc toàn bộ file này trước khi viết code.
> File này là **nguồn sự thật duy nhất** (single source of truth). Nếu cần thay đổi quyết định kiến trúc/rule,
> hãy **đề xuất sửa file này** trước, không tự ý làm khác. Sau mỗi task hoàn thành, cập nhật mục [5. Task Tracker](#5-task-tracker).

---

## 1. Project Overview

- **Môn học:** CSC13002 – Lập trình ứng dụng Java (HCMUS – FIT). Đồ án cá nhân DAMH#1.
- **Mục tiêu:** Ứng dụng **Slang Dictionary** (từ điển tiếng lóng) dùng Java IO, OOP, Generics, Collections.
- **Dữ liệu:** file `slang.txt` (~7.660 dòng), định dạng mỗi dòng `SLANG`MEANING`; nhiều nghĩa của một slang được ngăn bằng `|`.
- **Yêu cầu hiệu năng:** tìm kiếm **< 1 giây** với tới **100.000** slang words.
- **Yêu cầu từ giảng viên chấm lớp này (ưu tiên cao hơn đề gốc nếu mâu thuẫn):**
  1. Dữ liệu có dòng lỗi syntax → **phải xử lý, không được bỏ qua**.
  2. **History lưu ở file riêng**, không ghi vào file dữ liệu gốc.
  3. **Chú trọng giao diện (GUI)**, được sáng tạo.
  4. Dòng lỗi → **sửa tay một lần** trong file data (không gọi API/mạng; chạy hoàn toàn offline).
  5. "Ít nhất 10 commit" chỉ là optional → commit theo từng phần việc hoàn chỉnh, không gò số lượng.
  6. Search phải **nhanh**, chậm bị trừ điểm.
  7. Cấu trúc dự án **dễ quản lý, dễ debug, dễ mở rộng**.

---

## 2. Requirements

### 2.1 Yêu cầu chung
| # | Yêu cầu |
|---|---------|
| R1 | Dùng Java IO, OOP, Collections (có Generics). |
| R2 | Giao diện **Swing** (đã chọn), có menu/thanh điều hướng chọn chức năng. |
| R3 | Load file → đưa vào cấu trúc dữ liệu tối ưu tìm kiếm. |
| R4 | **Lưu lại cấu trúc dữ liệu/index** ra file để lần chạy sau không phải index lại. |
| R5 | Lưu danh sách sau khi add / edit / delete. |
| R6 | Dùng Git; nộp kèm ảnh chụp lịch sử commit. |
| R7 | Nếu chỉ dùng `List` mà không dùng cấu trúc dữ liệu khác → tối đa 70đ. **Phải dùng Map/Set/index.** |

### 2.2 Chức năng (mỗi chức năng 10đ; chậm >1s trừ 50%; UI kém tiện dụng trừ 30%)
| ID | Chức năng |
|----|-----------|
| F1 | Tìm theo slang word. |
| F2 | Tìm theo definition: hiện **tất cả** slang có definition chứa keyword. |
| F3 | Hiển thị History các slang đã tìm. |
| F4 | Add slang mới; nếu trùng → báo + hỏi **Overwrite** hay **Duplicate**. |
| F5 | Edit 1 slang. |
| F6 | Delete 1 slang, **có confirm**. |
| F7 | Reset danh sách về dữ liệu gốc. |
| F8 | Random 1 slang (On this day slang word). |
| F9 | Quiz: hiện 1 slang, 4 đáp án definition. |
| F10 | Quiz: hiện 1 definition, 4 đáp án slang. |

### 2.3 Quy định nộp bài (tóm tắt)
- Tên file nộp: `MSSV_SoDiemTuDanhGia.zip`.
- Gồm: **source code, file jar, file data, ảnh chụp commit, file txt tự chấm điểm từng chức năng + link video YouTube**.
- Video YouTube: mô tả cấu trúc dữ liệu, % code tham khảo, demo toàn bộ 1 chức năng.
- Code tham khảo từ nguồn ngoài **phải đánh dấu** bằng comment `// REF: <nguồn>` và thống kê % tham khảo.
- Tuyệt đối không copy bài người khác.

---

## 3. Architecture (dự kiến)

### 3.1 Tech stack
- **Java 17** (LTS), **Maven** (build + đóng gói jar), **Swing** (+ có thể dùng FlatLaf nếu thầy cho phép thư viện ngoài — xem Task U1).
- IDE: IntelliJ IDEA / VS Code / Eclipse đều được; **không commit file IDE** (xem `.gitignore`).

### 3.2 Cấu trúc thư mục
```
slang-dictionary/
├── PROJECT.md                  # file này
├── README.md                   # hướng dẫn build/chạy cho người ngoài
├── .gitignore
├── pom.xml
├── docs/
│   ├── self-grading.txt        # file tự chấm điểm để nộp
│   ├── commits/                # ảnh chụp commit
│   └── design-notes.md         # ghi chú cấu trúc dữ liệu (dùng khi quay video)
├── data/
│   ├── slang.original.txt      # dữ liệu gốc ĐÃ SỬA LỖI — KHÔNG BAO GIỜ GHI ĐÈ
│   ├── slang.current.txt       # dữ liệu đang dùng (add/edit/delete ghi vào đây)
│   ├── slang.index.bin         # index đã serialize (sinh tự động)
│   └── history.txt             # lịch sử tìm kiếm (file riêng)
├── src/
│   ├── main/java/com/slangdict/
│   │   ├── Main.java
│   │   ├── model/              # SlangWord, HistoryEntry, SearchType
│   │   ├── repository/         # đọc/ghi file: SlangFileReader, SlangFileWriter, DataBootstrap, IndexStore, HistoryStore, AtomicFiles
│   │   ├── index/              # SlangIndex (HashMap + inverted index), IntList
│   │   ├── service/            # DictionaryService (logic: tìm kiếm, history, ...), QuizService (sau)
│   │   ├── ui/                 # MainFrame + panels: SearchPanel, HistoryPanel, ManagePanel, QuizPanel...
│   │   │   └── components/     # component tái sử dụng, theme/style
│   │   ├── dev/                # DevConsole — menu console TẠM, xóa ở Phase 5
│   │   └── util/               # AppPaths, TextNormalizer
│   ├── main/resources/         # icon, font (nếu có)
│   └── test/java/com/slangdict/ # PerfCheck (đúng + nhanh trên 100k); unit test JUnit 5 (tùy chọn, task H2)
├── tools/
│   ├── ValidateData.java       # kiểm tra file dữ liệu
│   └── GenerateBigData.java    # sinh 100.000 slang để test hiệu năng (data/slang.100k.txt, bị ignore)
└── target/                     # (ignore) output của Maven
```

### 3.3 Phân tầng (bắt buộc giữ)
```
ui  →  service  →  index / repository  →  file
```
- **`ui` không được đọc/ghi file trực tiếp**, không chứa logic nghiệp vụ; chỉ gọi `service`.
- **`service`** chứa logic (search, add/edit/delete, quiz, random, reset). Không import `javax.swing`.
- **`repository`** chỉ làm I/O. **`index`** chỉ giữ cấu trúc dữ liệu trong RAM.
- Mọi đường dẫn file lấy qua `util/AppPaths` (không hard-code rải rác).

### 3.4 Cấu trúc dữ liệu (quyết định thiết kế)
Quan sát dữ liệu thật: có **17 slang bị trùng key** (vd `AWS`, `CHEESE`, `GOAT`, `HB`) → một slang có thể có nhiều entry.

| Mục đích | Cấu trúc | Độ phức tạp |
|----------|----------|-------------|
| Lưu toàn bộ | `List<SlangWord>` (giữ thứ tự, id = chỉ số/`long`) | O(1) theo id |
| Tìm theo slang (F1) | `Map<String, List<SlangWord>>` key = slang **chuẩn hóa** (UPPER/trim) | O(1) trung bình |
| Tìm theo definition (F2) | **Inverted index** `Map<String, Set<Integer>>`: token (lowercase) → tập id | O(k) theo kết quả |
| Random (F8, F9, F10) | Truy cập ngẫu nhiên trên `List` | O(1) |
| History (F3) | `LinkedHashSet`/`Deque` + file `history.txt` | O(1) |

- Định nghĩa: tách nghĩa bằng `|` → `List<String> meanings`.
- Tìm definition nhiều từ khóa: tách token, **giao** các tập id; nếu keyword là cụm có dấu cách/ký tự đặc biệt thì lọc lại bằng `contains` trên kết quả đã thu hẹp.
- **Persist index (R4):** ghi `SlangIndex` (cả entries + 2 map) ra `data/slang.index.bin` bằng **định dạng nhị phân tự định nghĩa** (`DataOutputStream`; không dùng Java Serialization vì chậm với 100k object). Header: `MAGIC, VERSION, size, CRC32` của `slang.current.txt`; footer `END_MAGIC`. Khởi động: nếu khớp → nạp index trực tiếp (không đọc/parse txt); nếu lệch/thiếu/hỏng → rebuild từ txt rồi ghi lại index. Dùng CRC32 thay vì ngày sửa file vì ngày sửa bị đổi khi giải nén zip.
- **Tìm theo definition (F2):** khớp **chuỗi con** không phân biệt hoa/thường. Quét từ vựng tìm token chứa keyword → hợp posting list → (keyword nhiều từ/có ký tự đặc biệt) kiểm tra lại bằng `contains`.
- Add/Edit/Delete: cập nhật RAM + ghi `slang.current.txt` + cập nhật `slang.index.bin` (có thể gom ghi khi thoát app và khi đóng cửa sổ, nhưng file txt phải ghi ngay).

### 3.5 Xử lý dữ liệu lỗi (yêu cầu của GV)
Đã khảo sát `slang.txt` (7.662 dòng):
- **Dòng 1** là header `Slag`Meaning` → bỏ khi load.
- **5 dòng "mồ côi"** (không có dấu `` ` ``) là phần nghĩa bị xuống dòng, nằm ngay **sau** dòng của slang tương ứng: dòng 413 (AMA), 908 (BOMB), 1112 (C4C), 2004 (ER), 2882 (HOODIE). Cách xử lý: **gộp vào dòng liền trước** thành `...`nghĩa 1|nghĩa 2`.
- Một số slang chứa ký tự đặc biệt/HTML entity (`&APOS;S`, `&HEARTS;`) — **giữ nguyên**, chỉ hiển thị đúng.
- **Quy tắc parse (bắt buộc):** tách dòng tại dấu `` ` `` **đầu tiên** → vế trái là slang, vế phải là meaning; **chỉ** vế meaning mới tách nghĩa bằng `|`.
  Lý do: có 5 slang chứa chính ký tự `|` (`8-|`, `8|`, `:(|)`, `:|`, `|O|`) — **không được split cả dòng theo `|`**.
- **Dấu `/` KHÔNG phải dấu ngăn nghĩa.** `CINO`Catholic/Christian In Name Only` là **một** nghĩa có cách nói thay thế
  (tương tự `BOO`Boyfriend/Girlfriend`, ~65 dòng). Không sửa, không tách.
- **Khoảng trắng thừa:** ~23 dòng có khoảng trắng thừa ở cuối meaning (`Cool `) và vài slang có dấu cách đầu/cuối (` HK`, `MBFAM `, `HD `).
  Reader phải `strip()` slang và từng meaning (và slang key **chuẩn hóa UPPER + strip** khi tạo index).
- **Dòng trùng y hệt:** ~14 dòng trùng hoàn toàn (vd `AWS`As We Speak` ×2, `NAE`No` ×2). Khi làm sạch nên **loại dòng trùng y hệt**
  (sau khi strip); các slang trùng key nhưng khác nghĩa thì **giữ cả hai**.
- **Lỗi encoding 2 dòng cuối (7661–7662):** `Æ`Registered Trademark Symbol` và `ô`Trademark` là ký tự `®` và `™` bị đọc sai bảng mã. Sửa tay thành `®` và `™`.
- File phải đọc/ghi bằng **UTF-8** tường minh.
- **Kiểm tra tự động:** chạy `java tools/ValidateData.java data/slang.original.txt` — kết quả mong muốn sau khi sửa: **0 ERROR**
  (WARN về khoảng trắng/trùng nên về 0 sau khi làm sạch; `key lặp` là bình thường).
- **Cách sửa:** sửa tay **một lần** để tạo `data/slang.original.txt` sạch. Đồng thời `SlangFileReader` vẫn phải **phòng thủ** (dòng thiếu `` ` `` → nối vào dòng trước, log cảnh báo, không crash) để file người dùng tự sửa vẫn load được.
- **Không** dùng API/mạng ở bất kỳ đâu. Ứng dụng chạy hoàn toàn offline.

### 3.6 Luồng file khi chạy
1. Lần đầu: nếu chưa có `slang.current.txt` → copy từ `slang.original.txt`.
2. Load index (hoặc rebuild) → mở UI.
3. F7 Reset: copy lại `original → current`, rebuild + ghi index (history giữ nguyên, trừ khi quyết định khác được ghi ở mục Decisions).
4. **`slang.original.txt` chỉ đọc.**

---

## 4. Roadmap

| Phase | Nội dung | Kết quả |
|-------|----------|---------|
| **0. Setup** | Tạo repo GitHub, Maven project, cấu trúc thư mục, `.gitignore`, README, đặt PROJECT.md | Project chạy được `Hello` |
| **1. Data** | Làm sạch `slang.txt` → `slang.original.txt`; model `SlangWord`; reader/writer | Load đúng ~7.660 entry |
| **2. Core** | `SlangIndex`, persist index, `DictionaryService`: F1, F2, History (F3) | Search < 1s trên 100k |
| **3. CRUD** | F4 Add (overwrite/duplicate), F5 Edit, F6 Delete, F7 Reset + lưu file | Dữ liệu bền vững |
| **4. Fun** | F8 Random, F9, F10 Quiz | Đủ 10 chức năng |
| **5. GUI** | Layout, theme, UX polish, phím tắt, thông báo | Giao diện đẹp, tiện dụng |
| **6. Hardening** | Test 100k, xử lý ngoại lệ, đo thời gian, dọn code | Ổn định |
| **7. Delivery** | Build jar, tự chấm điểm, ảnh commit, quay video, đóng zip | Nộp bài |

---

## 5. Task Tracker

**Ký hiệu trạng thái:** `[ ]` chưa làm · `[~]` đang làm · `[x]` xong · `[-]` bỏ/hoãn
**Ưu tiên:** **M** = bắt buộc · **O** = tùy chọn (làm nếu còn thời gian)

> Quy tắc cập nhật: chỉ đổi trạng thái khi task **thực sự** đạt "Definition of Done" (mục 6.6). Ghi chú ngắn vào cột Notes (ngày, vấn đề còn tồn).

### Phase 0 — Setup
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| S1 | Tạo repo GitHub + clone về máy | M | `[x]` | Repo: github.com/nguyenkhanghy/slang-dictionary |
| S2 | Khởi tạo Maven project (Java 17), cấu trúc package | M | `[x]` | `pom.xml` + khung package đã có; chạy `mvn clean package` thành công |
| S3 | `.gitignore`, `README.md`, thêm `PROJECT.md` | M | `[x]` | `.gitignore`, `README.md`, `PROJECT.md` đã có |
| S4 | Cấu hình đóng gói jar chạy được (`java -jar`) | M | `[x]` | Chạy `java -jar target/slang-dictionary.jar` thành công |

### Phase 1 — Data
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| D1 | Làm sạch dữ liệu → `slang.original.txt` (5 dòng mồ côi, strip khoảng trắng, bỏ dòng trùng y hệt, sửa `Æ`→`®`, `ô`→`™`, bỏ header) | M | `[x]` | Đã commit; xác nhận bằng `ValidateData` (0 ERROR) |
| D2 | `SlangWord` model (slang, meanings, Serializable) | M | `[x]` | `model/SlangWord`: immutable, tự kiểm tra ràng buộc định dạng |
| D3 | `SlangFileReader` (bỏ header, phòng thủ dòng lỗi, log) | M | `[x]` | Test trên file thô: gộp 5 dòng mồ côi, bỏ header/BOM, strip; KHÔNG loại dòng trùng |
| D4 | `SlangFileWriter` (ghi `slang.current.txt`, UTF-8) | M | `[x]` | Ghi qua file tạm + move (an toàn khi crash); nối nghĩa bằng "`| `" |
| D5 | `AppPaths` + logic khởi tạo file lần đầu | M | `[x]` | `util/AppPaths` (property → ./data → cạnh jar) + `repository/DataBootstrap` |
| D6 | Tool sinh 100.000 slang để test | M | `[x]` | `tools/GenerateBigData.java`; đã thử 100.000 entry |
| D7 | `tools/ValidateData.java` kiểm tra file dữ liệu | M | `[x]` | Đã có; chạy lại sau mỗi lần sửa data |

### Phase 2 — Core search
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| C1 | `SlangIndex`: map slang → list entry | M | `[x]` | `index/SlangIndex.bySlang`: `Map<String, IntList>`, key = slang IN HOA |
| C2 | Inverted index cho definition | M | `[x]` | `SlangIndex.byToken`: inverted index token → posting list (`IntList`) |
| C3 | Persist/load index (`slang.index.bin`) + kiểm tra hợp lệ | M | `[x]` | `repository/IndexStore`: file nhị phân + kiểm tra (kích thước, CRC32); hỏng/lệch → build lại, không crash |
| C4 | **F1** Tìm theo slang | M | `[x]` | `DictionaryService.searchBySlang` — khớp chính xác, không phân biệt hoa/thường, trả mọi entry trùng key |
| C5 | **F2** Tìm theo definition | M | `[x]` | `DictionaryService.searchByDefinition` — khớp chuỗi con ("lov" ra "love"), đã so với duyệt tuần tự |
| C6 | **F3** History (file `history.txt` riêng) | M | `[x]` | `HistoryStore` + `HistoryEntry`; ghi cả F1 và F2; file riêng `data/history.txt` |
| C7 | Test hiệu năng 100k (< 1s, in log thời gian) | M | `[~]` | `src/test/.../PerfCheck` đã chạy PASS trên máy dev (truy vấn ≤ vài ms); chờ chạy trên máy thật → đổi `[x]` |
| C8 | Gợi ý/autocomplete khi gõ (Trie hoặc `TreeMap`) | O | `[ ]` | |

### Phase 3 — CRUD
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| E1 | **F4** Add + xử lý trùng (Overwrite / Duplicate) | M | `[ ]` | |
| E2 | **F5** Edit | M | `[ ]` | |
| E3 | **F6** Delete + confirm | M | `[ ]` | |
| E4 | **F7** Reset về original | M | `[ ]` | |
| E5 | Validate input (rỗng, ký tự `` ` `` và xuống dòng bị cấm trong slang/meaning) | M | `[ ]` | |
| E6 | Undo thao tác xóa gần nhất | O | `[ ]` | |

### Phase 4 — Fun
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| Q1 | **F8** Random slang of the day | M | `[ ]` | |
| Q2 | **F9** Quiz: slang → 4 definition | M | `[ ]` | |
| Q3 | **F10** Quiz: definition → 4 slang | M | `[ ]` | |
| Q4 | Điểm số, streak, kết quả cuối quiz | O | `[ ]` | |
| Q5 | Slang of the day cố định theo ngày (seed = ngày) | O | `[ ]` | |

### Phase 5 — GUI
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| U1 | Quyết định look&feel (Nimbus / FlatLaf) | M | `[ ]` | Hỏi GV nếu dùng thư viện ngoài |
| U2 | `MainFrame` + sidebar/tab điều hướng 10 chức năng | M | `[ ]` | |
| U3 | Màn Search (slang + definition, kết quả dạng bảng/card) | M | `[ ]` | |
| U4 | Màn History, Manage (Add/Edit/Delete), Quiz, Random | M | `[ ]` | |
| U5 | Dialog confirm/thông báo thống nhất | M | `[ ]` | |
| U6 | Theme (màu, font, icon), spacing nhất quán | M | `[ ]` | |
| U7 | Dark mode / đổi theme | O | `[ ]` | |
| U8 | Phím tắt, Enter để tìm, focus hợp lý | O | `[ ]` | |
| U9 | Animation nhẹ, highlight keyword trong kết quả | O | `[ ]` | |

### Phase 6–7 — Hardening & Delivery
| ID | Task | Ưu tiên | Trạng thái | Notes |
|----|------|:------:|:---------:|-------|
| H1 | Xử lý ngoại lệ toàn cục (file thiếu/hỏng, index hỏng) — không crash | M | `[ ]` | |
| H2 | Unit test các service quan trọng | O | `[ ]` | |
| H3 | Dọn code, javadoc/comment tại phần quan trọng | M | `[ ]` | |
| L1 | Build jar + test chạy trên máy sạch (kèm thư mục `data/`) | M | `[ ]` | |
| L2 | Điền `docs/self-grading.txt` (điểm từng chức năng + link YouTube) | M | `[ ]` | |
| L3 | Chụp ảnh commit vào `docs/commits/` | M | `[ ]` | |
| L4 | Quay video YouTube (cấu trúc dữ liệu, % tham khảo, demo 1 chức năng) | M | `[ ]` | |
| L5 | Đóng gói `MSSV_SoDiemTuDanhGia.zip` | M | `[ ]` | |

---

## 6. Rules (bắt buộc cho mọi AI & con người)

### 6.1 Quy tắc chung
1. **Đọc PROJECT.md trước khi làm.** Làm đúng phạm vi task được giao, không "tiện tay" refactor/đổi kiến trúc ngoài task.
2. **Không thêm dependency** (thư viện ngoài) khi chưa được ghi trong mục Decisions. Mặc định chỉ dùng JDK (+ JUnit cho test).
3. **Không gọi mạng/API** ở runtime. Ứng dụng chạy offline hoàn toàn.
4. **Không hard-code đường dẫn tuyệt đối**; dùng `AppPaths`.
5. Khi không chắc → **hỏi/ghi giả định** thay vì tự đoán rồi làm lệch thiết kế.

### 6.2 Code style
- Java 17, indent 4 spaces, UTF-8, LF.
- Tên: `PascalCase` class, `camelCase` method/biến, `UPPER_SNAKE_CASE` hằng. Package `com.slangdict.*` toàn bộ chữ thường.
- Mỗi class **một trách nhiệm**; method ngắn (≈ ≤ 40 dòng); tránh class "God".
- Dùng **Generics/Collections đúng kiểu** (khai báo bằng interface: `Map<..>`, `List<..>`), tránh raw types.
- Comment tiếng Việt hoặc Anh đều được nhưng **thống nhất trong 1 file**; Javadoc cho class/method public quan trọng.
- Không để code chết/`System.out.println` debug trong bản commit (dùng `java.util.logging`).

### 6.3 Quy tắc dữ liệu & I/O
- Định dạng file: `SLANG`MEANING1|MEANING2` mỗi entry **một dòng**, UTF-8.
- **`data/slang.original.txt` read-only** — không code nào được ghi vào.
- Add/Edit/Delete chỉ ghi `slang.current.txt`. History chỉ ghi `history.txt`.
- Mọi I/O dùng try-with-resources, `BufferedReader/BufferedWriter`, charset UTF-8 tường minh.
- Ghi file an toàn: ghi ra file tạm rồi `Files.move(..., REPLACE_EXISTING)` để tránh hỏng file khi crash.
- Index: luôn kiểm tra hợp lệ khi load; hỏng → rebuild, **không crash**.

### 6.4 Quy tắc GUI
- Swing: mọi thao tác UI trên **EDT** (`SwingUtilities.invokeLater`); tác vụ nặng chạy `SwingWorker`, không chặn UI.
- UI **không** chứa logic nghiệp vụ, không đụng file.
- Một nơi duy nhất định nghĩa màu/font/spacing (`ui/components/Theme`). Không hard-code màu rải rác.
- Mọi hành động phá hủy (delete, reset, overwrite) **phải confirm**.
- Thông báo lỗi thân thiện, không hiện stack trace cho người dùng.

### 6.5 Quy tắc Git
- Nhánh chính: `main` luôn build được. Làm việc trên nhánh `feature/<mô-tả-ngắn>` rồi merge (hoặc commit thẳng `main` nếu thay đổi nhỏ).
- **Commit nhỏ, theo từng phần việc hoàn chỉnh**, trải đều các ngày (không dồn cuối deadline).
- Message theo dạng: `<type>: <mô tả ngắn>`; type ∈ `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`.
  Ví dụ: `feat: add slang index with inverted definition lookup`.
- Không commit: `target/`, file IDE (`.idea/`, `.vscode/`, `*.iml`), `slang.index.bin` (tùy — xem Decisions), file tạm.
- Mỗi commit nên build/chạy được.

### 6.6 Definition of Done (một task được đánh `[x]` khi)
- [ ] Code biên dịch, chạy đúng với dữ liệu thật.
- [ ] Đúng layer/kiến trúc ở mục 3.
- [ ] Xử lý ngoại lệ & trường hợp biên (rỗng, không tìm thấy, trùng...).
- [ ] Với chức năng tìm kiếm: đã đo thời gian trên 100k, < 1s.
- [ ] Đã cập nhật Task Tracker.
- [ ] Đã commit.

### 6.7 Đánh dấu code tham khảo
- Mọi đoạn lấy từ internet/AI/người khác phải có comment: `// REF: <nguồn/URL hoặc "AI-generated (tên công cụ)">`.
- Giữ danh sách trong `docs/references.md` để tính **% tham khảo**.

### 6.8 Quy tắc dành riêng cho AI assistant
- Khi trả code: ghi rõ **đường dẫn file đầy đủ**, và đưa **toàn bộ file** nếu sửa nhiều chỗ (hoặc diff rõ ràng).
- Cuối mỗi phiên làm việc: tóm tắt **đã làm gì / task ID nào đổi trạng thái / còn gì dở / bước tiếp theo**.
- Không đổi tên package/class/file đã có nếu không được yêu cầu.
- Nếu phát hiện PROJECT.md sai/lỗi thời → nói rõ và đề xuất bản sửa.

---

## 7. Decisions Log (ghi các quyết định đã chốt)

| Ngày | Quyết định | Lý do |
|------|-----------|-------|
| — | GUI dùng **Swing** | GV chú trọng giao diện, Swing đủ linh hoạt, có sẵn trong JDK |
| — | Build bằng **Maven**, Java 17 | Dễ đóng gói jar, dễ quản lý |
| — | `slang` key không duy nhất → `Map<String, List<SlangWord>>` | Dữ liệu có 17 key trùng |
| — | History ở file riêng `data/history.txt` | Yêu cầu GV |
| — | Dòng lỗi sửa tay 1 lần, không dùng API | Yêu cầu GV (chạy offline) |
| 2026-11-07 | Tìm thư mục `data/`: `-Dslang.dataDir` → `./data` (nếu có file gốc) → `data/` cạnh jar | Chạy được cả từ IDE/repo lẫn khi giảng viên chạy `java -jar` |
| 2026-11-07 | Reader **không** loại dòng trùng; writer nối nghĩa bằng "`\| `" | F4 cho phép duplicate; khớp định dạng dữ liệu gốc |
| 2026-11-07 | Đo thực tế: parse text 100.000 entry ≈ 0,4–0,5s | Chỉ riêng load text đã gần ngưỡng 1s → **bắt buộc** persist index (task C3) |
| 2026-11-07 | Index lưu bằng định dạng nhị phân tự viết; hợp lệ khi (size, CRC32) của `slang.current.txt` khớp | Nhanh hơn Java Serialization; không bị vô hiệu khi giải nén zip |
| 2026-11-07 | F1 khớp chính xác (không phân biệt hoa/thường); F2 khớp chuỗi con | Đúng chữ "chứa keyword" trong đề; vẫn nhanh nhờ inverted index |

| 2026-11-07 | Đo (100k, JVM nguội): đọc txt ~0,5s, build index ~0,5s, lưu ~0,3s, nạp cache ~0,3–0,5s; truy vấn F1/F2 ≤ vài ms | Build lại toàn bộ ≈ 1s → **Phase 3: add/edit/delete phải cập nhật index tăng dần**, ghi txt ngay, ghi index khi thoát/gom lại |
| _TBD_ | Có commit `slang.index.bin` hay không? | Cần chốt (khuyến nghị: **không commit**, app tự sinh; nhưng **nộp kèm** trong zip) |
| _TBD_ | Reset có xóa history không? | Cần chốt (khuyến nghị: giữ history) |
| _TBD_ | Có dùng FlatLaf không? | Hỏi GV về thư viện ngoài |
