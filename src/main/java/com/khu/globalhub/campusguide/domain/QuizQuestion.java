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

    @Column(nullable = false)
    private String category;

    /** 정답 보기의 인덱스(0-base). 채점 기준 — 언어와 무관. */
    @Column(nullable = false)
    private Integer answerIndex;

    /** 관리자 수정 시 메타 갱신 (텍스트는 번역 행에서 별도 관리). */
    public void updateMeta(String category, Integer answerIndex) {
        this.category = category;
        this.answerIndex = answerIndex;
    }
}
