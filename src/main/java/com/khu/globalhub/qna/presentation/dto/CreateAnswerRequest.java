package com.khu.globalhub.qna.presentation.dto;

import com.khu.globalhub.shared.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnswerRequest(

        @NotNull
        Boolean isAnonymous,

        @NotNull
        Language language,

        @NotBlank
        String content
) {}
