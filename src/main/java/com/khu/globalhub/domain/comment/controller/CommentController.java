package com.khu.globalhub.domain.comment.controller;

import com.khu.globalhub.domain.comment.dto.CommentResponse;
import com.khu.globalhub.domain.comment.dto.CreateCommentRequest;
import com.khu.globalhub.domain.comment.service.CommentService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.CommentTargetType;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 댓글 작성. */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long commentId = commentService.createComment(CommentTargetType.POST, postId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 작성되었습니다.", commentId));
    }

    /** 댓글 목록 조회 (대댓글 포함). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "KO") Language language
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<CommentResponse> result = commentService.getComments(CommentTargetType.POST, postId, memberId, language);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // 삭제/좋아요는 POST/QNA/ANSWER 공통으로 DELETE /api/comments/{commentId} 사용
}
