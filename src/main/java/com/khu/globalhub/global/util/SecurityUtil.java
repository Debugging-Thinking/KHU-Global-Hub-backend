package com.khu.globalhub.global.util;

import com.khu.globalhub.global.exception.CustomException;
import com.khu.globalhub.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 컨트롤러/서비스에서 현재 로그인된 회원 ID를 꺼낼 때 사용한다.
 * JwtAuthenticationFilter가 SecurityContext에 memberId(Long)를 principal로 저장한다.
 */
public class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return (Long) auth.getPrincipal();
    }
}
