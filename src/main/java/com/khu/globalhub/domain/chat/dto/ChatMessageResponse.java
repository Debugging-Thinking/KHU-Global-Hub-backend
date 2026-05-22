package com.khu.globalhub.domain.chat.dto;

import com.khu.globalhub.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long senderId,       // isSystem=true면 null
        String senderName,   // isSystem=true면 null
        String content,
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
                msg.getIsSystem(),
                msg.getIsRead(),
                msg.getSentAt()
        );
    }
}
