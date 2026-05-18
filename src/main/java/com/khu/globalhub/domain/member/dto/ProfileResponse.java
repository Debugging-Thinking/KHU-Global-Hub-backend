package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.enums.MentoringRole;

public record ProfileResponse(
        Long memberId,
        String email,
        String name,
        String profileImageUrl,
        String department,
        String nationality,
        int admissionYear,
        Language language,
        MentoringRole mentoringRole,
        double quizScore
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getMember().getId(),
                profile.getMember().getEmail(),
                profile.getName(),
                profile.getProfileImage(),
                profile.getDepartment(),
                profile.getNationality(),
                profile.getAdmissionYear(),
                profile.getLanguage(),
                profile.getMentoringRole(),
                profile.getQuizScore()
        );
    }
}
