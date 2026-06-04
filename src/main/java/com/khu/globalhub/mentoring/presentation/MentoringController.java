package com.khu.globalhub.mentoring.presentation;

import com.khu.globalhub.mentoring.presentation.dto.MentoringMatchResponse;
import com.khu.globalhub.mentoring.application.MentoringService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

    /**
     * 내 전체 매칭 이력 조회.
     */
    @GetMapping("/me/history")
    public ResponseEntity<ApiResponse<List<MentoringMatchResponse>>> getMyMatchHistory() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<MentoringMatchResponse> result = mentoringService.getMyMatchHistory(memberId);
        return ResponseEntity.ok(ApiResponse.ok("매칭 이력을 조회했습니다.", result));
    }
}
