package com.khu.globalhub.profile.presentation.dto;

import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(

        @NotBlank
        String name,

        @NotBlank
        String department,

        @NotBlank
        String nationality,

        @NotNull
        Integer admissionYear,

        @NotNull
        Language language,

        // 선택값: 실제 선호 언어 Azure 코드(6개 외 포함). 미입력 시 language로 폴백. 서버가 language(버킷)를 파생.
        String preferredLanguage,

        // 선택값: 미입력(null) 시 기존 역할 유지. 신입생은 서비스 계층에서 MENTEE로 강제.
        MentoringRole mentoringRole,

        // 선택값: 자기소개 (최대 500자)
        String bio,

        // 선택값: UI 테마 선호 ("LIGHT" | "DARK"). 미입력 시 LIGHT.
        String theme
) {}
