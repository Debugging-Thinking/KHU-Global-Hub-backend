package com.khu.globalhub.domain.comment.dto;

import com.khu.globalhub.domain.comment.entity.Comment;
import com.khu.globalhub.domain.comment.entity.CommentTranslation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 응답 DTO.
 * 대댓글(children) 포함, 번역된 content 사용.
 */
public record CommentResponse(
        Long commentId,
        String content,
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
                                     String authorName, boolean isLiked, boolean isOwner,
                                     List<CommentResponse> children) {
        return new CommentResponse(
                comment.getId(),
                translation.getContent(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                comment.getIsAnonymous() ? null : comment.getAuthor().getId(),
                comment.getIsAnonymous(),
                comment.getLikeCount(),
                isLiked,
                isOwner,
                children,
                comment.getCreatedAt()
        );
    }
}
