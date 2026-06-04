package com.khu.globalhub.campusguide.presentation;

import com.khu.globalhub.campusguide.application.BadgeService;
import com.khu.globalhub.campusguide.presentation.dto.BadgeResponse;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @PostMapping("/api/badges/{badgeId}")
    public ResponseEntity<ApiResponse<Void>> earn(@PathVariable String badgeId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        badgeService.earnBadge(memberId, badgeId);
        return ResponseEntity.ok(ApiResponse.ok("뱃지를 획득했습니다."));
    }

    @GetMapping("/api/badges/me")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMyBadges() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok(badgeService.getBadges(memberId)));
    }

    @GetMapping("/api/members/{memberId}/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMemberBadges(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok(badgeService.getBadges(memberId)));
    }
}
