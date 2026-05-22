package com.khu.globalhub.identity.application;

import com.khu.globalhub.identity.domain.Member;
import com.khu.globalhub.identity.infrastructure.MemberRepository;
import com.khu.globalhub.shared.port.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * shared의 {@link MemberQueryPort}를 identity BC가 구현한 어댑터.
 * 다른 BC는 이 클래스를 모르고 인터페이스에만 의존한다.
 */
@Component
@RequiredArgsConstructor
public class MemberQueryAdapter implements MemberQueryPort {

    private final MemberRepository memberRepository;

    @Override
    public boolean exists(Long memberId) {
        return memberRepository.existsById(memberId);
    }

    @Override
    public Optional<String> findEmail(Long memberId) {
        return memberRepository.findById(memberId).map(Member::getEmail);
    }
}
