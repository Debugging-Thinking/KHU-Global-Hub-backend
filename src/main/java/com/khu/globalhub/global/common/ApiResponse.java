package com.khu.globalhub.global.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    // 데이터 있는 성공 응답
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data);
    }

    // 메시지 + 데이터 성공 응답
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 데이터 없는 성공 응답 (생성/삭제 등)
    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    // 실패 응답
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
