package com.khu.globalhub.campusguide.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 퀴즈 문항 메타데이터.
 * 다국어 전환(V16) 이후 텍스트(question/options/explanation)는 {@link QuizQuestionTranslation}으로 분리되었고,
 * 이 엔티티는 언어 무관 메타(카테고리, 정답 인덱스)만 보유한다.
 */
@Entity
@Table(name = "quiz_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 카테고리 — {@link BadgeId} enum(닫힌 집합, 5종)에 바인딩.
     * 자유 문자열이 아니므로 오타·다른 표기("수강신청" 등)가 DB에 들어올 수 없다(조용히 사라지는 사고 방지).
     * 컬럼은 VARCHAR 그대로 enum 이름(COURSE_REG 등)을 저장 — 스키마/와이어 포맷 무변경.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeId category;

    /** 정답 보기의 인덱스(0-base). 채점 기준 — 언어와 무관. */
    @Column(nullable = false)
    private Integer answerIndex;

    /** 관리자 수정 시 메타 갱신 (텍스트는 번역 행에서 별도 관리). */
    public void updateMeta(BadgeId category, Integer answerIndex) {
        this.category = category;
        this.answerIndex = answerIndex;
    }
}
