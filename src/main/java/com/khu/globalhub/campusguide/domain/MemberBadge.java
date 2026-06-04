package com.khu.globalhub.campusguide.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_badges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberBadge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_id", nullable = false, length = 50)
    private BadgeId badgeId;

    public static MemberBadge of(Long memberId, BadgeId badgeId) {
        return MemberBadge.builder()
                .memberId(memberId)
                .badgeId(badgeId)
                .build();
    }
}
