package com.khu.globalhub.domain.qna.entity;

import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Q&A 질문 엔티티.
 * 네이버 지식인 방식으로 질문자가 답변 중 하나를 채택할 수 있다.
 * 실제 텍스트(제목, 내용)는 QnATranslation에 6개 언어 버전으로 저장된다.
 */
@Entity
@Table(name = "qnas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QnA extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 질문 작성자. 익명이라도 DB에는 저장됨. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    /** 익명 작성 여부. */
    @Column(nullable = false)
    private Boolean isAnonymous;

    /**
     * 채택 완료 여부.
     * true가 되면 더 이상 채택 불가 (최종 고정).
     * 채택은 질문 작성자만 가능.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdopted = false;

    /** 좋아요 수. QnALike 테이블로 중복 방지. */
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /** 6개 언어 번역 버전. */
    @OneToMany(mappedBy = "qna", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QnATranslation> translations = new ArrayList<>();

    /** 이 질문에 달린 답변 목록. */
    @OneToMany(mappedBy = "qna", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    /** 채택 처리. 한 번 채택하면 isAdopted = true로 고정되어 재채택 불가. */
    public void adopt() {
        this.isAdopted = true;
    }
}
