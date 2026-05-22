package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;

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
    public static ProfileResponse from(Profile profile, String email) {
        return new ProfileResponse(
                profile.getMemberId(),
                email,
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
