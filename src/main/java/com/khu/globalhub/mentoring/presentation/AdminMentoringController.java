package com.khu.globalhub.mentoring.presentation;

import com.khu.globalhub.mentoring.application.MentoringService;
import com.khu.globalhub.mentoring.presentation.dto.AdminMatchResponse;
import com.khu.globalhub.mentoring.presentation.dto.MentoringQueueResponse;
import com.khu.globalhub.mentoring.presentation.dto.RunMatchingRequest;
import com.khu.globalhub.shared.common.AdminGuard;
import com.khu.globalhub.shared.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 전용 멘토링 운영 API. 자동 스케줄러를 폐지(mentoring.scheduler.enabled=false)하고
 * 운영자가 직접 매칭을 실행/조회한다. 모든 엔드포인트는 AdminGuard로 관리자만 허용.
 */
@RestController
@RequestMapping("/api/admin/mentoring")
@RequiredArgsConstructor
public class AdminMentoringController {

    private final MentoringService mentoringService;
    private final AdminGuard adminGuard;

    /** 선택 매칭 실행 — 선택된 memberIds에만 그리디 적용. semester 미입력 시 현재 학기. */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<Void>> runMatching(@RequestBody RunMatchingRequest request) {
        adminGuard.check();
        String semester = request.semester();
        if (semester == null || semester.isBlank()) {
            LocalDate now = LocalDate.now();
            semester = now.getYear() + (now.getMonthValue() <= 6 ? "-1" : "-2");
        }
        mentoringService.runMatching(semester, request.memberIds());
        return ResponseEntity.ok(ApiResponse.ok("매칭이 완료되었습니다.", null));
    }

    /** 전체 매칭 현황 조회. */
    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<AdminMatchResponse>>> getAllMatches() {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok(mentoringService.getAllMatches()));
    }

    /** 매칭 대기열(현재 ACTIVE 매칭 없는 활성 멤버) 조회. */
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<MentoringQueueResponse>> getQueue() {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok(mentoringService.getQueue()));
    }
}
