package com.khu.globalhub.qna.presentation.dto;

import com.khu.globalhub.qna.domain.Answer;
import com.khu.globalhub.qna.domain.AnswerTranslation;
import com.khu.globalhub.shared.enums.Language;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long answerId,
        String content,
        String originalContent,
        Language originalLanguage,
        String imageUrl,
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
                                    AnswerTranslation original,
                                    String authorName, boolean isLiked, boolean isOwner) {
        return new AnswerResponse(
                answer.getId(),
                translation.getContent(),
                original.getContent(),
                original.getLanguage(),
                answer.getImageUrl(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                answer.getIsAnonymous() ? null : answer.getAuthorId(),
                answer.getIsAnonymous(),
                answer.getIsAdopted(),
                answer.getLikeCount(),
                isLiked,
                isOwner,
                answer.getCreatedAt()
        );
    }
}
