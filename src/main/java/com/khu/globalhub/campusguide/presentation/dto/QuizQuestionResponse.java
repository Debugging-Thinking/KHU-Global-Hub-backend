package com.khu.globalhub.campusguide.presentation.dto;

import java.util.List;

/**
 * 퀴즈 문항 응답 (정답/해설 미포함).
 * 텍스트(question/options)는 요청 언어의 번역 행에서 조립된다(없으면 KO 폴백).
 */
public record QuizQuestionResponse(
        Long id,
        String category,
        String question,
        List<String> options
) {}
