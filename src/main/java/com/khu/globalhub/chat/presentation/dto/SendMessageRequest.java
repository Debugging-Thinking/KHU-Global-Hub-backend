package com.khu.globalhub.chat.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(

        @NotNull
        Long receiverId,

        @NotBlank
        String content
) {}
