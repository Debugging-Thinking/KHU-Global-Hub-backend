package com.khu.globalhub.domain.member.entity;

import com.khu.globalhub.global.common.BaseTimeEntity;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.enums.MentoringRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * 프로필 엔티티. Member와 1:1 관계.
 * 이메일 인증 완료 후 반드시 생성해야 앱을 사용할 수 있다.
 * Profile row가 존재하면 프로필 설정 완료, 없으면 미완성으로 판단한다.
 */
@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false)
    private String name;

    private String profileImage;  // S3 URL, nullable

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String nationality;

    @Column(nullable = false)
    private Integer admissionYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /**
     * 멘토링 역할.
     * - 신입생(입학년도 == 현재년도): MENTEE 고정
     * - 재학생: MENTOR / MENTEE 선택 가능 (NONE은 프로필 수정에서만)
     * - 매년 3월 스케줄러: 작년 입학 멘티 → 자동 MENTOR 전환
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentoringRole mentoringRole;

    /** 퀴즈 최고 점수 (0.0 ~ 100.0). 당근 온도처럼 프로필에 표시. */
    @Column(nullable = false)
    @Builder.Default
    private Double quizScore = 0.0;

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateMentoringRole(MentoringRole role) {
        this.mentoringRole = role;
    }

    public void updateLanguage(Language language) {
        this.language = language;
    }

    public void updateQuizScore(Double score) {
        this.quizScore = score;
    }

    public void updateProfile(String name, String department, String nationality,
                              Integer admissionYear, Language language) {
        this.name = name;
        this.department = department;
        this.nationality = nationality;
        this.admissionYear = admissionYear;
        this.language = language;
    }
}
