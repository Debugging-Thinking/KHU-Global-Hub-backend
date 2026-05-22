package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.campusguide.domain.QuizQuestion;

import java.util.List;

public record QuizQuestionResponse(
        Long id,
        String category,
        String question,
        List<String> options
) {
    public static QuizQuestionResponse from(QuizQuestion q) {
        return new QuizQuestionResponse(q.getId(), q.getCategory(), q.getQuestion(), q.getOptions());
    }
}
