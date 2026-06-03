package com.khu.globalhub.profile.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String nationality;

    @Column(nullable = false)
    private Integer admissionYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentoringRole mentoringRole;

    @Column(length = 500)
    private String bio;

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
                              Integer admissionYear, Language language, String bio) {
        this.name = name;
        this.department = department;
        this.nationality = nationality;
        this.admissionYear = admissionYear;
        this.language = language;
        this.bio = bio;
    }
}