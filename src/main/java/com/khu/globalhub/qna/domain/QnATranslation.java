package com.khu.globalhub.qna.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * Q&A 질문 번역 엔티티.
 * QnA 1개당 최대 6개 행 (언어별 1개).
 * PostTranslation과 동일한 구조.
 */
@Entity
@Table(
    name = "qna_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"qna_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QnATranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private QnA qna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 번역된 질문 제목. */
    @Column(nullable = false)
    private String title;

    /** 번역된 질문 본문. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
