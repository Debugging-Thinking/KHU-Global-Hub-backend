package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.shared.enums.Language;

/**
 * 관리자 가이드 팁 생성/수정 요청.
 * categoryId(소속 카테고리)/icon/link/sortOrder는 메타로, title+content는 원문(language, 기본 KO) 번역 행으로 저장된다.
 * 저장 후 나머지 5개 언어는 비동기로 번역된다.
 */
public record AdminGuideTipRequest(
        Long categoryId,
        String icon,
        String link,
        Integer sortOrder,
        String title,
        String content,
        Language language   // 원문 언어 (선택, 기본 KO)
) {}
