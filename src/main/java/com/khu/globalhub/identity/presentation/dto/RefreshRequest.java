package com.khu.globalhub.identity.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotBlank(message = "refreshToken을 입력해주세요.")
    private String refreshToken;
}
