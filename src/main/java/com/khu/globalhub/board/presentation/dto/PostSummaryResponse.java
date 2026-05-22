package com.khu.globalhub.board.presentation.dto;

import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.board.domain.PostTranslation;

import java.time.LocalDateTime;

/**
 * 게시글 목록에서 사용하는 요약 응답.
 * 본문 내용은 포함하지 않는다.
 */
public record PostSummaryResponse(
        Long postId,
        String title,
        String authorName,     // isAnonymous=true면 null
        boolean isAnonymous,
        int likeCount,
        int commentCount,
        boolean hasImage,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse of(Post post, PostTranslation translation, String authorName) {
        return new PostSummaryResponse(
                post.getId(),
                translation.getTitle(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                post.getIsAnonymous(),
                post.getLikeCount(),
                post.getCommentCount(),
                !post.getImages().isEmpty(),
                post.getCreatedAt()
        );
    }
}
