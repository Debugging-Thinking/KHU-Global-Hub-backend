package com.khu.globalhub.mentoring.activity.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateActivityRequest(
        @NotBlank String title,
        @NotBlank String content
) {}