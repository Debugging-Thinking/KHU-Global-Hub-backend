package com.khu.globalhub.profile.presentation;

import com.khu.globalhub.profile.presentation.dto.ProfileResponse;
import com.khu.globalhub.profile.presentation.dto.UpdateMentoringRoleRequest;
import com.khu.globalhub.profile.presentation.dto.UpdateProfileRequest;
import com.khu.globalhub.profile.application.MemberService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 내 프로필 조회. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        Long myId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok(memberService.getProfile(myId)));
    }

    /** 타인 프로필 조회. */
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMemberProfile(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getProfile(memberId)));
    }

    /** 내 프로필 수정. */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok("프로필이 수정되었습니다.", memberService.updateProfile(myId, request)));
    }

    /** 멘토링 역할 변경. */
    @PatchMapping("/me/mentoring-role")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMentoringRole(
            @Valid @RequestBody UpdateMentoringRoleRequest request
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok("멘토링 역할이 변경되었습니다.", memberService.updateMentoringRole(myId, request)));
    }

    /** 프로필 이미지 업로드. 갱신된 전체 프로필 반환. */
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfileImage(
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        Long myId = SecurityUtil.getCurrentMemberId();
        ProfileResponse updated = memberService.updateProfileImage(myId, image.getBytes(), image.getContentType());
        return ResponseEntity.ok(ApiResponse.ok("프로필 이미지가 업데이트되었습니다.", updated));
    }
}
