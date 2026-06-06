package com.khu.globalhub.translation.presentation;

import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.translation.application.TranslateService;
import com.khu.globalhub.translation.presentation.dto.TranslateRequest;
import com.khu.globalhub.translation.presentation.dto.TranslateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * on-demand 텍스트 번역 (JWT 보호 — SecurityConfig anyRequest().authenticated()).
 * 콘텐츠 "번역하기"(6개 외 언어 사용자)와 채팅 메시지 "번역"이 공통으로 사용한다.
 */
@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslateService translateService;

    @PostMapping
    public ResponseEntity<ApiResponse<TranslateResponse>> translate(@Valid @RequestBody TranslateRequest request) {
        TranslateResponse result = translateService.translate(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
