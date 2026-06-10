package com.khu.globalhub.campusguide.presentation;

import com.khu.globalhub.campusguide.application.GuideAdminService;
import com.khu.globalhub.campusguide.presentation.dto.AdminGuideCategoryRequest;
import com.khu.globalhub.campusguide.presentation.dto.AdminGuideTipRequest;
import com.khu.globalhub.shared.common.AdminGuard;
import com.khu.globalhub.shared.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용 학사 가이드 운영 API. 모든 엔드포인트 AdminGuard로 관리자만.
 * 카테고리/팁 각각 생성/수정 시 원문 저장 + 6개 언어 비동기 사전번역(퀴즈 방식).
 */
@RestController
@RequestMapping("/api/admin/guide")
@RequiredArgsConstructor
public class AdminGuideController {

    private final GuideAdminService guideAdminService;
    private final AdminGuard adminGuard;

    // ── 카테고리 ─────────────────────────────────────────

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Long>> createCategory(@RequestBody AdminGuideCategoryRequest request) {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok("가이드 카테고리를 생성했습니다.", guideAdminService.createCategory(request)));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody AdminGuideCategoryRequest request
    ) {
        adminGuard.check();
        guideAdminService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.ok("가이드 카테고리를 수정했습니다.", null));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        adminGuard.check();
        guideAdminService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.ok("가이드 카테고리를 삭제했습니다.", null));
    }

    // ── 팁 ───────────────────────────────────────────────

    @PostMapping("/tips")
    public ResponseEntity<ApiResponse<Long>> createTip(@RequestBody AdminGuideTipRequest request) {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok("가이드 팁을 생성했습니다.", guideAdminService.createTip(request)));
    }

    @PutMapping("/tips/{tipId}")
    public ResponseEntity<ApiResponse<Void>> updateTip(
            @PathVariable Long tipId,
            @RequestBody AdminGuideTipRequest request
    ) {
        adminGuard.check();
        guideAdminService.updateTip(tipId, request);
        return ResponseEntity.ok(ApiResponse.ok("가이드 팁을 수정했습니다.", null));
    }

    @DeleteMapping("/tips/{tipId}")
    public ResponseEntity<ApiResponse<Void>> deleteTip(@PathVariable Long tipId) {
        adminGuard.check();
        guideAdminService.deleteTip(tipId);
        return ResponseEntity.ok(ApiResponse.ok("가이드 팁을 삭제했습니다.", null));
    }
}
