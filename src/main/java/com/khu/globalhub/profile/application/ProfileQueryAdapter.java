package com.khu.globalhub.profile.application;

import com.khu.globalhub.profile.domain.Profile;
import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * shared의 {@link ProfileQueryPort}를 profile BC가 구현한 어댑터.
 * 다른 BC는 이 클래스를 모르고 인터페이스에만 의존한다.
 */
@Component
@RequiredArgsConstructor
public class ProfileQueryAdapter implements ProfileQueryPort {

    private final ProfileRepository profileRepository;

    @Override
    public Optional<String> findName(Long memberId) {
        return profileRepository.findByMemberId(memberId).map(Profile::getName);
    }

    @Override
    public Optional<ProfileCard> findCard(Long memberId) {
        return profileRepository.findByMemberId(memberId)
                .map(p -> new ProfileCard(p.getName(), p.getProfileImage()));
    }
}
