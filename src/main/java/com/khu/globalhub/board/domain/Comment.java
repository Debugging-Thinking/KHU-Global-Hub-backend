package com.khu.globalhub.board.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 댓글 엔티티 (board BC 전용 — D3/D4로 QnA 댓글 폐기됨).
 *
 * targetId = 댓글이 달린 게시글(Post) ID. (과거 generic 컬럼명 target_id 유지)
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

    /** 댓글이 달린 게시글(Post) ID. (컬럼명은 기존 target_id 유지) */
    @Column(name = "target_id", nullable = false)
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
