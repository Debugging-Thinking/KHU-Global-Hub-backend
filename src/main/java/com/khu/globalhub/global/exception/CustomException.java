package com.khu.globalhub.global.exception;

import lombok.Getter;

/**
 * 서비스 레이어에서 비즈니스 예외를 던질 때 사용하는 기본 예외 클래스.
 * 사용 예시: throw new CustomException(ErrorCode.POST_NOT_FOUND);
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
