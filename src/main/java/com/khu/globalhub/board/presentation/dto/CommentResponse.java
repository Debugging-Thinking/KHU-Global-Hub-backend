package com.khu.globalhub.board.presentation.dto;

import com.khu.globalhub.board.domain.Comment;
import com.khu.globalhub.board.domain.CommentTranslation;
import com.khu.globalhub.shared.enums.Language;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 응답 DTO.
 * 대댓글(children) 포함. content는 요청 언어 번역본, originalContent/originalLanguage는 작성 원문.
 * (댓글마다 원문 언어가 다를 수 있어 항목별 원문/번역 토글을 위해 둘 다 내려준다)
 */
public record CommentResponse(
        Long commentId,
        String content,
        String originalContent,
        Language originalLanguage,
        String imageUrl,
        String authorName,     // isAnonymous=true면 null
        Long authorId,         // isAnonymous=true면 null
        boolean isAnonymous,
        int likeCount,
        boolean isLiked,
        boolean isOwner,
        List<CommentResponse> children,
        LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, CommentTranslation translation,
                                     CommentTranslation original,
                                     String authorName, boolean isLiked, boolean isOwner,
                                     List<CommentResponse> children) {
        return new CommentResponse(
                comment.getId(),
                translation.getContent(),
                original.getContent(),
                original.getLanguage(),
                comment.getImageUrl(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                comment.getIsAnonymous() ? null : comment.getAuthorId(),
                comment.getIsAnonymous(),
                comment.getLikeCount(),
                isLiked,
                isOwner,
                children,
                comment.getCreatedAt()
        );
    }
}
