package com.khu.globalhub.board.presentation;

import com.khu.globalhub.board.application.PostService;
import com.khu.globalhub.board.presentation.dto.PostSummaryResponse;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 특정 멤버의 게시글 목록 (board 소유 데이터).
 * 경로는 기존 계약 유지(/api/members/{memberId}/posts)이나 소유 BC는 board.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberPostController {

    private final PostService postService;

    /** 본인이면 익명 포함, 타인이면 익명 제외. */
    @GetMapping("/{memberId}/posts")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getMemberPosts(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "KO") Language language,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        Page<PostSummaryResponse> result = postService.getMemberPosts(myId, memberId, language, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
