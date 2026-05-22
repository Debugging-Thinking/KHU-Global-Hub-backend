package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 최상위 댓글 목록 (targetId = postId).
     * parent = null 조건으로 대댓글은 제외 (대댓글은 Comment.children으로 접근).
     */
    List<Comment> findByTargetIdAndParentIsNullOrderByCreatedAtAsc(Long targetId);
}
