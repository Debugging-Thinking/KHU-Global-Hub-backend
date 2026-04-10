package com.khu.globalhub.global.enums;

/**
 * 멘토-멘티 매칭의 진행 상태.
 *
 * ACTIVE    : 현재 활성화된 매칭 (학기 진행 중)
 * COMPLETED : 학기 종료로 완료된 매칭
 * CANCELLED : 중도 취소된 매칭
 */
public enum MatchStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
