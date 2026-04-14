package com.khu.globalhub.domain.qna.dto;

import com.khu.globalhub.domain.qna.entity.Answer;
import com.khu.globalhub.domain.qna.entity.AnswerTranslation;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long answerId,
        String content,
        String authorName,
        Long authorId,
        boolean isAnonymous,
        boolean isAdopted,
        int likeCount,
        boolean isLiked,
        boolean isOwner,
        LocalDateTime createdAt
) {
    public static AnswerResponse of(Answer answer, AnswerTranslation translation,
                                    String authorName, boolean isLiked, boolean isOwner) {
        return new AnswerResponse(
                answer.getId(),
                translation.getContent(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                answer.getIsAnonymous() ? null : answer.getAuthor().getId(),
                answer.getIsAnonymous(),
                answer.getIsAdopted(),
                answer.getLikeCount(),
                isLiked,
                isOwner,
                answer.getCreatedAt()
        );
    }
}
