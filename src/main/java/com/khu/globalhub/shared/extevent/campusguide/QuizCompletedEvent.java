package com.khu.globalhub.shared.extevent.campusguide;

/**
 * 통합 이벤트: 퀴즈 응시 완료. campusguide BC가 발행, profile BC가 소비(quizScore 갱신).
 * payload는 ID + 점수만 (엔티티/집계 금지). 위치 규칙: shared/extevent/&lt;발행BC&gt;/.
 */
public record QuizCompletedEvent(Long memberId, double score) {}
