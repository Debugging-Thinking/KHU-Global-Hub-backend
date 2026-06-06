package com.khu.globalhub.qna.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * Q&A 답변 번역 엔티티.
 * Answer 1개당 최대 6개 행 (언어별 1개).
 * 답변은 제목 없이 본문만 있어서 content 필드만 존재한다.
 */
@Entity
@Table(
    name = "answer_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"answer_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnswerTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 번역된 답변 본문. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public void updateContent(String content) {
        this.content = content;
    }

    /** 원문 행을 자동 감지된 언어로 재라벨 (작성 시 claimed 언어와 다를 때). */
    public void updateLanguage(Language language) {
        this.language = language;
    }
}
