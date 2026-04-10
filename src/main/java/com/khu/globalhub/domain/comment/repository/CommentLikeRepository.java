package com.khu.globalhub.domain.comment.repository;

import com.khu.globalhub.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);

    void deleteByMemberIdAndCommentId(Long memberId, Long commentId);
}
