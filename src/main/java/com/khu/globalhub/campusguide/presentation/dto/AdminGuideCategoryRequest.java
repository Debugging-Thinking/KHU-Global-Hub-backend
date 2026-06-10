package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.shared.enums.Language;

/**
 * 관리자 가이드 카테고리 생성/수정 요청.
 * badgeKey/emoji/color/sortOrder는 메타로, title은 원문(language, 기본 KO) 번역 행으로 저장된다.
 * 저장 후 나머지 5개 언어는 비동기로 번역된다.
 */
public record AdminGuideCategoryRequest(
        String badgeKey,
        String emoji,
        String color,
        Integer sortOrder,
        String title,
        Language language   // 원문 언어 (선택, 기본 KO)
) {}
