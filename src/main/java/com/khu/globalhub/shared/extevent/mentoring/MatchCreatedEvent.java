package com.khu.globalhub.shared.extevent.mentoring;

/**
 * 통합 이벤트: 멘토-멘티 매칭 생성. mentoring BC가 발행, chat BC가 소비(시스템 메시지 삽입).
 * payload는 ID만. 위치 규칙: shared/extevent/&lt;발행BC&gt;/.
 */
public record MatchCreatedEvent(Long mentorId, Long menteeId) {}
