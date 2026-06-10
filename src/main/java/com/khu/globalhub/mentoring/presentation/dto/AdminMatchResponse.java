package com.khu.globalhub.mentoring.presentation.dto;

import java.time.LocalDateTime;

/**
 * 관리자 전체 매칭 현황 행. (멘토/멘티 이름 포함, 탈퇴 시 "(탈퇴)" 폴백)
 */
public record AdminMatchResponse(
        Long matchId,
        Long mentorId,
        String mentorName,
        Long menteeId,
        String menteeName,
        String semester,
        String status,
        LocalDateTime matchedAt
) {}
