package com.khu.globalhub.chat.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(

        @NotNull
        Long receiverId,

        /** 메시지 내용. 이미지만 보낼 경우 비어 있을 수 있다(서비스에서 content/imageUrl 중 하나 필수 검증). */
        String content,

        /** 첨부 이미지 URL (선택). */
        String imageUrl
) {}
