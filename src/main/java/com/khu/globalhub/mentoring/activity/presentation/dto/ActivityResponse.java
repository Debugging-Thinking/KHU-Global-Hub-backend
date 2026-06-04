package com.khu.globalhub.mentoring.activity.presentation.dto;

import com.khu.globalhub.mentoring.activity.domain.MentoringActivity;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        Long matchId,
        Long authorId,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(MentoringActivity a) {
        return new ActivityResponse(
                a.getId(),
                a.getMatchId(),
                a.getAuthorId(),
                a.getTitle(),
                a.getContent(),
                a.getCreatedAt()
        );
    }
}