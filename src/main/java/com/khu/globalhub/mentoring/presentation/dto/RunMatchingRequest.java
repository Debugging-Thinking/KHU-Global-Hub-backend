package com.khu.globalhub.mentoring.presentation.dto;

import java.util.List;

/**
 * 관리자 선택 매칭 요청. memberIds = 대기열에서 관리자가 고른 멤버들(전체선택 포함).
 * semester 미지정 시 백엔드가 현재 학기로 계산.
 */
public record RunMatchingRequest(
        String semester,
        List<Long> memberIds
) {}
