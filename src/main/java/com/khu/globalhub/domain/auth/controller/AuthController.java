package com.khu.globalhub.domain.auth.controller;

import com.khu.globalhub.domain.auth.dto.*;
import com.khu.globalhub.domain.auth.service.AuthService;
import com.khu.globalhub.global.common.ApiResponse;
import com.khu.globalhub.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 — @khu.ac.kr 이메일로 인증 코드 발송 */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("인증 이메일을 발송했습니다."));
    }

    /** 이메일 인증 — 코드 확인 후 JWT 발급. hasProfile=false면 프로필 생성 화면으로 이동 */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmail(request)));
    }

    /** 프로필 생성 — 이메일 인증 후 필수. 완료되면 앱 사용 가능 */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        authService.createProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("프로필이 생성되었습니다."));
    }

    /** 로그인 — JWT 발급. hasProfile=false면 프로필 생성 화면으로 이동 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    /** Access Token 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.getRefreshToken())));
    }

    /** 로그아웃 — 서버의 Refresh Token 제거 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        authService.logout(memberId);
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 되었습니다."));
    }
}
