package com.khu.globalhub.domain.quiz.entity;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 응시자 ID. identity BC를 ID로만 참조 (D7). */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Integer correctCount;

    @Column(nullable = false)
    private Integer totalCount;

    /** 점수 (0.0 ~ 100.0). 당근 온도처럼 프로필에 반영될 값. */
    @Column(nullable = false)
    private Double score;
}
