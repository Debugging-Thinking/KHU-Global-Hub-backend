package com.khu.globalhub.identity.presentation.dto;

import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateProfileRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "학과를 입력해주세요.")
    private String department;

    @NotBlank(message = "국적을 입력해주세요.")
    private String nationality;

    @NotNull(message = "입학년도를 입력해주세요.")
    private Integer admissionYear;

    @NotNull(message = "언어를 선택해주세요.")
    private Language language;

    /**
     * 실제 선호 언어 Azure 코드 (6개 외 포함, 예: "fr"). 선택값 — 미입력 시 language로 폴백(하위호환).
     * 서버가 이 값에서 language(버킷)를 파생한다.
     */
    private String preferredLanguage;

    @NotNull(message = "멘토링 역할을 선택해주세요.")
    private MentoringRole mentoringRole;
}
