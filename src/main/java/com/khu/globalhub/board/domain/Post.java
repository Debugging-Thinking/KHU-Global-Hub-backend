package com.khu.globalhub.board.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티. (게시판 1종 — 자유게시판)
 * 실제 텍스트(제목, 내용)는 PostTranslation에 6개 언어 버전으로 저장된다.
 * 이 테이블은 메타 정보(작성자, 익명 여부 등)만 관리한다.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 게시글 작성자 ID. 익명 게시글이라도 DB에는 author_id가 저장된다 (삭제/신고 처리용). identity BC를 ID로만 참조 (D7). */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 익명 게시 여부. true이면 프론트에서 작성자 정보를 숨긴다. */
    @Column(nullable = false)
    private Boolean isAnonymous;

    /**
     * 좋아요 수. PostLike 테이블로 중복 방지를 하고,
     * 좋아요 추가/취소 시 이 값도 함께 업데이트한다.
     * 인기 게시물 기준: likeCount >= 10
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    /** 게시글에 첨부된 이미지 목록. Post 삭제 시 같이 삭제된다 (cascade). */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    /** 6개 언어 번역 버전 목록. 글 작성 시 Azure Translator로 일괄 생성. */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostTranslation> translations = new ArrayList<>();

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void incrementCommentCount() { this.commentCount++; }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }
}
