package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.campusguide.domain.QuizResult;

import java.time.LocalDateTime;

public record MyQuizResultResponse(
        Long id,
        int correctCount,
        int totalCount,
        double score,
        LocalDateTime completedAt
) {
    public static MyQuizResultResponse from(QuizResult r) {
        return new MyQuizResultResponse(
                r.getId(), r.getCorrectCount(), r.getTotalCount(),
                r.getScore(), r.getCreatedAt()
        );
    }
}
