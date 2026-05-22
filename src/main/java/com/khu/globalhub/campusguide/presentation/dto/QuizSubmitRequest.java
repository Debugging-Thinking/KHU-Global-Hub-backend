package com.khu.globalhub.campusguide.presentation.dto;

import java.util.List;

public record QuizSubmitRequest(
        List<QuizAnswerItem> answers
) {
    public record QuizAnswerItem(Long questionId, int selectedOption) {}
}
