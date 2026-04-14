package com.khu.globalhub.domain.board.dto;

import com.khu.globalhub.domain.board.entity.Post;
import com.khu.globalhub.domain.board.entity.PostTranslation;
import com.khu.globalhub.global.enums.BoardType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 조회 응답.
 * 번역된 제목/본문, 이미지 URL 목록, 좋아요 여부 포함.
 */
public record PostDetailResponse(
        Long postId,
        BoardType boardType,
        String title,
        String content,
        String authorName,     // isAnonymous=true면 null
        Long authorId,         // isAnonymous=true면 null (프로필 이동용)
        boolean isAnonymous,
        int likeCount,
        int commentCount,
        boolean isLiked,       // 현재 로그인한 사용자의 좋아요 여부
        boolean isOwner,       // 작성자 본인 여부 (수정/삭제 버튼 표시용)
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static PostDetailResponse of(Post post, PostTranslation translation,
                                        String authorName, boolean isLiked, boolean isOwner) {
        List<String> imageUrls = post.getImages().stream()
                .map(img -> img.getImageUrl())
                .toList();

        return new PostDetailResponse(
                post.getId(),
                post.getBoardType(),
                translation.getTitle(),
                translation.getContent(),
                authorName,   // 익명이면 서비스가 "익명N" 전달
                post.getIsAnonymous() ? null : post.getAuthor().getId(),
                post.getIsAnonymous(),
                post.getLikeCount(),
                post.getCommentCount(),
                isLiked,
                isOwner,
                imageUrls,
                post.getCreatedAt()
        );
    }
}
