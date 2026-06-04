package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.BadgeId;
import com.khu.globalhub.campusguide.domain.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {
    boolean existsByMemberIdAndBadgeId(Long memberId, BadgeId badgeId);
    List<MemberBadge> findAllByMemberId(Long memberId);
}
