package com.khu.globalhub.campusguide.presentation.dto;

import java.util.List;

/**
 * 관리자 전용 문항 응답 (정답 answerIndex + 해설 explanation 포함).
 * 공개 {@link QuizQuestionResponse}와 달리, 수정 폼 프리필을 위해 정답/해설을 노출한다 (AdminGuard 뒤에서만 호출).
 */
public record AdminQuizQuestionResponse(
        Long id,
        String category,
        String question,
        List<String> options,
        Integer answerIndex,
        String explanation
) {}
