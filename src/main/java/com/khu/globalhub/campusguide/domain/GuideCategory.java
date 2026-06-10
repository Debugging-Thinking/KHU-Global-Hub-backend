package com.khu.globalhub.campusguide.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 학사 가이드 카테고리 메타데이터.
 * 퀴즈(QuizQuestion)와 동일하게 다국어 텍스트(title)는 {@link GuideCategoryTranslation}으로 분리하고,
 * 이 엔티티는 언어 무관 메타(뱃지 키, 이모지, 색상, 정렬순서)만 보유한다.
 */
@Entity
@Table(name = "guide_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 프론트 뱃지 식별 키 (예: COURSE_REG). 언어 무관 — 아이콘/색상 매핑에 사용. */
    @Column(name = "badge_key", nullable = false)
    private String badgeKey;

    /** 카테고리 대표 이모지 (예: 📚). */
    @Column
    private String emoji;

    /** 카테고리 테마 색상 (예: #C41230). */
    @Column
    private String color;

    /** 노출 정렬 순서(오름차순). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 관리자 수정 시 메타 갱신 (텍스트는 번역 행에서 별도 관리). */
    public void updateMeta(String badgeKey, String emoji, String color, int sortOrder) {
        this.badgeKey = badgeKey;
        this.emoji = emoji;
        this.color = color;
        this.sortOrder = sortOrder;
    }
}
