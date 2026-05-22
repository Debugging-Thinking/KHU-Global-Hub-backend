package com.khu.globalhub.shared.enums;

/**
 * 유저의 멘토링 역할.
 *
 * MENTEE : 신입생 (가입 시 기본값, 멘토 선택 불가)
 * MENTOR : 멘티 2학기 완료 후 자동 전환 또는 재학생이 직접 선택
 * NONE   : 멘토링 미희망 (프로필 설정에서 직접 변경해야 함)
 */
public enum MentoringRole {
    MENTOR,
    MENTEE,
    NONE
}
