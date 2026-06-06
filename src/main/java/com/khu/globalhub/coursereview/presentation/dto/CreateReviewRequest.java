package com.khu.globalhub.coursereview.presentation.dto;

import com.khu.globalhub.coursereview.domain.AttendanceType;
import com.khu.globalhub.coursereview.domain.FrequencyLevel;
import com.khu.globalhub.shared.enums.Language;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 강의평 작성 요청. 별점·본문 필수, 지표 5종 선택, language는 작성자 언어(원문 라벨, 미지정 시 KO). */
public record CreateReviewRequest(
        @NotNull @Min(1) @Max(5)
        Integer rating,

        @NotBlank
        String content,

        AttendanceType attendanceType,
        FrequencyLevel presentationFreq,
        FrequencyLevel groupWorkFreq,
        FrequencyLevel assignmentFreq,
        FrequencyLevel koreanUsage,

        Language language
) {}
