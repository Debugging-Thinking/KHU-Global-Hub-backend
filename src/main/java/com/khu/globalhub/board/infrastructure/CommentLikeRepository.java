package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);

    void deleteByMemberIdAndCommentId(Long memberId, Long commentId);

    void deleteByCommentIdIn(java.util.Collection<Long> commentIds);
}
