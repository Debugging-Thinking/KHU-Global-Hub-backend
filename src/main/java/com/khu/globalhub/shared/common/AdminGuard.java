package com.khu.globalhub.shared.common;

import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 관리자 전용 API 가드. 현재 요청자(JWT principal)가 관리자가 아니면 ADMIN_ONLY(403).
 * 어떤 BC의 컨트롤러든 주입해 {@code adminGuard.check()}로 사용 (shared.port에만 의존, 구현 BC 비결합).
 */
@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final MemberQueryPort memberQueryPort;

    public void check() {
        if (!memberQueryPort.isAdmin(SecurityUtil.getCurrentMemberId())) {
            throw new CustomException(ErrorCode.ADMIN_ONLY);
        }
    }
}
