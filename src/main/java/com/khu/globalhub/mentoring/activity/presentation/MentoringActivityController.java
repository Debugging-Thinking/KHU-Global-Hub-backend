package com.khu.globalhub.mentoring.activity.presentation;

import com.khu.globalhub.mentoring.activity.application.MentoringActivityService;
import com.khu.globalhub.mentoring.activity.presentation.dto.ActivityResponse;
import com.khu.globalhub.mentoring.activity.presentation.dto.CreateActivityRequest;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mentoring/{matchId}/activities")
@RequiredArgsConstructor
public class MentoringActivityController {

    private final MentoringActivityService activityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(
            @PathVariable Long matchId
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok("활동 기록을 조회했습니다.",
                activityService.getActivities(matchId, myId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @PathVariable Long matchId,
            @Valid @RequestBody CreateActivityRequest req
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok("활동 기록이 등록되었습니다.",
                activityService.createActivity(matchId, myId, req)));
    }
}