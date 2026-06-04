package com.khu.globalhub.campusguide.presentation.dto;

import com.khu.globalhub.campusguide.domain.BadgeId;
import com.khu.globalhub.campusguide.domain.MemberBadge;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BadgeResponse {
    private final String badgeId;
    private final String badgeNameKO;
    private final String badgeNameEN;
    private final String emoji;
    private final LocalDateTime earnedAt;

    public BadgeResponse(MemberBadge mb) {
        BadgeId b = mb.getBadgeId();
        this.badgeId     = b.name();
        this.badgeNameKO = b.getNameKO();
        this.badgeNameEN = b.getNameEN();
        this.emoji       = b.getEmoji();
        this.earnedAt    = mb.getCreatedAt();
    }
}
