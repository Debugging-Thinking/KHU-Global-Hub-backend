package com.khu.globalhub.coursereview.presentation.dto;

import java.util.Map;

/**
 * 강의 단위 지표 집계 — 각 지표의 옵션별 응답 수.
 * 키는 enum name(예: 출석 OFFLINE/ONLINE/BLENDED, 빈도 LOW/MEDIUM/HIGH), 0 버킷도 포함.
 * (미입력 응답은 카운트에서 제외 — 합이 reviewCount보다 작을 수 있음)
 */
public record IndicatorSummary(
        Map<String, Integer> attendance,
        Map<String, Integer> presentation,
        Map<String, Integer> groupWork,
        Map<String, Integer> assignment,
        Map<String, Integer> koreanUsage
) {}
