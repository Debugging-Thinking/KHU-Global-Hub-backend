package com.khu.globalhub.domain.member.controller;

import com.khu.globalhub.domain.board.dto.PostSummaryResponse;
import com.khu.globalhub.domain.member.dto.ProfileResponse;
import com.khu.globalhub.domain.member.dto.UpdateMentoringRoleRequest;
import com.khu.globalhub.domain.member.dto.UpdateProfileRequest;
import com.khu.globalhub.domain.member.service.MemberService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    /** 프로필 이미지 업로드. */
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> updateProfileImage(
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        Long myId = SecurityUtil.getCurrentMemberId();
        String url = memberService.updateProfileImage(myId, image.getBytes(), image.getContentType());
        return ResponseEntity.ok(ApiResponse.ok("프로필 이미지가 업데이트되었습니다.", url));
    }

    /**
     * 특정 멤버의 게시글 목록.
     * 본인이면 익명 포함, 타인이면 익명 제외.
     */
    @GetMapping("/{memberId}/posts")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getMemberPosts(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "KO") Language language,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        Page<PostSummaryResponse> result = memberService.getMemberPosts(myId, memberId, language, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
