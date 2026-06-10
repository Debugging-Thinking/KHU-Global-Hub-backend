package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.shared.enums.Language;

import java.util.List;

/**
 * 퀴즈 채점 요청. 채점은 answerIndex(selectedOption) 기준 — 언어와 무관.
 * language는 채점 결과의 해설(explanation)을 어떤 언어로 내려줄지 선택(선택, 기본 KO).
 */
public record QuizSubmitRequest(
        List<QuizAnswerItem> answers,
        Language language
) {
    public record QuizAnswerItem(Long questionId, int selectedOption) {}
}
