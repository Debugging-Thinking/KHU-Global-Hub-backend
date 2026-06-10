package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.shared.enums.Language;

import java.util.List;

/**
 * 관리자 퀴즈 문항 생성/수정 요청.
 * category/answerIndex는 메타로, question/options/explanation은 원문(language, 기본 KO) 번역 행으로 저장된다.
 * 저장 후 나머지 5개 언어는 비동기로 번역된다.
 */
public record AdminQuizQuestionRequest(
        String category,
        String question,
        List<String> options,
        Integer answerIndex,
        String explanation,
        Language language   // 원문 언어 (선택, 기본 KO)
) {}
