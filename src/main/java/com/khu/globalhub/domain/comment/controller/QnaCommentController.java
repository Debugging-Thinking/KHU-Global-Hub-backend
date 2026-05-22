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

/**
 * Q&A 질문과 답변에 달리는 댓글 API.
 *
 * Q&A 질문 댓글  : /api/qnas/{qnaId}/comments
 * Q&A 답변 댓글  : /api/qnas/{qnaId}/answers/{answerId}/comments
 *
 * 댓글 삭제/좋아요는 commentId만 필요하므로
 * 기존 CommentController (/api/posts/{postId}/comments/{commentId}/...) 로직을
 * 그대로 재사용한다 — 단, postId 대신 별도 엔드포인트를 제공한다.
 */
@RestController
@RequiredArgsConstructor
public class QnaCommentController {

    private final CommentService commentService;

    // ── Q&A 질문 댓글 ────────────────────────────────────────────────

    @PostMapping("/api/qnas/{qnaId}/comments")
    public ResponseEntity<ApiResponse<Long>> createQnaComment(
            @PathVariable Long qnaId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long commentId = commentService.createComment(CommentTargetType.QNA, qnaId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 작성되었습니다.", commentId));
    }

    @GetMapping("/api/qnas/{qnaId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getQnaComments(
            @PathVariable Long qnaId,
            @RequestParam(defaultValue = "KO") Language language
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<CommentResponse> result = commentService.getComments(CommentTargetType.QNA, qnaId, memberId, language);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Q&A 답변 댓글 ────────────────────────────────────────────────

    @PostMapping("/api/qnas/{qnaId}/answers/{answerId}/comments")
    public ResponseEntity<ApiResponse<Long>> createAnswerComment(
            @PathVariable Long qnaId,
            @PathVariable Long answerId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long commentId = commentService.createComment(CommentTargetType.ANSWER, answerId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 작성되었습니다.", commentId));
    }

    @GetMapping("/api/qnas/{qnaId}/answers/{answerId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAnswerComments(
            @PathVariable Long qnaId,
            @PathVariable Long answerId,
            @RequestParam(defaultValue = "KO") Language language
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<CommentResponse> result = commentService.getComments(CommentTargetType.ANSWER, answerId, memberId, language);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── 댓글 삭제/좋아요 (QNA/ANSWER 공통) ──────────────────────────

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }

    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(@PathVariable Long commentId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean liked = commentService.toggleLike(commentId, memberId);
        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, liked));
    }
}
