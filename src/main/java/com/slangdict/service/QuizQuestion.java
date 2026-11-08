package com.slangdict.service;

import java.util.List;
import java.util.Objects;

import com.slangdict.model.SlangWord;

/**
 * Một câu hỏi trắc nghiệm đã được tạo xong.
 *
 * @param prompt       nội dung câu hỏi (F9: slang; F10: các nghĩa nối bằng " | ")
 * @param options      các đáp án, đã xáo trộn (không thể sửa đổi)
 * @param correctIndex vị trí đáp án đúng trong {@code options}
 * @param answer       entry đúng (để UI hiện lời giải)
 */
public record QuizQuestion(String prompt, List<String> options, int correctIndex, SlangWord answer) {

    public QuizQuestion {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(answer, "answer");
        options = List.copyOf(options);
        if (correctIndex < 0 || correctIndex >= options.size()) {
            throw new IllegalArgumentException("correctIndex out of range: " + correctIndex);
        }
    }

    /** @param choice vị trí đáp án người dùng chọn (0-based). */
    public boolean isCorrect(int choice) {
        return choice == correctIndex;
    }

    public String correctOption() {
        return options.get(correctIndex);
    }
}