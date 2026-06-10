package com.khu.globalhub.identity.application;

import com.khu.globalhub.identity.infrastructure.MemberRepository;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 운영(정지/해제). identity BC 소유.
 */
@Service
@RequiredArgsConstructor
public class MemberAdminService {

    private final MemberRepository memberRepository;

    @Transactional
    public void suspend(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                .suspend();
    }

    @Transactional
    public void activate(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                .activate();
    }
}
