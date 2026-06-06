package com.khu.globalhub.qna.presentation.dto;

import com.khu.globalhub.shared.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQnARequest(

        @NotNull
        Boolean isAnonymous,

        @NotNull
        Language language,

        @NotBlank
        String title,

        @NotBlank
        String content,

        /** 첨부 이미지 URL (선택). */
        String imageUrl
) {}
