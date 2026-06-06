package com.khu.globalhub.coursereview.presentation;

import com.khu.globalhub.coursereview.application.CourseReviewService;
import com.khu.globalhub.coursereview.presentation.dto.CreateReviewRequest;
import com.khu.globalhub.coursereview.presentation.dto.LectureDetailResponse;
import com.khu.globalhub.coursereview.presentation.dto.LectureSummaryResponse;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    /** 강의 검색 목록 (학기/검색어). */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LectureSummaryResponse>>> search(
            @RequestParam(defaultValue = "2026-1") String semester,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(courseReviewService.search(semester, query, pageable)));
    }

    /** 강의 상세 + 강의평 목록(요청 언어 번역, original=true면 원문). */
    @GetMapping("/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDetailResponse>> getLecture(
            @PathVariable Long lectureId,
            @RequestParam(defaultValue = "KO") Language language,
            @RequestParam(defaultValue = "false") boolean original
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok(courseReviewService.getLecture(lectureId, memberId, language, original)));
    }

    /** 강의평 작성. */
    @PostMapping("/{lectureId}/reviews")
    public ResponseEntity<ApiResponse<Long>> createReview(
            @PathVariable Long lectureId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long id = courseReviewService.createReview(lectureId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("강의평이 등록되었습니다.", id));
    }

    /** 강의평 삭제 (작성자 본인만). */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        courseReviewService.deleteReview(reviewId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("강의평이 삭제되었습니다."));
    }
}
