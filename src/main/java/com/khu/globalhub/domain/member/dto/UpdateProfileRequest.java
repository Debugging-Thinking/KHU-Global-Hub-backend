package com.khu.globalhub.domain.member.dto;

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

        // 멘토링 역할: MENTOR(멘토) / MENTEE(멘티) / NONE(미참여)
        @NotNull
        MentoringRole mentoringRole
) {}
