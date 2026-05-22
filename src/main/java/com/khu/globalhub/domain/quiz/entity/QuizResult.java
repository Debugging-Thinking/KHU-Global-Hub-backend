package com.khu.globalhub.domain.quiz.entity;

import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.global.common.BaseTimeEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer correctCount;

    @Column(nullable = false)
    private Integer totalCount;

    /** 점수 (0.0 ~ 100.0). 당근 온도처럼 프로필에 반영될 값. */
    @Column(nullable = false)
    private Double score;
}
