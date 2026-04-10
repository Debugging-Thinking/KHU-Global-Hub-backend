package com.khu.globalhub.domain.auth.dto;

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

    public static LoginResponse of(String accessToken, String refreshToken, boolean hasProfile) {
        return new LoginResponse(accessToken, refreshToken, hasProfile);
    }
}
