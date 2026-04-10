package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.enums.MentoringRole;

public record ProfileResponse(
        Long memberId,
        String name,
        String profileImage,
        String department,
        String nationality,
        int admissionYear,
        Language language,
        MentoringRole mentoringRole
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getMember().getId(),
                profile.getName(),
                profile.getProfileImage(),
                profile.getDepartment(),
                profile.getNationality(),
                profile.getAdmissionYear(),
                profile.getLanguage(),
                profile.getMentoringRole()
        );
    }
}
