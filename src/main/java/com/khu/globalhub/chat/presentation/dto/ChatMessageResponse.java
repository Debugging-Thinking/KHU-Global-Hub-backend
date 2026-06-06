package com.khu.globalhub.chat.presentation.dto;

import com.khu.globalhub.chat.domain.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long senderId,       // isSystem=true면 null
        String senderName,   // isSystem=true면 null
        String content,
        String imageUrl,
        boolean isSystem,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse of(ChatMessage msg, String senderName) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSenderId(),
                senderName,
                msg.getContent(),
                msg.getImageUrl(),
                msg.getIsSystem(),
                msg.getIsRead(),
                msg.getSentAt()
        );
    }
}
