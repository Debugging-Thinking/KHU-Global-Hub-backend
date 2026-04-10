package com.khu.globalhub.domain.qna.dto;

import com.khu.globalhub.domain.qna.entity.QnA;
import com.khu.globalhub.domain.qna.entity.QnATranslation;

import java.time.LocalDateTime;

public record QnASummaryResponse(
        Long id,
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
                qna.getIsAnonymous() ? null : authorName,
                qna.getIsAnonymous(),
                qna.getIsAdopted(),
                qna.getLikeCount(),
                answerCount,
                qna.getCreatedAt()
        );
    }
}
