package com.khu.globalhub.campusguide.presentation.dto;

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
