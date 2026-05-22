package com.khu.globalhub.profile.application;

import com.khu.globalhub.identity.application.ProfileGateway;
import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * identity의 {@link ProfileGateway} 포트를 profile BC가 구현한 어댑터.
 * 존재 조회는 repository로, 생성은 MemberService(검증 포함)로 위임한다.
 */
@Component
@RequiredArgsConstructor
public class ProfileGatewayAdapter implements ProfileGateway {

    private final ProfileRepository profileRepository;
    private final MemberService memberService;

    @Override
    public boolean exists(Long memberId) {
        return profileRepository.existsByMemberId(memberId);
    }

    @Override
    public void create(ProfileCreationCommand c) {
        memberService.createProfile(c.memberId(), c.name(), c.department(),
                c.nationality(), c.admissionYear(), c.language(), c.mentoringRole());
    }
}
