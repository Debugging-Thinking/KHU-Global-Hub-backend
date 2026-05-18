package com.khu.globalhub.domain.quiz.dto;

import com.khu.globalhub.domain.quiz.entity.QuizResult;

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
