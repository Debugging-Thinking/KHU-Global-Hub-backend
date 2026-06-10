package com.khu.globalhub.identity.presentation;

import com.khu.globalhub.identity.application.MemberAdminService;
import com.khu.globalhub.shared.common.AdminGuard;
import com.khu.globalhub.shared.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 회원 운영 API — 활동 정지/해제. 모든 엔드포인트 AdminGuard로 관리자만.
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberAdminService memberAdminService;
    private final AdminGuard adminGuard;

    @PostMapping("/{memberId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable Long memberId) {
        adminGuard.check();
        memberAdminService.suspend(memberId);
        return ResponseEntity.ok(ApiResponse.ok("계정을 정지했습니다.", null));
    }

    @PostMapping("/{memberId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long memberId) {
        adminGuard.check();
        memberAdminService.activate(memberId);
        return ResponseEntity.ok(ApiResponse.ok("계정 정지를 해제했습니다.", null));
    }
}
