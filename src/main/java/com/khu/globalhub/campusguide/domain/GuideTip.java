package com.khu.globalhub.campusguide.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 학사 가이드 팁 메타데이터 (카테고리 하위 항목).
 * 다국어 텍스트(title/content)는 {@link GuideTipTranslation}으로 분리하고,
 * 이 엔티티는 언어 무관 메타(아이콘, 링크, 정렬순서)와 소속 카테고리 ID만 보유한다.
 *
 * BC 격리: 카테고리는 {@code category_id}(Long ID)로만 참조(@ManyToOne 직접참조 금지).
 */
@Entity
@Table(name = "guide_tips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 카테고리 소속인지 (ID 참조). */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** 팁 아이콘 이모지 (예: 💻). */
    @Column
    private String icon;

    /** 외부 링크 (선택). */
    @Column(columnDefinition = "TEXT")
    private String link;

    /** 카테고리 내 노출 정렬 순서(오름차순). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 관리자 수정 시 메타 갱신 (텍스트는 번역 행에서 별도 관리). */
    public void updateMeta(String icon, String link, int sortOrder) {
        this.icon = icon;
        this.link = link;
        this.sortOrder = sortOrder;
    }
}
