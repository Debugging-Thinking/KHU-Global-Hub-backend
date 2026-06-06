package com.khu.globalhub.coursereview.presentation;

import com.khu.globalhub.coursereview.application.LectureImportService;
import com.khu.globalhub.shared.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경희대 국제캠 수강편람 수집 트리거. (sugang.khu.ac.kr 종합시간표 스크래핑 → lectures upsert)
 * 운영/로컬 공용 — 학기 데이터 갱신 시 1회 호출.
 */
@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureImportController {

    private final LectureImportService lectureImportService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Integer>> importLectures(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "10") String term
    ) {
        int count = lectureImportService.importGlobalCampus(year, term);
        return ResponseEntity.ok(ApiResponse.ok(count + "개 강의 수집 완료", count));
    }
}
