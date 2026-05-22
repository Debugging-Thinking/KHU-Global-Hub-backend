package com.khu.globalhub.identity.application;

import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;

/**
 * profile BC로 향하는 아웃바운드 포트 (의존성 역전).
 * identity는 이 인터페이스만 알고, 구현 어댑터는 profile BC가 제공한다.
 * → identity가 profile 패키지를 import 하지 않는다 (BC 격리, 의존 방향 profile→identity).
 */
public interface ProfileGateway {

    /** 해당 계정의 프로필 존재 여부 (로그인 응답 hasProfile 라우팅용). */
    boolean exists(Long memberId);

    /** 최초 프로필 생성 (POST /api/auth/profile). 비즈니스 규칙 검증은 profile BC가 담당. */
    void create(ProfileCreationCommand command);

    record ProfileCreationCommand(
            Long memberId,
            String name,
            String department,
            String nationality,
            Integer admissionYear,
            Language language,
            MentoringRole mentoringRole
    ) {}
}
