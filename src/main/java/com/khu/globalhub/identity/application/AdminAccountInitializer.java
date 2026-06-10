package com.khu.globalhub.identity.application;

import com.khu.globalhub.identity.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부팅 시 ADMIN_EMAIL(app.admin.email) 계정에 관리자 권한을 동기화한다.
 * 단일 운영자 계정 모델 — 해당 이메일 회원이 가입돼 있으면 is_admin=true로 보정한다.
 * 값이 비어있으면(local 등) 아무 것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        memberRepository.findByEmail(adminEmail).ifPresentOrElse(
                member -> {
                    if (!Boolean.TRUE.equals(member.getIsAdmin())) {
                        member.grantAdmin();
                        log.info("[Admin] '{}' 계정에 관리자 권한 부여", adminEmail);
                    }
                },
                () -> log.warn("[Admin] ADMIN_EMAIL '{}' 계정이 아직 가입되지 않음 — 가입 후 재기동 시 부여됨", adminEmail)
        );
    }
}
