package com.khu.globalhub.identity.presentation.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;

    /** false면 프론트에서 프로필 생성 화면으로 이동 */
    private final boolean hasProfile;

    /** 관리자 계정 여부 — 프론트가 관리자 모드 화면 분기에 사용. */
    private final boolean isAdmin;

    public static LoginResponse of(String accessToken, String refreshToken, boolean hasProfile, boolean isAdmin) {
        return new LoginResponse(accessToken, refreshToken, hasProfile, isAdmin);
    }
}
