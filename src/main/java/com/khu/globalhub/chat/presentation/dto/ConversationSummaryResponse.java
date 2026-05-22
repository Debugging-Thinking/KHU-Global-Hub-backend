package com.khu.globalhub.chat.presentation.dto;

import java.time.LocalDateTime;

/**
 * DM 홈 화면의 대화 상대 목록 한 줄 요약.
 */
public record ConversationSummaryResponse(
        Long partnerId,
        String partnerName,
        String partnerProfileImage,  // nullable
        String lastMessage,
        int unreadCount,
        LocalDateTime lastMessageAt
) {}
