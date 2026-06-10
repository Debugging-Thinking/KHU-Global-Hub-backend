package com.khu.globalhub.mentoring.presentation.dto;

import java.util.List;

/**
 * 매칭 대기열 — 현재 ACTIVE 매칭이 없는 활성 멤버(관리자 제외).
 * 학기 무관 전역 풀: 어느 학기 탭에서 보든 동일, 실행하는 탭의 학기로 매칭된다.
 */
public record MentoringQueueResponse(
        List<MemberCard> waitingMentors,
        List<MemberCard> waitingMentees
) {
    public record MemberCard(
            Long memberId,
            String name,
            String department,
            String nationality,
            int admissionYear
    ) {}
}
