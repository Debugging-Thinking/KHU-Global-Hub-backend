package com.khu.globalhub.coursereview.presentation.dto;

import com.khu.globalhub.coursereview.domain.Lecture;

import java.util.List;

public record LectureDetailResponse(
        Long lectureId,
        String code,
        String name,
        String professor,
        String college,
        String type,
        Integer credits,
        String semester,
        int reviewCount,
        double avgRating,
        IndicatorSummary indicators,
        List<CourseReviewResponse> reviews
) {
    public static LectureDetailResponse of(Lecture l, int reviewCount, double avgRating,
                                           IndicatorSummary indicators, List<CourseReviewResponse> reviews) {
        return new LectureDetailResponse(
                l.getId(), l.getCode(), l.getName(), l.getProfessor(),
                l.getCollege(), l.getType(), l.getCredits(), l.getSemester(),
                reviewCount, Math.round(avgRating * 10) / 10.0, indicators, reviews
        );
    }
}
