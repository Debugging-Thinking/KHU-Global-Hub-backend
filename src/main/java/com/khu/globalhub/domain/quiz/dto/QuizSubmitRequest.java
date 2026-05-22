package com.khu.globalhub.domain.quiz.dto;

import java.util.List;

public record QuizSubmitRequest(
        List<QuizAnswerItem> answers
) {
    public record QuizAnswerItem(Long questionId, int selectedOption) {}
}
