package com.khu.globalhub.coursereview.presentation.dto;

import com.khu.globalhub.coursereview.domain.Lecture;

public record LectureSummaryResponse(
        Long lectureId,
        String code,
        String name,
        String professor,
        String college,
        String type,
        Integer credits,
        int reviewCount,
        double avgRating
) {
    public static LectureSummaryResponse of(Lecture l, int reviewCount, double avgRating) {
        return new LectureSummaryResponse(
                l.getId(), l.getCode(), l.getName(), l.getProfessor(),
                l.getCollege(), l.getType(), l.getCredits(),
                reviewCount, Math.round(avgRating * 10) / 10.0
        );
    }
}
