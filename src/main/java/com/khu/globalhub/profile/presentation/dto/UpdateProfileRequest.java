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

        // Nullable: freshmen fixed to MENTEE, verified in service layer
        MentoringRole mentoringRole,

        // Nullable: optional self-introduction
        String bio
) {}