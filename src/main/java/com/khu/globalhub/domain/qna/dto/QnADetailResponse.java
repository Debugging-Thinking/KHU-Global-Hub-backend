package com.khu.globalhub.domain.qna.dto;

import com.khu.globalhub.domain.qna.entity.QnA;
import com.khu.globalhub.domain.qna.entity.QnATranslation;

import java.time.LocalDateTime;
import java.util.List;

public record QnADetailResponse(
        Long id,
        String title,
        String content,
        String authorName,
        Long authorId,
        boolean isAnonymous,
        boolean isAdopted,
        int likeCount,
        boolean isLiked,
        boolean isOwner,
        List<AnswerResponse> answers,
        LocalDateTime createdAt
) {
    public static QnADetailResponse of(QnA qna, QnATranslation translation,
                                       String authorName, boolean isLiked, boolean isOwner,
                                       List<AnswerResponse> answers) {
        return new QnADetailResponse(
                qna.getId(),
                translation.getTitle(),
                translation.getContent(),
                qna.getIsAnonymous() ? null : authorName,
                qna.getIsAnonymous() ? null : qna.getAuthor().getId(),
                qna.getIsAnonymous(),
                qna.getIsAdopted(),
                qna.getLikeCount(),
                isLiked,
                isOwner,
                answers,
                qna.getCreatedAt()
        );
    }
}
