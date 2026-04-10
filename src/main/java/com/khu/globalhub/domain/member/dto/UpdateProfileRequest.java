package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.global.enums.Language;
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
        Language language
) {}
