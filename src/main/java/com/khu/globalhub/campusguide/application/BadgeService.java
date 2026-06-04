package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.BadgeId;
import com.khu.globalhub.campusguide.domain.MemberBadge;
import com.khu.globalhub.campusguide.infrastructure.MemberBadgeRepository;
import com.khu.globalhub.campusguide.presentation.dto.BadgeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final MemberBadgeRepository memberBadgeRepository;

    @Transactional
    public void earnBadge(Long memberId, String badgeIdStr) {
        BadgeId badgeId = BadgeId.valueOf(badgeIdStr);
        if (memberBadgeRepository.existsByMemberIdAndBadgeId(memberId, badgeId)) {
            return;
        }
        memberBadgeRepository.save(MemberBadge.of(memberId, badgeId));
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> getBadges(Long memberId) {
        return memberBadgeRepository.findAllByMemberId(memberId)
                .stream()
                .map(BadgeResponse::new)
                .toList();
    }
}
