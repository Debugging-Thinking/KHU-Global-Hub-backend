package com.khu.globalhub.board.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import com.khu.globalhub.shared.enums.CommentTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 통합 댓글 엔티티.
 * 게시글(POST), Q&A 질문(QNA), Q&A 답변(ANSWER)의 댓글을 하나의 테이블로 관리한다.
 *
 * 어느 게시물의 댓글인지는 targetType + targetId 조합으로 식별한다.
 * 예시: targetType=POST, targetId=5 → 게시글 id=5의 댓글
 *
 * 대댓글은 parent_id를 통해 구현한다.
 * parent_id = NULL    → 일반 댓글
 * parent_id = 댓글 ID → 해당 댓글의 답글(대댓글)
 */
@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 댓글이 달린 대상의 종류.
     * POST / QNA / ANSWER 중 하나.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentTargetType targetType;

    /**
     * 댓글이 달린 대상의 ID.
     * targetType=POST 이면 Post.id, targetType=QNA 이면 QnA.id, 등.
     */
    @Column(nullable = false)
    private Long targetId;

    /**
     * 부모 댓글. 대댓글인 경우에만 값이 있고, 일반 댓글은 NULL.
     * 셀프 참조(같은 테이블의 다른 행을 참조).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** 이 댓글의 답글(대댓글) 목록. */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> children = new ArrayList<>();

    /** 댓글 작성자 ID. identity BC를 ID로만 참조 (D7). */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 익명 작성 여부. */
    @Column(nullable = false)
    private Boolean isAnonymous;

    /** 좋아요 수. CommentLike 테이블로 중복 방지. */
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /** 6개 언어 번역 버전. */
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentTranslation> translations = new ArrayList<>();

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}
