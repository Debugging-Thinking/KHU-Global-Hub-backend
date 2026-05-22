package com.khu.globalhub.domain.auth.dto;

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

    @NotNull(message = "멘토링 역할을 선택해주세요.")
    private MentoringRole mentoringRole;
}
