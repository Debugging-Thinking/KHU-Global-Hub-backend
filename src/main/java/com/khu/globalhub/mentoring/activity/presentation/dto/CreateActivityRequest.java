package com.khu.globalhub.mentoring.activity.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateActivityRequest(
        @NotBlank String title,
        @NotBlank String content,
        /** Base64 data URI 목록 (최대 5장) */
        List<String> imageDataUris
) {}