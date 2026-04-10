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

import java.util.List;

@RestController
@RequestMapping("/api/mentoring")
@RequiredArgsConstructor
public class MentoringController {

    private final MentoringService mentoringService;

    /**
     * 내 현재 학기 매칭 목록 조회.
     * 멘티: 1개, 멘토: 여러 개 가능. 빈 리스트 = 매칭 없음.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MentoringMatchResponse>>> getMyMatch() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<MentoringMatchResponse> result = mentoringService.getMyMatch(memberId);
        String message = result.isEmpty() ? "현재 진행 중인 매칭이 없습니다." : "매칭 정보를 조회했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }
}
