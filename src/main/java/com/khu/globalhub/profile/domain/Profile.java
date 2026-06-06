package com.khu.globalhub.profile.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;
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

    /** 소유 계정 ID. identity BC를 ID로만 참조 (D7). */
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

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
     * 실제 선택한 선호 언어의 Azure 코드 (6개 외 언어 포함, 예: "fr", "ja", "ko").
     * {@link #language}(6개 enum)는 이 값에서 파생된 "버킷"이다:
     * 6개 중 하나면 그 언어, 그 외면 EN. 정적 UI/사전번역 콘텐츠 선택에 language를 쓰고,
     * on-demand 번역(콘텐츠/채팅)의 목표 언어에는 preferredLanguage를 쓴다.
     * (레거시 행은 null일 수 있어 응답 조립 시 language 코드로 폴백)
     */
    @Column(name = "preferred_language")
    private String preferredLanguage;

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

    /** 자기소개. 선택 입력(nullable), 최대 500자. */
    @Column(length = 500)
    private String bio;

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
                              Integer admissionYear, Language language, String preferredLanguage, String bio) {
        this.name = name;
        this.department = department;
        this.nationality = nationality;
        this.admissionYear = admissionYear;
        this.language = language;
        this.preferredLanguage = preferredLanguage;
        this.bio = bio;
    }
}
