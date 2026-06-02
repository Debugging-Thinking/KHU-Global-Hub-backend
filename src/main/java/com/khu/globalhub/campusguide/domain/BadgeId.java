package com.khu.globalhub.campusguide.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeId {
    COURSE_REG("수강신청 박사", "Course Reg Expert", "📚"),
    TRANSPORT  ("교통 박사",    "Transport Expert",   "🚌"),
    FOOD       ("맛집 박사",    "Food Expert",         "🍽️"),
    CAMPUS_SITE("사이트 박사",  "Campus Site Expert",  "🔗"),
    HUMANITIES ("교양 박사",    "Humanities Expert",   "🎓");

    private final String nameKO;
    private final String nameEN;
    private final String emoji;
}
