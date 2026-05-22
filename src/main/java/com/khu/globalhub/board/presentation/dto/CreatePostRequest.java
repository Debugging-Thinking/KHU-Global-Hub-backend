package com.khu.globalhub.board.presentation.dto;

import com.khu.globalhub.shared.enums.BoardType;
import com.khu.globalhub.shared.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePostRequest(

        @NotNull
        BoardType boardType,

        @NotNull
        Boolean isAnonymous,

        /** 작성자가 입력한 원문 언어 */
        @NotNull
        Language language,

        @NotBlank
        String title,

        @NotBlank
        String content
) {}
