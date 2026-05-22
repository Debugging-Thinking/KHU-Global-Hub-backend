package com.khu.globalhub.shared.port;

import java.util.Optional;

/**
 * profile BC가 노출하는 읽기 전용 조회 포트 (크로스-BC 계약).
 * 콘텐츠(board/qna/comment)·채팅 BC가 작성자·발신자 표시 정보를 얻을 때
 * profile 패키지를 직접 import하지 않고 이 인터페이스에만 의존한다.
 * 구현은 profile BC가 제공(ProfileQueryAdapter).
 * shared는 어떤 BC도 import하지 않으므로 공용 계약을 여기 둔다.
 *
 * NOTE: 목록 조회 N+1 최적화 시 배치 메서드(Map<Long,String> findNames(Collection))를 추가 예정.
 */
public interface ProfileQueryPort {

    /** 표시 이름. 프로필 미존재 시 empty (호출측에서 "Unknown" 등 폴백). */
    Optional<String> findName(Long memberId);

    /** 이름 + 프로필 이미지 (채팅 목록 미리보기 등). */
    Optional<ProfileCard> findCard(Long memberId);

    record ProfileCard(String name, String profileImage) {}
}
