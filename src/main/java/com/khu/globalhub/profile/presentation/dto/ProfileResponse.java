package com.khu.globalhub.profile.presentation.dto;

import com.khu.globalhub.profile.domain.Profile;
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
        String preferredLanguage,
        MentoringRole mentoringRole,
        double quizScore,
        String bio,
        String theme,
        boolean isAdmin,
        boolean isActive
) {
    /** isAdmin/isActive 불필요한 컨텍스트(예: 멘토링 파트너 카드)용 오버로드 — 기본값 위임. */
    public static ProfileResponse from(Profile profile, String email) {
        return from(profile, email, false, true);
    }

    public static ProfileResponse from(Profile profile, String email, boolean isAdmin, boolean isActive) {
        // 레거시 행(preferredLanguage=null)은 language enum의 Azure 코드로 폴백.
        String preferred = profile.getPreferredLanguage() != null
                ? profile.getPreferredLanguage()
                : profile.getLanguage().toAzureCode();
        return new ProfileResponse(
                profile.getMemberId(),
                email,
                profile.getName(),
                profile.getProfileImage(),
                profile.getDepartment(),
                profile.getNationality(),
                profile.getAdmissionYear(),
                profile.getLanguage(),
                preferred,
                profile.getMentoringRole(),
                profile.getQuizScore(),
                profile.getBio(),
                profile.getTheme(),
                isAdmin,
                isActive
        );
    }
}