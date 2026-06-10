package com.khu.globalhub.shared.port;

import java.util.Optional;

/**
 * identity BC가 노출하는 읽기 전용 조회 포트 (크로스-BC 계약).
 * 다른 BC가 계정 존재 여부·이메일을 알아야 할 때 identity 패키지를 직접 import하지 않고
 * 이 인터페이스에만 의존한다. 구현은 identity BC가 제공(MemberQueryAdapter).
 */
public interface MemberQueryPort {

    /** 계정 존재 여부 (예: 채팅 수신자 검증). */
    boolean exists(Long memberId);

    /** 이메일. 계정 미존재 시 empty (예: 프로필 응답 조립). */
    Optional<String> findEmail(Long memberId);

    /** 관리자 계정 여부 (예: 삭제 권한·관리자 전용 API 가드). 미존재 시 false. */
    boolean isAdmin(Long memberId);
}
