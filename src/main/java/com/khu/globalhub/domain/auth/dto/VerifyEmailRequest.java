package com.khu.globalhub.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class VerifyEmailRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "인증 코드를 입력해주세요.")
    private String code;
}
