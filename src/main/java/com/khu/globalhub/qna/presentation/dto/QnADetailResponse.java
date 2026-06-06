package com.khu.globalhub.qna.presentation.dto;

import com.khu.globalhub.qna.domain.QnA;
import com.khu.globalhub.qna.domain.QnATranslation;
import com.khu.globalhub.shared.enums.Language;

import java.time.LocalDateTime;
import java.util.List;

public record QnADetailResponse(
        Long qnaId,
        String title,
        String content,
        String originalTitle,
        String originalContent,
        String imageUrl,
        String authorName,
        Long authorId,
        boolean isAnonymous,
        boolean isAdopted,
        int likeCount,
        boolean isLiked,
        boolean isOwner,
        List<AnswerResponse> answers,
        LocalDateTime createdAt,
        Language originalLanguage  // Q&A 원문 언어
) {
    public static QnADetailResponse of(QnA qna, QnATranslation translation,
                                       QnATranslation original,
                                       String authorName, boolean isLiked, boolean isOwner,
                                       List<AnswerResponse> answers) {
        return new QnADetailResponse(
                qna.getId(),
                translation.getTitle(),
                translation.getContent(),
                original.getTitle(),
                original.getContent(),
                qna.getImageUrl(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                qna.getIsAnonymous() ? null : qna.getAuthorId(),
                qna.getIsAnonymous(),
                qna.getIsAdopted(),
                qna.getLikeCount(),
                isLiked,
                isOwner,
                answers,
                qna.getCreatedAt(),
                original.getLanguage()
        );
    }
}
