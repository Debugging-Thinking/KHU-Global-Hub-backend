package com.khu.globalhub.campusguide.presentation.dto;

import java.util.List;

/**
 * 학사 가이드 조회 응답 (카테고리 + 하위 팁 트리).
 * 텍스트(title/content)는 요청 언어 번역 행에서 조립된다(없으면 KO 폴백).
 * badgeKey/emoji/color/icon/link는 언어 무관 메타.
 */
public record GuideResponse(
        Long id,
        String badgeKey,
        String emoji,
        String color,
        String title,
        List<Tip> tips
) {
    public record Tip(
            Long id,
            String icon,
            String link,
            String title,
            String content
    ) {}
}
