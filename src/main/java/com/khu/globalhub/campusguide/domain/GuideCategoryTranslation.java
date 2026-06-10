package com.khu.globalhub.campusguide.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * 가이드 카테고리 번역 엔티티 — 퀴즈(QuizQuestionTranslation)와 동일한 사전번역 방식.
 * 카테고리 1개당 최대 6개 행(언어별 1개). 작성/수정 시 원문 행 동기 저장 + 나머지 비동기 번역.
 *
 * BC 격리: 카테고리는 {@code category_id}(Long ID)로만 참조(@ManyToOne 직접참조 금지).
 */
@Entity
@Table(
    name = "guide_category_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideCategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 카테고리의 번역인지 (ID 참조). */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** 이 행이 어떤 언어 버전인지. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 해당 언어로 번역된 카테고리 제목. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    public void updateContent(String title) {
        this.title = title;
    }

    /** 원문 행을 자동 감지된 언어로 재라벨 (작성 시 claimed 언어와 다를 때). */
    public void updateLanguage(Language language) {
        this.language = language;
    }
}
