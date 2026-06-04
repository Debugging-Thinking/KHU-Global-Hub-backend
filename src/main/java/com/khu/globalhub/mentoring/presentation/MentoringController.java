package com.khu.globalhub.mentoring.presentation;

import com.khu.globalhub.mentoring.presentation.dto.MentoringMatchResponse;
import com.khu.globalhub.mentoring.application.MentoringService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    /**
     * 수동 매칭 트리거 (관리자/테스트용).
     * POST /api/mentoring/run?semester=2026-1
     * semester 미입력 시 현재 학기로 자동 계산.
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Void>> runMatching(
            @RequestParam(required = false) String semester
    ) {
        if (semester == null || semester.isBlank()) {
            LocalDate now = LocalDate.now();
            semester = now.getYear() + (now.getMonthValue() <= 6 ? "-1" : "-2");
        }
        mentoringService.runMatching(semester);
        return ResponseEntity.ok(ApiResponse.ok("매칭이 완료되었습니다.", null));
    }
}
