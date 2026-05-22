package com.khu.globalhub.board.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * 댓글 번역 엔티티.
 * Comment 1개당 최대 6개 행 (언어별 1개).
 * 게시글/Q&A 번역과 동일한 방식으로 작성 시점에 6개 언어 버전이 생성된다.
 */
@Entity
@Table(
    name = "comment_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 번역된 댓글 내용. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public void updateContent(String content) {
        this.content = content;
    }
}
