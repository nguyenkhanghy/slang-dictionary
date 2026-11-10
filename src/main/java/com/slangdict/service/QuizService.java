package com.slangdict.service;

import com.slangdict.model.SlangWord;
import com.slangdict.util.TextNormalizer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * F8 (từ lóng của ngày + từ lóng ngẫu nhiên) và F9/F10 (quiz 4 đáp án). Tầng service: không import Swing, không in ra console, không ghi file.
 *
 * <p>Luôn đọc dữ liệu hiện tại từ {@link DictionaryService#getAllWords()} (không cache), nên add/edit/delete/reset
 * giữa các câu hỏi có hiệu lực ngay. {@link Random} được truyền vào để test có seed. Không thread-safe.
 *
 * <p>Tránh câu hỏi có nhiều đáp án đúng (dữ liệu thật có nhiều slang khác nhau cùng nghĩa và một slang có nhiều
 * entry). Đáp án nhiễu phải:
 * <ul>
 *   <li>thuộc entry có slang KHÁC slang của đáp án đúng;</li>
 *   <li>không chia sẻ bất kỳ nghĩa nào (so sánh không phân biệt hoa/thường) với các nghĩa bị cấm:
 *       F9: nghĩa của MỌI entry cùng slang với đáp án; F10: nghĩa của đáp án
 *       (kiểm tra cả mọi entry của slang nhiễu, vì slang đó có thể có entry khác trùng nghĩa);</li>
 *   <li>khác nhau đôi một về văn bản đáp án (không phân biệt hoa/thường).</li>
 * </ul>
 * Không đủ dữ liệu để có 4 đáp án như vậy → {@link Optional#empty()}.
 */
public final class QuizService {

    public static final int OPTION_COUNT = 4;
    /** Nghĩa của một entry được nối bằng chuỗi này khi hiển thị làm câu hỏi/đáp án (quyết định Q2/Q3). */
    public static final String MEANING_JOINER = " | ";

    private static final int MAX_ANSWER_ATTEMPTS = 30;
    private static final int SAMPLE_TRIES = 60;
    /** Dữ liệu nhỏ hơn mức này được duyệt vét cạn để kết quả rỗng nghĩa là THỰC SỰ không thể tạo câu hỏi. */
    private static final int EXHAUSTIVE_LIMIT = 200;

    private enum Kind { SLANG_TO_MEANING, MEANING_TO_SLANG }

    private final DictionaryService dictionary;
    private final Random random;

    public QuizService(DictionaryService dictionary) {
        this(dictionary, new Random());
    }

    public QuizService(DictionaryService dictionary, Random random) {
        this.dictionary = dictionary;
        this.random = random;
    }

    /** Các nghĩa của entry nối bằng {@link #MEANING_JOINER} (dạng hiển thị chuẩn, UI cũng nên dùng). */
    public static String meaningText(SlangWord word) {
        return String.join(MEANING_JOINER, word.getMeanings());
    }

    /** F8: một slang ngẫu nhiên (mỗi lần gọi khác nhau). Rỗng nếu chưa có dữ liệu. */
    public Optional<SlangWord> randomSlang() {
        List<SlangWord> all = dictionary.getAllWords();
        if (all.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(all.get(random.nextInt(all.size())));
    }

    /**
     * F8 "On this day": từ lóng của ngày {@code date}. Cùng ngày + cùng dữ liệu thì luôn ra cùng một từ (không phụ thuộc
     * {@link Random} được truyền vào, nên khởi động lại hay tạo {@code QuizService} khác vẫn như nhau). Nếu dữ liệu thêm/xóa
     * trong ngày (số entry đổi) thì từ của ngày có thể đổi theo. Rỗng nếu chưa có dữ liệu.
     */
    public Optional<SlangWord> slangOfTheDay(LocalDate date) {
        List<SlangWord> all = dictionary.getAllWords();
        if (all.isEmpty()) {
            return Optional.empty();
        }
        long seed = date.toEpochDay() * 0x9E3779B97F4A7C15L; // nhân hằng số để các ngày liên tiếp không cho kết quả gần nhau
        return Optional.of(all.get(new Random(seed).nextInt(all.size())));
    }

    /** F9: hiển thị 1 slang, 4 đáp án là nghĩa. */
    public Optional<QuizQuestion> nextSlangToMeaningQuestion() {
        return nextQuestion(Kind.SLANG_TO_MEANING);
    }

    /** F10: hiển thị các nghĩa của 1 slang, 4 đáp án là slang. */
    public Optional<QuizQuestion> nextMeaningToSlangQuestion() {
        return nextQuestion(Kind.MEANING_TO_SLANG);
    }

    // ------------------------------------------------------------------ question building

    private Optional<QuizQuestion> nextQuestion(Kind kind) {
        List<SlangWord> all = dictionary.getAllWords();
        int n = all.size();
        if (n < OPTION_COUNT) {
            return Optional.empty();
        }
        for (int attempt = 0; attempt < MAX_ANSWER_ATTEMPTS; attempt++) {
            Optional<QuizQuestion> question = tryBuild(all, all.get(random.nextInt(n)), kind);
            if (question.isPresent()) {
                return question;
            }
        }
        if (n <= EXHAUSTIVE_LIMIT) {
            int start = random.nextInt(n);
            for (int i = 0; i < n; i++) {
                Optional<QuizQuestion> question = tryBuild(all, all.get((start + i) % n), kind);
                if (question.isPresent()) {
                    return question;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<QuizQuestion> tryBuild(List<SlangWord> all, SlangWord answer, Kind kind) {
        List<SlangWord> distractors = pickDistractors(all, answer, kind);
        if (distractors == null) {
            return Optional.empty();
        }
        String correct = optionText(answer, kind);
        List<String> options = new ArrayList<>(OPTION_COUNT);
        options.add(correct);
        for (SlangWord d : distractors) {
            options.add(optionText(d, kind));
        }
        Collections.shuffle(options, random);
        String prompt = kind == Kind.SLANG_TO_MEANING ? answer.getSlang() : meaningText(answer);
        return Optional.of(new QuizQuestion(prompt, options, options.indexOf(correct), answer));
    }

    /** Chọn OPTION_COUNT-1 entry nhiễu hợp lệ: thử ngẫu nhiên trước, rồi duyệt tuần tự từ vị trí ngẫu nhiên. */
    private List<SlangWord> pickDistractors(List<SlangWord> all, SlangWord answer, Kind kind) {
        int n = all.size();
        String answerKey = TextNormalizer.normalizeSlang(answer.getSlang());
        Set<String> forbidden = forbiddenMeanings(answer, kind);
        Set<String> usedOptions = new HashSet<>();
        usedOptions.add(optionKey(answer, kind));
        List<SlangWord> chosen = new ArrayList<>(OPTION_COUNT - 1);

        for (int t = 0; t < SAMPLE_TRIES && chosen.size() < OPTION_COUNT - 1; t++) {
            tryAdd(all.get(random.nextInt(n)), answerKey, forbidden, usedOptions, chosen, kind);
        }
        if (chosen.size() < OPTION_COUNT - 1) {
            int start = random.nextInt(n);
            for (int i = 0; i < n && chosen.size() < OPTION_COUNT - 1; i++) {
                tryAdd(all.get((start + i) % n), answerKey, forbidden, usedOptions, chosen, kind);
            }
        }
        return chosen.size() == OPTION_COUNT - 1 ? chosen : null;
    }

    private void tryAdd(SlangWord candidate, String answerKey, Set<String> forbidden,
                        Set<String> usedOptions, List<SlangWord> chosen, Kind kind) {
        if (TextNormalizer.normalizeSlang(candidate.getSlang()).equals(answerKey)) {
            return;
        }
        String key = optionKey(candidate, kind);
        if (usedOptions.contains(key) || !compatible(candidate, forbidden, kind)) {
            return;
        }
        usedOptions.add(key);
        chosen.add(candidate);
    }

    /** F9: nghĩa của entry nhiễu không được trùng nghĩa nào bị cấm. F10: mọi entry của slang nhiễu đều không được trùng. */
    private boolean compatible(SlangWord candidate, Set<String> forbidden, Kind kind) {
        if (kind == Kind.SLANG_TO_MEANING) {
            return !sharesMeaning(candidate, forbidden);
        }
        for (SlangWord sameKey : dictionary.lookupSlang(candidate.getSlang())) {
            if (sharesMeaning(sameKey, forbidden)) {
                return false;
            }
        }
        return !sharesMeaning(candidate, forbidden);
    }

    private Set<String> forbiddenMeanings(SlangWord answer, Kind kind) {
        Set<String> forbidden = new HashSet<>();
        addNormalized(answer.getMeanings(), forbidden);
        if (kind == Kind.SLANG_TO_MEANING) {
            for (SlangWord sameKey : dictionary.lookupSlang(answer.getSlang())) {
                addNormalized(sameKey.getMeanings(), forbidden);
            }
        }
        return forbidden;
    }

    private static boolean sharesMeaning(SlangWord word, Set<String> forbidden) {
        for (String meaning : word.getMeanings()) {
            if (forbidden.contains(TextNormalizer.normalizeText(meaning))) {
                return true;
            }
        }
        return false;
    }

    private static void addNormalized(Collection<String> meanings, Set<String> into) {
        for (String meaning : meanings) {
            into.add(TextNormalizer.normalizeText(meaning));
        }
    }

    private static String optionText(SlangWord word, Kind kind) {
        return kind == Kind.SLANG_TO_MEANING ? meaningText(word) : word.getSlang();
    }

    /** Khóa so sánh "hai đáp án có giống nhau không" (không phân biệt hoa/thường). */
    private static String optionKey(SlangWord word, Kind kind) {
        return kind == Kind.SLANG_TO_MEANING
                ? TextNormalizer.normalizeText(meaningText(word))
                : TextNormalizer.normalizeSlang(word.getSlang());
    }
}