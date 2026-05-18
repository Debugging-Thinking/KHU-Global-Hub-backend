package com.khu.globalhub.domain.quiz.dto;

import java.util.List;

public record QuizSubmitResponse(
        int correctCount,
        int totalCount,
        double score,
        List<QuizAnswerResult> results
) {
    public record QuizAnswerResult(
            Long questionId,
            boolean correct,
            int correctAnswer,
            String explanation
    ) {}
}
