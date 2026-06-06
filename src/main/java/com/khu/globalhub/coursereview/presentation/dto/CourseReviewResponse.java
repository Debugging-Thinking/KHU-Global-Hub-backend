package com.khu.globalhub.coursereview.presentation.dto;

import com.khu.globalhub.coursereview.domain.CourseReview;
import com.khu.globalhub.shared.enums.Language;

import java.time.LocalDateTime;

/**
 * 강의평 응답 — 익명(작성자 이름 미노출). isMine은 삭제 버튼 표시용.
 * content는 요청 언어 번역본, originalContent/originalLanguage는 작성 원문(항목별 토글용).
 */
public record CourseReviewResponse(
        Long reviewId,
        int rating,
        String content,
        String originalContent,
        Language originalLanguage,
        boolean isMine,
        String attendanceType,
        String presentationFreq,
        String groupWorkFreq,
        String assignmentFreq,
        String koreanUsage,
        LocalDateTime createdAt
) {
    public static CourseReviewResponse of(CourseReview r, String content,
                                          String originalContent, Language originalLanguage, boolean isMine) {
        return new CourseReviewResponse(
                r.getId(), r.getRating(), content, originalContent, originalLanguage, isMine,
                name(r.getAttendanceType()), name(r.getPresentationFreq()), name(r.getGroupWorkFreq()),
                name(r.getAssignmentFreq()), name(r.getKoreanUsage()), r.getCreatedAt());
    }

    private static String name(Enum<?> e) {
        return e == null ? null : e.name();
    }
}
