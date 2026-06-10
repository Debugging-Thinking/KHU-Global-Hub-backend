package com.khu.globalhub.campusguide.presentation;

import com.khu.globalhub.campusguide.application.GuideService;
import com.khu.globalhub.campusguide.presentation.dto.GuideResponse;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학사 가이드 조회 API. 카테고리(정렬순서) + 하위 팁(정렬순서)을 트리로 반환한다.
 * language로 번역 선택(기본 KO, 없으면 KO 폴백).
 */
@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuideResponse>>> getGuide(
            @RequestParam(defaultValue = "KO") Language language
    ) {
        return ResponseEntity.ok(ApiResponse.ok(guideService.getGuide(language)));
    }
}
