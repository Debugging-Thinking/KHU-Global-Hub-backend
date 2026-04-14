package com.khu.globalhub.domain.mentoring.controller;

import com.khu.globalhub.domain.mentoring.dto.MentoringMatchResponse;
import com.khu.globalhub.domain.mentoring.service.MentoringService;
import com.khu.globalhub.global.common.ApiResponse;
import com.khu.globalhub.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/mentoring")
@RequiredArgsConstructor
public class MentoringController {

    private final MentoringService mentoringService;

    /**
     * 내 현재 ACTIVE 매칭 단건 조회.
     * 매칭 없으면 404 반환 → 프론트에서 .catch로 처리.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MentoringMatchResponse>> getMyMatch() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        MentoringMatchResponse result = mentoringService.getMyMatch(memberId);
        return ResponseEntity.ok(ApiResponse.ok("매칭 정보를 조회했습니다.", result));
    }
}
