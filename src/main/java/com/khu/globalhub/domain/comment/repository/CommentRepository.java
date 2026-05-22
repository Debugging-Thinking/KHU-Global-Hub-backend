package com.khu.globalhub.domain.comment.repository;

import com.khu.globalhub.domain.comment.entity.Comment;
import com.khu.globalhub.shared.enums.CommentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 대상(게시글/QnA 등)의 최상위 댓글 목록.
     * parent = null 조건으로 대댓글은 제외 (대댓글은 Comment.children으로 접근).
     */
    List<Comment> findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, Long targetId);
}
