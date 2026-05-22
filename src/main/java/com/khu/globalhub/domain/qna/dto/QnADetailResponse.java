package com.khu.globalhub.domain.qna.dto;

import com.khu.globalhub.domain.qna.entity.QnA;
import com.khu.globalhub.domain.qna.entity.QnATranslation;
import com.khu.globalhub.shared.enums.Language;

import java.time.LocalDateTime;
import java.util.List;

public record QnADetailResponse(
        Long qnaId,
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
        LocalDateTime createdAt,
        Language originalLanguage  // Q&A 원문 언어
) {
    public static QnADetailResponse of(QnA qna, QnATranslation translation,
                                       String authorName, boolean isLiked, boolean isOwner,
                                       List<AnswerResponse> answers, Language originalLanguage) {
        return new QnADetailResponse(
                qna.getId(),
                translation.getTitle(),
                translation.getContent(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                qna.getIsAnonymous() ? null : qna.getAuthor().getId(),
                qna.getIsAnonymous(),
                qna.getIsAdopted(),
                qna.getLikeCount(),
                isLiked,
                isOwner,
                answers,
                qna.getCreatedAt(),
                originalLanguage
        );
    }
}
