package com.khu.globalhub.domain.qna.dto;

import com.khu.globalhub.global.enums.Language;
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
