package com.khu.globalhub.qna.presentation.dto;

import com.khu.globalhub.qna.domain.QnA;
import com.khu.globalhub.qna.domain.QnATranslation;

import java.time.LocalDateTime;

public record QnASummaryResponse(
        Long qnaId,
        String title,
        String authorName,
        boolean isAnonymous,
        boolean isAdopted,
        int likeCount,
        int answerCount,
        LocalDateTime createdAt
) {
    public static QnASummaryResponse of(QnA qna, QnATranslation translation,
                                        String authorName, int answerCount) {
        return new QnASummaryResponse(
                qna.getId(),
                translation.getTitle(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                qna.getIsAnonymous(),
                qna.getIsAdopted(),
                qna.getLikeCount(),
                answerCount,
                qna.getCreatedAt()
        );
    }
}
