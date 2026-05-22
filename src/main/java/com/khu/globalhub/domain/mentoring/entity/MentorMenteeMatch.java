package com.khu.globalhub.domain.mentoring.entity;

import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.shared.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 멘토-멘티 매칭 엔티티.
 * 매 학기 개강 첫 주 월요일에 스케줄러가 자동으로 매칭을 생성한다.
 *
 * 매칭 기준 (우선순위):
 *   1. 같은 학과
 *   2. 같은 국적
 *   3. 학번 차이 1~2년 선배 우선
 *
 * 학기 종료 시 스케줄러가 ACTIVE → COMPLETED 처리하고,
 * 멘티였던 유저의 menteeSemesterCount를 +1 하여 2 이상이면 자동 MENTOR 전환.
 */
@Entity
@Table(name = "mentor_mentee_matches")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MentorMenteeMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 멘토 역할의 유저. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Member mentor;

    /** 멘티 역할의 유저. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private Member mentee;

    /**
     * 매칭이 이루어진 학기. 형식: "2025-1", "2025-2"
     * 학기 구분 및 스케줄러 중복 방지에 사용.
     */
    @Column(nullable = false)
    private String semester;

    /**
     * 매칭 상태.
     * ACTIVE    : 현재 진행 중
     * COMPLETED : 학기 종료로 완료
     * CANCELLED : 중도 취소
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.ACTIVE;

    /** 매칭이 생성된 시각. */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime matchedAt;

    public void complete() {
        this.status = MatchStatus.COMPLETED;
    }

    public void cancel() {
        this.status = MatchStatus.CANCELLED;
    }
}
