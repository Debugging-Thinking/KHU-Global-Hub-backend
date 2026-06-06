package com.khu.globalhub.qna.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Q&A 답변 엔티티.
 * 질문 1개에 여러 답변이 달릴 수 있다.
 * 질문 작성자가 답변 중 하나를 채택(isAdopted=true)하면,
 * 해당 QnA의 isAdopted도 true로 변경하여 더 이상 채택 불가 처리한다.
 */
@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Answer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 답변이 속한 질문. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private QnA qna;

    /** 답변 작성자 ID. identity BC를 ID로만 참조 (D7). */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 익명 작성 여부. */
    @Column(nullable = false)
    private Boolean isAnonymous;

    /**
     * 채택된 답변 여부.
     * 질문 작성자가 이 답변을 채택하면 true로 변경.
     * 채택된 후에는 QnA.isAdopted도 true로 변경해 재채택을 막는다.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdopted = false;

    /** 좋아요 수. AnswerLike 테이블로 중복 방지. */
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /** 첨부 이미지 URL (선택). */
    @Column(name = "image_url")
    private String imageUrl;

    /** 6개 언어 번역 버전. */
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerTranslation> translations = new ArrayList<>();

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    /** 이 답변을 채택 처리. */
    public void adopt() {
        this.isAdopted = true;
    }
}
