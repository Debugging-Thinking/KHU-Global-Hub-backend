package com.khu.globalhub.translation.presentation.dto;

import java.util.List;

/**
 * on-demand 번역 응답.
 * @param translations 입력 texts와 같은 순서의 번역문
 * @param detectedSource 자동 감지된 원문 언어 코드 (source 미지정 시), 없으면 null
 */
public record TranslateResponse(
        List<String> translations,
        String detectedSource
) {}
