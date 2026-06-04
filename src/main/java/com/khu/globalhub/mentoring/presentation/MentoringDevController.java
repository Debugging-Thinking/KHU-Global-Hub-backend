package com.khu.globalhub.mentoring.presentation;

import com.khu.globalhub.mentoring.application.MentoringService;
import com.khu.globalhub.shared.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 멘토링 수동 매칭 트리거 — 로컬 전용(@Profile("local")).
 * 운영 프로필에서는 빈이 등록되지 않아 엔드포인트가 노출되지 않는다.
 * 운영 매칭은 MentoringScheduler(매년 3/1·9/1)가 담당하며,
 * 전체 재매칭을 임의의 로그인 유저가 트리거하지 못하도록 prod에서 제외한다.
 */
@Profile("local")
@RestController
@RequestMapping("/api/mentoring")
@RequiredArgsConstructor
public class MentoringDevController {

    private final MentoringService mentoringService;

    /**
     * 수동 매칭 트리거 (로컬 테스트용).
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
