package com.khu.globalhub.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(

        @NotNull
        Long receiverId,

        @NotBlank
        String content
) {}
