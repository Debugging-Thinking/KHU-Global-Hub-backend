package com.khu.globalhub.translation.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * on-demand 번역 요청.
 * @param texts  번역할 텍스트들 (게시글 제목/본문, 댓글, 채팅 메시지 등)
 * @param target 목표 언어 Azure 코드 (예: "fr", "ja", "ko"). 보통 조회자의 preferredLanguage.
 * @param source 원문 언어 Azure 코드. null이면 자동 감지.
 */
public record TranslateRequest(
        @NotEmpty List<String> texts,
        @NotNull String target,
        String source
) {}
