package com.khu.globalhub.mentoring.activity.presentation.dto;

import com.khu.globalhub.mentoring.activity.domain.MentoringActivity;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityResponse(
        Long id,
        Long matchId,
        Long authorId,
        String title,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(MentoringActivity a) {
        List<String> urls = a.getImages().stream()
                .sorted(java.util.Comparator.comparingInt(
                        com.khu.globalhub.mentoring.activity.domain.ActivityImage::getOrderIndex))
                .map(com.khu.globalhub.mentoring.activity.domain.ActivityImage::getImageUrl)
                .toList();
        return new ActivityResponse(
                a.getId(),
                a.getMatchId(),
                a.getAuthorId(),
                a.getTitle(),
                a.getContent(),
                urls,
                a.getCreatedAt()
        );
    }
}