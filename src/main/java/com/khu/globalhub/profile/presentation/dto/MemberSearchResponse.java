package com.khu.globalhub.profile.presentation.dto;

import com.khu.globalhub.profile.domain.Profile;

/**
 * 관리자 회원검색 결과 행 (이름/아바타/학과 + memberId로 프로필 이동).
 */
public record MemberSearchResponse(
        Long memberId,
        String name,
        String profileImageUrl,
        String department
) {
    public static MemberSearchResponse from(Profile p) {
        return new MemberSearchResponse(p.getMemberId(), p.getName(), p.getProfileImage(), p.getDepartment());
    }
}
